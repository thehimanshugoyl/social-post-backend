import { useState, useMemo } from "react";
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
} from "lucide-react";

/* ---------------------------------------------------------
   Platform registry — each entry is the single source of
   truth for that channel's constraints. Adding a platform
   means adding one object here; nothing else changes.
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

/* ---------------------------------------------------------
   Pure validation function — takes a platform's rule set
   plus the current draft, returns a status + message list.
   Kept outside the component so it's easy to unit test.
--------------------------------------------------------- */
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
      text: `Feed preview cuts off after ${platform.hookWarnAt} characters — front-load the point.`,
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

/* Segmented signal-strength meter standing in for a plain
   progress bar — reads like a channel VU meter, ties back
   to the "broadcast console" idea for the whole composer. */
function SignalMeter({ ratio, status, color }) {
  const segments = 14;
  const filled = Math.round(Math.min(ratio, 1) * segments);
  const barColor = status === "error" ? STATUS_COLOR.error : status === "warning" ? STATUS_COLOR.warning : color;
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

export default function PostComposer() {
  const [content, setContent] = useState(
    "Shipping the new dashboard today ⚡ Small teams can finally see release health at a glance.\n\n#buildinpublic"
  );
  const [mediaCount, setMediaCount] = useState(1);
  const [selectedIds, setSelectedIds] = useState(["twitter", "instagram", "linkedin"]);
  const [justPublished, setJustPublished] = useState(false);

  const togglePlatform = (id) => {
    setJustPublished(false);
    setSelectedIds((prev) => (prev.includes(id) ? prev.filter((p) => p !== id) : [...prev, id]));
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
  const canPublish = activePlatforms.length > 0 && !hasBlockingError;

  return (
    <div
      className="min-h-screen w-full p-4 sm:p-8"
      style={{ backgroundColor: "#101318", fontFamily: "'Inter', system-ui, sans-serif" }}
    >
      <div className="mx-auto max-w-5xl">
        {/* Header */}
        <div className="mb-6 flex items-baseline justify-between">
          <div>
            <p className="text-[11px] uppercase tracking-[0.2em] text-gray-500" style={{ fontFamily: "'Space Grotesk', sans-serif" }}>
              Composer Console
            </p>
            <h1
              className="mt-1 text-2xl font-semibold text-gray-100 sm:text-3xl"
              style={{ fontFamily: "'Space Grotesk', sans-serif" }}
            >
              One draft, every channel
            </h1>
          </div>
          <div className="hidden text-right text-xs text-gray-500 sm:block">
            <p>{activePlatforms.length} channel{activePlatforms.length === 1 ? "" : "s"} live</p>
          </div>
        </div>

        <div className="grid grid-cols-1 gap-5 lg:grid-cols-[1.1fr_1fr]">
          {/* LEFT: composer */}
          <div className="rounded-xl border p-4 sm:p-5" style={{ backgroundColor: "#171B21", borderColor: "#262B33" }}>
            {/* Platform chips */}
            <div className="mb-4 flex flex-wrap gap-2">
              {PLATFORMS.map((platform) => {
                const isOn = selectedIds.includes(platform.id);
                return (
                  <button
                    key={platform.id}
                    type="button"
                    onClick={() => togglePlatform(platform.id)}
                    className="flex items-center gap-2 rounded-full border px-3 py-1.5 text-xs font-medium transition-colors"
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
                    <platform.Icon size={13} />
                    {platform.name}
                  </button>
                );
              })}
            </div>

            {/* Textarea */}
            <textarea
              value={content}
              onChange={(e) => {
                setJustPublished(false);
                setContent(e.target.value);
              }}
              rows={8}
              placeholder="Write once. Each channel below shows how it'll land."
              className="w-full resize-none rounded-lg border bg-transparent p-3 text-sm leading-relaxed text-gray-100 placeholder-gray-600 outline-none focus:border-gray-500"
              style={{ borderColor: "#2A2F38" }}
            />

            <div className="mt-3 flex flex-wrap items-center justify-between gap-3">
              <div className="flex items-center gap-2">
                <span className="text-[11px] uppercase tracking-wide text-gray-500">Attachments</span>
                <MediaStepper count={mediaCount} onChange={setMediaCount} max={10} />
              </div>
              <span className="text-xs tabular-nums text-gray-500">{content.length.toLocaleString()} characters</span>
            </div>

            <button
              type="button"
              disabled={!canPublish}
              onClick={() => setJustPublished(true)}
              className="mt-5 flex w-full items-center justify-center gap-2 rounded-lg py-2.5 text-sm font-semibold transition-colors"
              style={{
                backgroundColor: canPublish ? "#3FBF7F" : "#262B33",
                color: canPublish ? "#0B1410" : "#5C6270",
                cursor: canPublish ? "pointer" : "not-allowed",
              }}
            >
              <Send size={15} />
              {activePlatforms.length === 0
                ? "Select a channel to publish"
                : hasBlockingError
                ? "Fix errors before publishing"
                : `Publish to ${activePlatforms.length} channel${activePlatforms.length === 1 ? "" : "s"}`}
            </button>
            {justPublished && canPublish && (
              <p className="mt-2 text-center text-xs" style={{ color: "#3FBF7F" }}>
                Published to {activePlatforms.map((p) => p.name).join(", ")}.
              </p>
            )}
          </div>

          {/* RIGHT: per-channel monitors */}
          <div className="flex flex-col gap-3">
            {activePlatforms.length === 0 && (
              <div
                className="flex flex-1 items-center justify-center rounded-xl border border-dashed p-8 text-center text-sm text-gray-600"
                style={{ borderColor: "#262B33" }}
              >
                No channels selected. Turn one on above to see its live validation.
              </div>
            )}

            {results.map(({ platform, result }) => {
              const ratio = result.charCount / platform.maxChars;
              const StatusIcon = STATUS_ICON[result.status];
              return (
                <div
                  key={platform.id}
                  className="rounded-xl border p-4"
                  style={{ backgroundColor: "#171B21", borderColor: "#262B33" }}
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                      <platform.Icon size={15} style={{ color: platform.color }} />
                      <span className="text-sm font-medium text-gray-200">{platform.name}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <SignalMeter ratio={ratio} status={result.status} color={platform.color} />
                      <span
                        className="text-[11px] tabular-nums"
                        style={{ color: result.charCount > platform.maxChars ? STATUS_COLOR.error : "#7C8494" }}
                      >
                        {result.charCount.toLocaleString()}/{platform.maxChars.toLocaleString()}
                      </span>
                    </div>
                  </div>

                  <div className="mt-3 space-y-1.5">
                    {result.messages.map((m, i) => {
                      const MsgIcon = STATUS_ICON[m.level];
                      return (
                        <div key={i} className="flex items-start gap-1.5 text-xs" style={{ color: STATUS_COLOR[m.level] }}>
                          <MsgIcon size={13} className="mt-[1px] shrink-0" />
                          <span className="text-gray-400" style={{ color: m.level === "ok" ? STATUS_COLOR.ok : undefined }}>
                            {m.text}
                          </span>
                        </div>
                      );
                    })}
                  </div>

                  <div
                    className="mt-3 flex items-center gap-1.5 rounded-md px-2 py-1 text-[11px]"
                    style={{ backgroundColor: `${STATUS_COLOR[result.status]}14`, color: STATUS_COLOR[result.status] }}
                  >
                    <StatusIcon size={12} />
                    {result.status === "ok" ? "Ready" : result.status === "warning" ? "Review suggested" : "Blocked"}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}
