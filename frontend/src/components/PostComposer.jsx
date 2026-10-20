import { useState, useMemo } from "react";
import { useAuth } from "../context/AuthContext";
import { postsApi } from "../services/api";
import {
  Twitter,
  Instagram,
  Linkedin,
  Facebook,
  Image as ImageIcon,
  Minus,
  Plus,
  AlertTriangle,
  CheckCircle2,
  XCircle,
  Send,
  Calendar,
  Sparkles,
  Tag,
  Hash,
} from "lucide-react";

/* ---------------------------------------------------------
   Platform registry — single source of truth for channel constraints
--------------------------------------------------------- */
const PLATFORMS = [
  {
    id: "twitter",
    name: "X / Twitter",
    handle: "@you",
    Icon: Twitter,
    color: "#3DBFAE",
    maxChars: 280,
    mediaMax: 4,
    requiresMedia: false,
    hashtagWarnAt: 3,
  },
  {
    id: "instagram",
    name: "Instagram",
    handle: "@you",
    Icon: Instagram,
    color: "#E85C8A",
    maxChars: 2200,
    mediaMax: 10,
    requiresMedia: true,
    hashtagWarnAt: 30,
  },
  {
    id: "linkedin",
    name: "LinkedIn",
    handle: "You",
    Icon: Linkedin,
    color: "#3B82C4",
    maxChars: 3000,
    mediaMax: 9,
    requiresMedia: false,
    hookWarnAt: 150,
  },
  {
    id: "facebook",
    name: "Facebook",
    handle: "You",
    Icon: Facebook,
    color: "#6C7FE0",
    maxChars: 63206,
    mediaMax: 10,
    requiresMedia: false,
    engageWarnAt: 500,
  },
];

function validatePost(platform, content, mediaCount) {
  const messages = [];
  let status = "ok";
  const charCount = content.length;

  const escalate = (level) => {
    if (level === "error") status = "error";
    else if (level === "warning" && status !== "error") status = "warning";
  };

  if (charCount === 0) {
    escalate("warning");
    messages.push({ level: "warning", text: "Nothing written yet for this channel." });
  }

  if (charCount > platform.maxChars) {
    escalate("error");
    messages.push({
      level: "error",
      text: `${(charCount - platform.maxChars).toLocaleString()} characters over the ${platform.maxChars.toLocaleString()}-character limit.`,
    });
  }

  if (platform.requiresMedia && mediaCount === 0) {
    escalate("error");
    messages.push({ level: "error", text: "Requires at least one photo or video attached." });
  }

  if (mediaCount > platform.mediaMax) {
    escalate("error");
    messages.push({
      level: "error",
      text: `Max ${platform.mediaMax} attachments allowed — ${mediaCount} attached.`,
    });
  }

  const hashtagCount = (content.match(/#\w+/g) || []).length;
  if (platform.hashtagWarnAt && hashtagCount > platform.hashtagWarnAt) {
    escalate("warning");
    messages.push({
      level: "warning",
      text: `${hashtagCount} hashtags used — more than ${platform.hashtagWarnAt} can read as spammy here.`,
    });
  }

  if (platform.hookWarnAt && charCount > platform.hookWarnAt) {
    escalate("warning");
    messages.push({
      level: "warning",
      text: `Feed preview cuts off after ${platform.hookWarnAt} characters — front-load the key takeaway.`,
    });
  }

  if (platform.engageWarnAt && charCount > platform.engageWarnAt) {
    escalate("warning");
    messages.push({
      level: "warning",
      text: `Posts past ${platform.engageWarnAt} characters tend to see lower reach here.`,
    });
  }

  if (status === "ok" && charCount > 0) {
    messages.push({ level: "ok", text: "Clear to publish." });
  }

  return { status, messages, charCount };
}

const STATUS_COLOR = {
  ok: "#3FBF7F",
  warning: "#E0A83D",
  error: "#E0546B",
};

const STATUS_ICON = {
  ok: CheckCircle2,
  warning: AlertTriangle,
  error: XCircle,
};

function SignalMeter({ ratio, status, color }) {
  const segments = 14;
  const filled = Math.round(Math.min(ratio, 1) * segments);
  const barColor =
    status === "error"
      ? STATUS_COLOR.error
      : status === "warning"
      ? STATUS_COLOR.warning
      : color;

  return (
    <div className="flex items-end gap-[3px]" aria-hidden="true">
      {Array.from({ length: segments }).map((_, i) => (
        <div
          key={i}
          className="w-[4px] rounded-sm transition-colors duration-200"
          style={{
            height: 5 + i * 1.1,
            backgroundColor: i < filled ? barColor : "#262B33",
          }}
        />
      ))}
    </div>
  );
}

function MediaStepper({ count, onChange, max }) {
  return (
    <div className="flex items-center gap-2">
      <button
        type="button"
        onClick={() => onChange(Math.max(0, count - 1))}
        className="flex h-6 w-6 items-center justify-center rounded border border-gray-700 text-gray-400 hover:border-gray-500 hover:text-gray-200"
        aria-label="Remove attachment"
      >
        <Minus size={12} />
      </button>
      <span className="flex items-center gap-1 text-xs text-gray-300 tabular-nums">
        <ImageIcon size={13} className="text-gray-500" />
        {count}
      </span>
      <button
        type="button"
        onClick={() => onChange(Math.min(max, count + 1))}
        className="flex h-6 w-6 items-center justify-center rounded border border-gray-700 text-gray-400 hover:border-gray-500 hover:text-gray-200"
        aria-label="Add attachment"
      >
        <Plus size={12} />
      </button>
    </div>
  );
}

export default function PostComposer({ onNotify, onNavigateToScheduler, onPostCreated }) {
  const { user } = useAuth();
  const [title, setTitle] = useState("Full-Stack Integration Architecture v3.3.1");
  const [content, setContent] = useState(
    "Connecting React with Spring Boot REST APIs and Kafka event streaming! Real-time constraints, token security, and async processing unified.\n\n#FullStack #SpringBoot #React"
  );
  const [tagsInput, setTagsInput] = useState("react, spring-boot, kafka, rest-api");
  const [mediaCount, setMediaCount] = useState(1);
  const [selectedIds, setSelectedIds] = useState(["twitter", "linkedin"]);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const togglePlatform = (id) => {
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((p) => p !== id) : [...prev, id]
    );
  };

  const activePlatforms = PLATFORMS.filter((p) => selectedIds.includes(p.id));

  const results = useMemo(
    () =>
      activePlatforms.map((platform) => ({
        platform,
        result: validatePost(platform, content, mediaCount),
      })),
    [activePlatforms, content, mediaCount]
  );

  const hasBlockingError = results.some((r) => r.result.status === "error");
  const canPublish = activePlatforms.length > 0 && !hasBlockingError && content.trim().length > 0;

  // Publish directly to backend REST API (POST /api/v1/posts)
  const handlePublishImmediate = async () => {
    if (!canPublish) return;
    setIsSubmitting(true);

    try {
      const parsedTags = tagsInput
        .split(",")
        .map((t) => t.trim().replace(/^#/, ""))
        .filter(Boolean);

      const postPayload = {
        title: title.trim() || `Social Post ${Date.now()}`,
        content: content.trim(),
        author: user?.username || "himanshu",
        status: "PUBLISHED",
        tags: parsedTags,
      };

      const res = await postsApi.createPost(postPayload);

      onNotify?.({
        type: "success",
        title: "Post Published to Backend!",
        message: `Successfully created and published post ID #${res?.data?.id || "new"}.`,
        correlationId: res?.correlationId,
      });

      onPostCreated?.();
    } catch (err) {
      onNotify?.({
        type: "error",
        title: "Publishing Failed",
        message: err.message,
        correlationId: err.correlationId,
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  // Bridge to Scheduler module
  const handleQueueForScheduler = () => {
    const parsedTags = tagsInput
      .split(",")
      .map((t) => t.trim().replace(/^#/, ""))
      .filter(Boolean);

    onNavigateToScheduler?.({
      title: title.trim(),
      content: content.trim(),
      tags: parsedTags,
      channel: activePlatforms[0]?.name || "Twitter",
    });
  };

  return (
    <div className="w-full">
      {/* Header */}
      <div className="mb-6 flex flex-col sm:flex-row sm:items-baseline sm:justify-between gap-2">
        <div>
          <p
            className="text-[11px] uppercase tracking-[0.2em] text-[#3FBF7F] font-mono"
          >
            Multi-Platform Composer Console
          </p>
          <h1 className="mt-1 text-2xl font-bold text-gray-100 sm:text-3xl tracking-tight">
            One Draft, Unified API Dispatch
          </h1>
          <p className="text-xs text-gray-400 mt-1">
            Author real-time content validated against platform constraints and publish directly to Spring Boot backend.
          </p>
        </div>
        <div className="text-xs text-gray-400 font-mono">
          <span className="text-[#3FBF7F] font-semibold">{activePlatforms.length}</span> channel
          {activePlatforms.length === 1 ? "" : "s"} monitored
        </div>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-[1.15fr_1fr]">
        {/* LEFT: Composer Controls */}
        <div
          className="rounded-2xl border p-5 shadow-lg flex flex-col justify-between"
          style={{ backgroundColor: "#171B21", borderColor: "#262B33" }}
        >
          <div className="space-y-4">
            {/* Platform toggles */}
            <div>
              <label className="block text-[11px] font-mono uppercase tracking-wider text-gray-400 mb-2">
                Target Broadcast Channels
              </label>
              <div className="flex flex-wrap gap-2">
                {PLATFORMS.map((platform) => {
                  const isOn = selectedIds.includes(platform.id);
                  return (
                    <button
                      key={platform.id}
                      type="button"
                      onClick={() => togglePlatform(platform.id)}
                      className="flex items-center gap-2 rounded-full border px-3 py-1.5 text-xs font-medium transition-all"
                      style={{
                        borderColor: isOn ? platform.color : "#2A2F38",
                        backgroundColor: isOn ? `${platform.color}1A` : "transparent",
                        color: isOn ? platform.color : "#7C8494",
                      }}
                    >
                      <span
                        className="h-1.5 w-1.5 rounded-full"
                        style={{ backgroundColor: isOn ? platform.color : "#3A4049" }}
                      />
                      <platform.Icon size={14} />
                      {platform.name}
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Post Title */}
            <div>
              <label className="block text-[11px] font-mono uppercase tracking-wider text-gray-400 mb-1">
                Post Title / Headline
              </label>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="Enter an expressive headline..."
                className="w-full rounded-lg border bg-[#101318] px-3.5 py-2 text-sm text-gray-100 placeholder-gray-600 outline-none focus:border-[#3FBF7F]"
                style={{ borderColor: "#2A2F38" }}
              />
            </div>

            {/* Post Content */}
            <div>
              <div className="flex justify-between items-center mb-1">
                <label className="block text-[11px] font-mono uppercase tracking-wider text-gray-400">
                  Content Body
                </label>
                <span className="text-[11px] font-mono text-gray-500 tabular-nums">
                  {content.length.toLocaleString()} characters
                </span>
              </div>
              <textarea
                value={content}
                onChange={(e) => setContent(e.target.value)}
                rows={7}
                placeholder="Write once. Every channel monitors real-time limits and structure..."
                className="w-full resize-none rounded-lg border bg-[#101318] p-3 text-sm leading-relaxed text-gray-100 placeholder-gray-600 outline-none focus:border-[#3FBF7F]"
                style={{ borderColor: "#2A2F38" }}
              />
            </div>

            {/* Tags & Attachments */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-1">
              <div>
                <label className="block text-[11px] font-mono uppercase tracking-wider text-gray-400 mb-1">
                  Tags (Comma separated)
                </label>
                <div className="relative">
                  <Tag size={13} className="absolute left-3 top-2.5 text-gray-500" />
                  <input
                    type="text"
                    value={tagsInput}
                    onChange={(e) => setTagsInput(e.target.value)}
                    placeholder="fullstack, springboot, react"
                    className="w-full rounded-lg border bg-[#101318] pl-8 pr-3 py-1.5 text-xs text-gray-100 placeholder-gray-600 outline-none focus:border-[#3FBF7F]"
                    style={{ borderColor: "#2A2F38" }}
                  />
                </div>
              </div>

              <div>
                <label className="block text-[11px] font-mono uppercase tracking-wider text-gray-400 mb-1">
                  Media Attachments
                </label>
                <div className="flex items-center justify-between rounded-lg border bg-[#101318] px-3 py-1.5" style={{ borderColor: "#2A2F38" }}>
                  <span className="text-xs text-gray-400 flex items-center gap-1">
                    <ImageIcon size={13} className="text-gray-500" />
                    <span>Uploads</span>
                  </span>
                  <MediaStepper count={mediaCount} onChange={setMediaCount} max={10} />
                </div>
              </div>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="mt-6 pt-4 border-t border-gray-800 space-y-2.5">
            <button
              type="button"
              disabled={!canPublish || isSubmitting}
              onClick={handlePublishImmediate}
              className="flex w-full items-center justify-center gap-2 rounded-xl py-3 text-sm font-semibold transition-all duration-200 shadow-md"
              style={{
                backgroundColor: canPublish ? "#3FBF7F" : "#262B33",
                color: canPublish ? "#0B1410" : "#5C6270",
                cursor: canPublish && !isSubmitting ? "pointer" : "not-allowed",
              }}
            >
              <Send size={15} />
              {isSubmitting ? (
                "Publishing to Spring Boot API..."
              ) : activePlatforms.length === 0 ? (
                "Select a channel to publish"
              ) : hasBlockingError ? (
                "Resolve blocking constraint errors"
              ) : (
                `Publish to Backend (${activePlatforms.length} Channel${
                  activePlatforms.length === 1 ? "" : "s"
                })`
              )}
            </button>

            <button
              type="button"
              onClick={handleQueueForScheduler}
              className="flex w-full items-center justify-center gap-2 rounded-xl py-2.5 text-xs font-semibold border border-gray-700 bg-[#1E232B] text-gray-200 hover:bg-[#252C36] transition-colors"
            >
              <Calendar size={14} className="text-[#3DBFAE]" />
              <span>Queue for Kafka Scheduled Delivery</span>
            </button>
          </div>
        </div>

        {/* RIGHT: Live Per-Channel Validators */}
        <div className="flex flex-col gap-3.5">
          <div className="flex items-center justify-between px-1">
            <h2 className="text-xs font-mono uppercase tracking-wider text-gray-400 flex items-center gap-1.5">
              <Sparkles size={13} className="text-[#3FBF7F]" />
              Live Platform Compliance Engine
            </h2>
            <span className="text-[11px] text-gray-500 font-mono">Real-time AST</span>
          </div>

          {activePlatforms.length === 0 && (
            <div
              className="flex flex-1 items-center justify-center rounded-2xl border border-dashed p-10 text-center text-sm text-gray-500"
              style={{ backgroundColor: "#171B2140", borderColor: "#262B33" }}
            >
              No channels selected. Toggle a broadcast platform above to monitor its live constraints.
            </div>
          )}

          {results.map(({ platform, result }) => {
            const ratio = result.charCount / platform.maxChars;
            const StatusIcon = STATUS_ICON[result.status];

            return (
              <div
                key={platform.id}
                className="rounded-2xl border p-4 shadow-sm transition-all"
                style={{ backgroundColor: "#171B21", borderColor: "#262B33" }}
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2.5">
                    <div
                      className="p-1.5 rounded-lg"
                      style={{ backgroundColor: `${platform.color}18` }}
                    >
                      <platform.Icon size={16} style={{ color: platform.color }} />
                    </div>
                    <div>
                      <span className="text-sm font-semibold text-gray-100">
                        {platform.name}
                      </span>
                      <span className="text-[10px] text-gray-500 font-mono ml-2">
                        {platform.handle}
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center gap-3">
                    <SignalMeter
                      ratio={ratio}
                      status={result.status}
                      color={platform.color}
                    />
                    <span
                      className="text-xs font-mono tabular-nums"
                      style={{
                        color:
                          result.charCount > platform.maxChars
                            ? STATUS_COLOR.error
                            : "#8E97A6",
                      }}
                    >
                      {result.charCount.toLocaleString()}/{platform.maxChars.toLocaleString()}
                    </span>
                  </div>
                </div>

                {/* Validation Messages */}
                <div className="mt-3 space-y-1.5 border-t border-gray-800/80 pt-2.5">
                  {result.messages.map((m, i) => {
                    const MsgIcon = STATUS_ICON[m.level];
                    return (
                      <div
                        key={i}
                        className="flex items-start gap-2 text-xs"
                        style={{ color: STATUS_COLOR[m.level] }}
                      >
                        <MsgIcon size={13} className="mt-[2px] shrink-0" />
                        <span
                          className="text-gray-300"
                          style={{ color: m.level === "ok" ? STATUS_COLOR.ok : undefined }}
                        >
                          {m.text}
                        </span>
                      </div>
                    );
                  })}
                </div>

                <div
                  className="mt-3 flex items-center justify-between rounded-lg px-2.5 py-1.5 text-[11px] font-mono"
                  style={{
                    backgroundColor: `${STATUS_COLOR[result.status]}14`,
                    color: STATUS_COLOR[result.status],
                  }}
                >
                  <span className="flex items-center gap-1.5">
                    <StatusIcon size={12} />
                    {result.status === "ok"
                      ? "Validation Passed"
                      : result.status === "warning"
                      ? "Optimization Advisory"
                      : "Blocking Constraint Error"}
                  </span>
                  <span className="uppercase text-[10px]">
                    {platform.id}
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
