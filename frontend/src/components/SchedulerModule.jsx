import { useState, useEffect } from "react";
import { useAuth } from "../context/AuthContext";
import { eventsApi } from "../services/api";
import {
  Calendar,
  Clock,
  Send,
  AlertTriangle,
  RotateCcw,
  CheckCircle2,
  XCircle,
  Radio,
  RefreshCw,
  Layers,
  ShieldAlert,
  ArrowRight,
} from "lucide-react";

export default function SchedulerModule({ initialDraft, onNotify, onPostCreated }) {
  const { user } = useAuth();

  // Scheduling form state
  const [title, setTitle] = useState(initialDraft?.title || "Scheduled Product Launch Event");
  const [slug, setSlug] = useState("");
  const [content, setContent] = useState(
    initialDraft?.content ||
      "Announcing our brand new real-time distributed architecture! Powered by Spring Boot, Apache Kafka, and React."
  );
  const [channel, setChannel] = useState(initialDraft?.channel || "Twitter");
  const [tags, setTags] = useState(
    initialDraft?.tags?.join(", ") || "launch, fullstack, reactive"
  );
  const [scheduledTime, setScheduledTime] = useState(() => {
    const d = new Date(Date.now() + 3600 * 1000);
    return d.toISOString().slice(0, 16);
  });
  const [failureMode, setFailureMode] = useState("NONE");
  const [submitting, setSubmitting] = useState(false);

  // DLQ & Scheduled events state
  const [dlqMessages, setDlqMessages] = useState([]);
  const [loadingDlq, setLoadingDlq] = useState(false);
  const [replayingId, setReplayingId] = useState(null);
  const [replayingAll, setReplayingAll] = useState(false);
  const [localHistory, setLocalHistory] = useState([
    {
      eventId: "evt-init-9021",
      title: "Quarterly Systems Review",
      channel: "LinkedIn",
      scheduledAt: new Date(Date.now() - 15 * 60000).toISOString(),
      status: "PROCESSED",
      failureMode: "NONE",
    },
    {
      eventId: "evt-init-9022",
      title: "Tech Stack Highlights",
      channel: "Twitter",
      scheduledAt: new Date(Date.now() + 45 * 60000).toISOString(),
      status: "QUEUED",
      failureMode: "NONE",
    },
  ]);

  const loadDlq = async () => {
    setLoadingDlq(true);
    try {
      const res = await eventsApi.getDlqMessages();
      setDlqMessages(res?.data || []);
    } catch (err) {
      console.warn("Failed to fetch DLQ:", err.message);
    } finally {
      setLoadingDlq(false);
    }
  };

  useEffect(() => {
    loadDlq();
    const timer = setInterval(loadDlq, 15000);
    return () => clearInterval(timer);
  }, []);

  const handleScheduleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);

    try {
      const parsedTags = tags
        .split(",")
        .map((t) => t.trim())
        .filter(Boolean);

      const generatedSlug =
        slug.trim() ||
        title
          .toLowerCase()
          .replace(/[^a-z0-9]+/g, "-")
          .replace(/(^-|-$)/g, "") + `-${Date.now().toString().slice(-4)}`;

      const payload = {
        title: title.trim(),
        slug: generatedSlug,
        content: content.trim(),
        author: user?.username || "himanshu",
        channel,
        tags: parsedTags,
        scheduledAt: new Date(scheduledTime).toISOString(),
        failureMode,
      };

      const res = await eventsApi.schedulePost(payload);

      const newEvent = {
        eventId: res?.data?.eventId || `evt-${Date.now()}`,
        title: title.trim(),
        channel,
        scheduledAt: payload.scheduledAt,
        status: res?.data?.status || "QUEUED",
        failureMode,
      };

      setLocalHistory((prev) => [newEvent, ...prev]);

      onNotify?.({
        type: "success",
        title: "Dispatched to Apache Kafka!",
        message: `Event queued on topic '${res?.data?.topic || "social-scheduled-posts"}' with ID ${
          res?.data?.eventId
        }`,
        correlationId: res?.correlationId,
      });

      // Reload DLQ after 2 seconds in case failure simulation was picked up
      setTimeout(loadDlq, 2500);
      onPostCreated?.();
    } catch (err) {
      onNotify?.({
        type: "error",
        title: "Kafka Queue Error",
        message: err.message,
        correlationId: err.correlationId,
      });
    } finally {
      setSubmitting(false);
    }
  };

  const handleTestIdempotency = async () => {
    try {
      const res = await eventsApi.testIdempotency();
      onNotify?.({
        type: "info",
        title: "Idempotency Test Sent",
        message: `Dispatched twin duplicate events (${res?.data?.sharedEventId}). Kafka consumer deduplication active.`,
        correlationId: res?.correlationId,
      });
    } catch (err) {
      onNotify?.({
        type: "error",
        title: "Idempotency Test Failed",
        message: err.message,
      });
    }
  };

  const handleReplaySingle = async (id) => {
    setReplayingId(id);
    try {
      const res = await eventsApi.replayDlq(id);
      onNotify?.({
        type: "success",
        title: "Message Replayed!",
        message: `DLQ message #${id} re-enqueued to active Kafka topic.`,
        correlationId: res?.correlationId,
      });
      await loadDlq();
      onPostCreated?.();
    } catch (err) {
      onNotify?.({
        type: "error",
        title: "Replay Failed",
        message: err.message,
      });
    } finally {
      setReplayingId(null);
    }
  };

  const handleReplayAll = async () => {
    setReplayingAll(true);
    try {
      const res = await eventsApi.replayAllDlq();
      onNotify?.({
        type: "success",
        title: "Bulk Replay Completed",
        message: `Successfully re-enqueued ${res?.data?.replayedCount || 0} dead-lettered events.`,
        correlationId: res?.correlationId,
      });
      await loadDlq();
      onPostCreated?.();
    } catch (err) {
      onNotify?.({
        type: "error",
        title: "Bulk Replay Failed",
        message: err.message,
      });
    } finally {
      setReplayingAll(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Top Banner */}
      <div className="flex flex-col sm:flex-row sm:items-baseline sm:justify-between gap-2">
        <div>
          <p className="text-[11px] uppercase tracking-[0.2em] text-[#3DBFAE] font-mono">
            Event-Driven Scheduling Engine
          </p>
          <h1 className="mt-1 text-2xl font-bold text-gray-100 sm:text-3xl tracking-tight">
            Kafka Producer & Queue Dispatch
          </h1>
          <p className="text-xs text-gray-400 mt-1">
            Asynchronously orchestrate posts with automated exponential backoff retries and Dead-Letter Queue (DLQ) safeguards.
          </p>
        </div>

        <div className="flex gap-2">
          <button
            type="button"
            onClick={handleTestIdempotency}
            className="flex items-center gap-1.5 rounded-lg border border-gray-700 bg-[#1E232B] px-3 py-1.5 text-xs font-semibold text-gray-200 hover:bg-[#252C36] transition-colors"
          >
            <Radio size={13} className="text-[#E0A83D]" />
            <span>Test Idempotency Twin</span>
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-[1.1fr_1fr] gap-6">
        {/* LEFT: Schedule Form */}
        <div
          className="rounded-2xl border p-5 shadow-lg space-y-4"
          style={{ backgroundColor: "#171B21", borderColor: "#262B33" }}
        >
          <div className="flex items-center justify-between border-b border-gray-800 pb-3">
            <h2 className="text-sm font-semibold text-gray-100 flex items-center gap-2">
              <Calendar size={16} className="text-[#3FBF7F]" />
              Schedule Post Dispatch
            </h2>
            <span className="text-[10px] font-mono text-gray-500 uppercase">
              Topic: social-scheduled-posts
            </span>
          </div>

          <form onSubmit={handleScheduleSubmit} className="space-y-4">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label className="block text-[11px] font-mono uppercase tracking-wider text-gray-400 mb-1">
                  Target Channel
                </label>
                <select
                  value={channel}
                  onChange={(e) => setChannel(e.target.value)}
                  className="w-full rounded-lg border bg-[#101318] px-3 py-2 text-xs text-gray-100 outline-none focus:border-[#3FBF7F]"
                  style={{ borderColor: "#2A2F38" }}
                >
                  <option value="Twitter">X / Twitter</option>
                  <option value="LinkedIn">LinkedIn</option>
                  <option value="Instagram">Instagram</option>
                  <option value="Facebook">Facebook</option>
                </select>
              </div>

              <div>
                <label className="block text-[11px] font-mono uppercase tracking-wider text-gray-400 mb-1">
                  Scheduled Delivery Date & Time
                </label>
                <div className="relative">
                  <input
                    type="datetime-local"
                    value={scheduledTime}
                    onChange={(e) => setScheduledTime(e.target.value)}
                    required
                    className="w-full rounded-lg border bg-[#101318] px-3 py-2 text-xs text-gray-100 outline-none focus:border-[#3FBF7F]"
                    style={{ borderColor: "#2A2F38" }}
                  />
                </div>
              </div>
            </div>

            <div>
              <label className="block text-[11px] font-mono uppercase tracking-wider text-gray-400 mb-1">
                Post Title
              </label>
              <input
                type="text"
                required
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="Scheduled post title"
                className="w-full rounded-lg border bg-[#101318] px-3.5 py-2 text-sm text-gray-100 placeholder-gray-600 outline-none focus:border-[#3FBF7F]"
                style={{ borderColor: "#2A2F38" }}
              />
            </div>

            <div>
              <label className="block text-[11px] font-mono uppercase tracking-wider text-gray-400 mb-1">
                Post Payload Content
              </label>
              <textarea
                required
                rows={4}
                value={content}
                onChange={(e) => setContent(e.target.value)}
                placeholder="Post content..."
                className="w-full resize-none rounded-lg border bg-[#101318] p-3 text-xs leading-relaxed text-gray-100 placeholder-gray-600 outline-none focus:border-[#3FBF7F]"
                style={{ borderColor: "#2A2F38" }}
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label className="block text-[11px] font-mono uppercase tracking-wider text-gray-400 mb-1">
                  Custom Slug (Optional)
                </label>
                <input
                  type="text"
                  value={slug}
                  onChange={(e) => setSlug(e.target.value)}
                  placeholder="auto-generated-if-empty"
                  className="w-full rounded-lg border bg-[#101318] px-3 py-2 text-xs text-gray-100 placeholder-gray-600 outline-none focus:border-[#3FBF7F]"
                  style={{ borderColor: "#2A2F38" }}
                />
              </div>

              <div>
                <label className="block text-[11px] font-mono uppercase tracking-wider text-gray-400 mb-1">
                  Tags (CSV)
                </label>
                <input
                  type="text"
                  value={tags}
                  onChange={(e) => setTags(e.target.value)}
                  placeholder="spring, kafka, test"
                  className="w-full rounded-lg border bg-[#101318] px-3 py-2 text-xs text-gray-100 placeholder-gray-600 outline-none focus:border-[#3FBF7F]"
                  style={{ borderColor: "#2A2F38" }}
                />
              </div>
            </div>

            {/* Failure Simulation Selector (Exp 3.1.1 & 3.1.2) */}
            <div className="rounded-xl border p-3 bg-[#101318]" style={{ borderColor: "#2A2F38" }}>
              <div className="flex items-center justify-between mb-2">
                <span className="text-[11px] font-mono uppercase tracking-wider text-gray-400 flex items-center gap-1.5">
                  <ShieldAlert size={13} className="text-[#E0A83D]" />
                  Reliability Simulation Mode
                </span>
                <span className="text-[10px] text-gray-500 font-mono">Fault Tolerance</span>
              </div>
              <div className="grid grid-cols-3 gap-2">
                {[
                  { id: "NONE", label: "Normal (Success)", color: "text-[#3FBF7F]" },
                  { id: "TRANSIENT", label: "Transient (3 Retries)", color: "text-[#E0A83D]" },
                  { id: "FATAL", label: "Fatal (Direct DLQ)", color: "text-[#E0546B]" },
                ].map((mode) => (
                  <button
                    key={mode.id}
                    type="button"
                    onClick={() => setFailureMode(mode.id)}
                    className={`py-1.5 px-2 rounded-lg border text-[11px] font-medium transition-all ${
                      failureMode === mode.id
                        ? "border-[#3FBF7F] bg-[#3FBF7F]/15 text-gray-100 font-semibold"
                        : "border-gray-800 text-gray-400 hover:border-gray-700"
                    }`}
                  >
                    <span className={mode.color}>● </span>
                    {mode.label}
                  </button>
                ))}
              </div>
            </div>

            <button
              type="submit"
              disabled={submitting}
              className="w-full flex items-center justify-center gap-2 rounded-xl py-3 text-sm font-semibold transition-all duration-200"
              style={{
                backgroundColor: "#3DBFAE",
                color: "#0B1410",
                opacity: submitting ? 0.7 : 1,
              }}
            >
              <Send size={15} />
              {submitting ? "Dispatched to Kafka..." : "Queue Asynchronous Event"}
            </button>
          </form>
        </div>

        {/* RIGHT: Scheduled Pipeline & DLQ */}
        <div className="space-y-6">
          {/* Scheduled Pipeline List */}
          <div
            className="rounded-2xl border p-5 shadow-lg"
            style={{ backgroundColor: "#171B21", borderColor: "#262B33" }}
          >
            <div className="flex items-center justify-between border-b border-gray-800 pb-3 mb-3">
              <h2 className="text-sm font-semibold text-gray-100 flex items-center gap-2">
                <Clock size={16} className="text-[#3DBFAE]" />
                Scheduled Event Pipeline
              </h2>
              <span className="text-[11px] font-mono text-gray-500">
                {localHistory.length} tracked
              </span>
            </div>

            <div className="space-y-2.5 max-h-56 overflow-y-auto pr-1">
              {localHistory.map((item, idx) => (
                <div
                  key={idx}
                  className="rounded-xl border p-3 bg-[#101318] flex items-center justify-between text-xs"
                  style={{ borderColor: "#262B33" }}
                >
                  <div className="space-y-0.5 max-w-[70%]">
                    <p className="font-semibold text-gray-200 truncate">{item.title}</p>
                    <div className="flex items-center gap-2 text-[10px] text-gray-400 font-mono">
                      <span className="text-[#3DBFAE]">{item.channel}</span>
                      <span>•</span>
                      <span>{new Date(item.scheduledAt).toLocaleTimeString()}</span>
                      {item.failureMode !== "NONE" && (
                        <span className="text-[#E0A83D] font-bold">[{item.failureMode}]</span>
                      )}
                    </div>
                  </div>

                  <span
                    className={`px-2 py-0.5 rounded-full text-[10px] font-mono font-medium border ${
                      item.status === "PROCESSED"
                        ? "border-[#3FBF7F]/30 bg-[#3FBF7F]/10 text-[#3FBF7F]"
                        : item.status === "QUEUED"
                        ? "border-[#3B82C4]/30 bg-[#3B82C4]/10 text-[#3B82C4]"
                        : "border-[#E0546B]/30 bg-[#E0546B]/10 text-[#E0546B]"
                    }`}
                  >
                    {item.status}
                  </span>
                </div>
              ))}
            </div>
          </div>

          {/* DLQ Inspector & Remediation Console */}
          <div
            className="rounded-2xl border p-5 shadow-lg"
            style={{ backgroundColor: "#171B21", borderColor: "#262B33" }}
          >
            <div className="flex items-center justify-between border-b border-gray-800 pb-3 mb-3">
              <div className="flex items-center gap-2">
                <AlertTriangle size={16} className="text-[#E0546B]" />
                <h2 className="text-sm font-semibold text-gray-100">
                  Dead-Letter Queue (DLQ)
                </h2>
                <span className="rounded-full bg-[#E0546B]/20 text-[#E0546B] text-[10px] px-2 py-0.2 font-mono">
                  {dlqMessages.length}
                </span>
              </div>

              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={loadDlq}
                  title="Refresh DLQ"
                  className="p-1 rounded text-gray-400 hover:text-gray-200"
                >
                  <RefreshCw size={13} className={loadingDlq ? "animate-spin" : ""} />
                </button>
                {dlqMessages.length > 0 && (
                  <button
                    type="button"
                    onClick={handleReplayAll}
                    disabled={replayingAll}
                    className="flex items-center gap-1 rounded bg-[#E0546B]/15 border border-[#E0546B]/30 text-[#E0546B] px-2 py-1 text-[11px] font-medium hover:bg-[#E0546B]/25 transition-colors"
                  >
                    <RotateCcw size={11} className={replayingAll ? "animate-spin" : ""} />
                    <span>Replay All</span>
                  </button>
                )}
              </div>
            </div>

            {dlqMessages.length === 0 ? (
              <div className="py-8 text-center text-xs text-gray-500 font-mono">
                <CheckCircle2 size={24} className="mx-auto mb-2 text-[#3FBF7F]/70" />
                DLQ is clean. Zero poison pills or failed executions.
              </div>
            ) : (
              <div className="space-y-2.5 max-h-56 overflow-y-auto pr-1">
                {dlqMessages.map((msg) => (
                  <div
                    key={msg.id}
                    className="rounded-xl border p-3 bg-[#2B1419]/50 border-[#E0546B]/30 flex flex-col gap-1.5 text-xs"
                  >
                    <div className="flex items-center justify-between">
                      <span className="font-mono text-[10px] text-gray-400">
                        DLQ Record #{msg.id} • Retries: {msg.retryCount}
                      </span>
                      <button
                        type="button"
                        onClick={() => handleReplaySingle(msg.id)}
                        disabled={replayingId === msg.id}
                        className="flex items-center gap-1 text-[11px] font-semibold text-[#3FBF7F] hover:underline"
                      >
                        <RotateCcw size={11} className={replayingId === msg.id ? "animate-spin" : ""} />
                        <span>Replay</span>
                      </button>
                    </div>

                    <p className="font-semibold text-gray-200 text-xs">
                      {msg.title || "Failed Event"}
                    </p>

                    <p className="text-[11px] text-[#E0546B] line-clamp-2 font-mono">
                      {msg.exceptionMessage || "Simulation failure trigger"}
                    </p>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
