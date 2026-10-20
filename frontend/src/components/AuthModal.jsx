import { useState } from "react";
import { useAuth } from "../context/AuthContext";
import { X, Lock, User, Mail, Shield, CheckCircle } from "lucide-react";

export default function AuthModal({ isOpen, onClose, onNotify }) {
  const { login, register } = useAuth();
  const [isRegister, setIsRegister] = useState(false);
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState("ROLE_USER");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      if (isRegister) {
        await register(username, email, password, role);
        onNotify?.({
          type: "success",
          title: "Account Created!",
          message: `Welcome, ${username}! You are logged in with role ${role}.`,
        });
      } else {
        await login(username, password);
        onNotify?.({
          type: "success",
          title: "Welcome Back!",
          message: `Successfully logged in as ${username}.`,
        });
      }
      onClose();
    } catch (err) {
      setError(err.message || "Authentication failed. Please check your credentials.");
      onNotify?.({
        type: "error",
        title: "Auth Error",
        message: err.message,
        correlationId: err.correlationId,
      });
    } finally {
      setLoading(false);
    }
  };

  const handleFillDemo = (demoUser, demoPass) => {
    setUsername(demoUser);
    setPassword(demoPass);
    setIsRegister(false);
    setError(null);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
      <div
        className="w-full max-w-md rounded-2xl border p-6 shadow-2xl relative"
        style={{ backgroundColor: "#171B21", borderColor: "#262B33" }}
      >
        <button
          onClick={onClose}
          className="absolute top-4 right-4 text-gray-400 hover:text-gray-200 transition-colors"
          aria-label="Close modal"
        >
          <X size={18} />
        </button>

        {/* Header Tabs */}
        <div className="flex border-b border-gray-800 pb-3 mb-5">
          <button
            type="button"
            onClick={() => {
              setIsRegister(false);
              setError(null);
            }}
            className={`pb-2 px-4 text-sm font-semibold transition-colors border-b-2 -mb-[13px] ${
              !isRegister
                ? "border-[#3FBF7F] text-[#3FBF7F]"
                : "border-transparent text-gray-400 hover:text-gray-200"
            }`}
          >
            Sign In
          </button>
          <button
            type="button"
            onClick={() => {
              setIsRegister(true);
              setError(null);
            }}
            className={`pb-2 px-4 text-sm font-semibold transition-colors border-b-2 -mb-[13px] ${
              isRegister
                ? "border-[#3FBF7F] text-[#3FBF7F]"
                : "border-transparent text-gray-400 hover:text-gray-200"
            }`}
          >
            Create Account
          </button>
        </div>

        {error && (
          <div className="mb-4 p-3 rounded-lg bg-[#2B1419] border border-[#E0546B]/40 text-[#E0546B] text-xs">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-medium text-gray-400 mb-1">
              Username
            </label>
            <div className="relative">
              <User size={15} className="absolute left-3 top-3 text-gray-500" />
              <input
                type="text"
                required
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="e.g. himanshu"
                className="w-full rounded-lg border bg-[#101318] pl-9 pr-3 py-2 text-sm text-gray-100 placeholder-gray-600 outline-none focus:border-[#3FBF7F]"
                style={{ borderColor: "#2A2F38" }}
              />
            </div>
          </div>

          {isRegister && (
            <div>
              <label className="block text-xs font-medium text-gray-400 mb-1">
                Email Address
              </label>
              <div className="relative">
                <Mail size={15} className="absolute left-3 top-3 text-gray-500" />
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="name@example.com"
                  className="w-full rounded-lg border bg-[#101318] pl-9 pr-3 py-2 text-sm text-gray-100 placeholder-gray-600 outline-none focus:border-[#3FBF7F]"
                  style={{ borderColor: "#2A2F38" }}
                />
              </div>
            </div>
          )}

          <div>
            <label className="block text-xs font-medium text-gray-400 mb-1">
              Password
            </label>
            <div className="relative">
              <Lock size={15} className="absolute left-3 top-3 text-gray-500" />
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="w-full rounded-lg border bg-[#101318] pl-9 pr-3 py-2 text-sm text-gray-100 placeholder-gray-600 outline-none focus:border-[#3FBF7F]"
                style={{ borderColor: "#2A2F38" }}
              />
            </div>
          </div>

          {isRegister && (
            <div>
              <label className="block text-xs font-medium text-gray-400 mb-1">
                Assigned Role (RBAC)
              </label>
              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => setRole("ROLE_USER")}
                  className={`flex items-center justify-center gap-1.5 p-2 rounded-lg border text-xs font-medium transition-all ${
                    role === "ROLE_USER"
                      ? "border-[#3FBF7F] bg-[#3FBF7F]/10 text-[#3FBF7F]"
                      : "border-gray-800 text-gray-400 hover:border-gray-700"
                  }`}
                >
                  <User size={13} /> Regular User
                </button>
                <button
                  type="button"
                  onClick={() => setRole("ROLE_ADMIN")}
                  className={`flex items-center justify-center gap-1.5 p-2 rounded-lg border text-xs font-medium transition-all ${
                    role === "ROLE_ADMIN"
                      ? "border-[#E0A83D] bg-[#E0A83D]/10 text-[#E0A83D]"
                      : "border-gray-800 text-gray-400 hover:border-gray-700"
                  }`}
                >
                  <Shield size={13} /> Administrator
                </button>
              </div>
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full mt-2 py-2.5 px-4 rounded-lg font-semibold text-sm transition-all duration-200 flex items-center justify-center gap-2"
            style={{
              backgroundColor: "#3FBF7F",
              color: "#0B1410",
              opacity: loading ? 0.7 : 1,
            }}
          >
            {loading ? (
              <span>Authenticating...</span>
            ) : (
              <span>{isRegister ? "Register & Sign In" : "Sign In with JWT"}</span>
            )}
          </button>
        </form>

        {/* Demo Accounts Quick-fill */}
        <div className="mt-5 pt-4 border-t border-gray-800/80">
          <p className="text-[11px] uppercase tracking-wider text-gray-500 mb-2 font-mono">
            Demo Credentials
          </p>
          <div className="flex gap-2">
            <button
              type="button"
              onClick={() => handleFillDemo("himanshu", "password123")}
              className="flex-1 text-left px-2.5 py-1.5 rounded border border-gray-800 bg-[#101318] text-[11px] text-gray-400 hover:border-gray-700 transition-colors"
            >
              <span className="text-gray-200 font-medium">User:</span> himanshu
            </button>
            <button
              type="button"
              onClick={() => handleFillDemo("admin", "admin123")}
              className="flex-1 text-left px-2.5 py-1.5 rounded border border-gray-800 bg-[#101318] text-[11px] text-gray-400 hover:border-gray-700 transition-colors"
            >
              <span className="text-[#E0A83D] font-medium">Admin:</span> admin
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
