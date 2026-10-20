import { CheckCircle2, AlertTriangle, XCircle, Info, X } from "lucide-react";

export default function Toast({ toasts, onDismiss }) {
  if (!toasts || toasts.length === 0) return null;

  return (
    <div className="fixed bottom-5 right-5 z-50 flex flex-col gap-2 max-w-md w-full px-4 sm:px-0">
      {toasts.map((toast) => {
        const { id, type = "info", title, message, correlationId } = toast;

        const config = {
          success: {
            bg: "bg-[#13281E]",
            border: "border-[#3FBF7F]/40",
            icon: CheckCircle2,
            iconColor: "text-[#3FBF7F]",
            badge: "bg-[#3FBF7F]/20 text-[#3FBF7F]",
          },
          error: {
            bg: "bg-[#2B1419]",
            border: "border-[#E0546B]/40",
            icon: XCircle,
            iconColor: "text-[#E0546B]",
            badge: "bg-[#E0546B]/20 text-[#E0546B]",
          },
          warning: {
            bg: "bg-[#292212]",
            border: "border-[#E0A83D]/40",
            icon: AlertTriangle,
            iconColor: "text-[#E0A83D]",
            badge: "bg-[#E0A83D]/20 text-[#E0A83D]",
          },
          info: {
            bg: "bg-[#14202B]",
            border: "border-[#3B82C4]/40",
            icon: Info,
            iconColor: "text-[#3B82C4]",
            badge: "bg-[#3B82C4]/20 text-[#3B82C4]",
          },
        }[type] || {
          bg: "bg-[#171B21]",
          border: "border-gray-700",
          icon: Info,
          iconColor: "text-gray-400",
          badge: "bg-gray-800 text-gray-400",
        };

        const IconComponent = config.icon;

        return (
          <div
            key={id}
            className={`flex items-start justify-between gap-3 p-4 rounded-xl border shadow-xl backdrop-blur-md transition-all duration-300 ${config.bg} ${config.border}`}
          >
            <div className="flex items-start gap-3">
              <IconComponent className={`h-5 w-5 mt-0.5 shrink-0 ${config.iconColor}`} />
              <div className="flex-1 text-sm">
                {title && <p className="font-semibold text-gray-100">{title}</p>}
                <p className="text-gray-300 mt-0.5 text-xs sm:text-sm">{message}</p>
                {correlationId && (
                  <p className="mt-1 font-mono text-[10px] text-gray-500">
                    Trace ID: {correlationId}
                  </p>
                )}
              </div>
            </div>
            <button
              onClick={() => onDismiss(id)}
              className="text-gray-400 hover:text-gray-200 transition-colors p-1"
              aria-label="Close notification"
            >
              <X size={15} />
            </button>
          </div>
        );
      })}
    </div>
  );
}
