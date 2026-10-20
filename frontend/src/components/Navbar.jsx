import { useState, useEffect } from "react";
import { useAuth } from "../context/AuthContext";
import { systemApi } from "../services/api";
import {
  PenTool,
  Calendar,
  Layers,
  BarChart3,
  User,
  LogOut,
  Radio,
  ShieldCheck,
  LogIn,
  RefreshCw,
} from "lucide-react";

export default function Navbar({ activeTab, onTabChange, onOpenAuth }) {
  const { user, isAuthenticated, isAdmin, logout } = useAuth();
  const [backendOnline, setBackendOnline] = useState(null);
  const [checkingHealth, setCheckingHealth] = useState(false);

  const checkHealth = async () => {
    setCheckingHealth(true);
    const health = await systemApi.checkBackendHealth();
    setBackendOnline(health.online);
    setCheckingHealth(false);
  };

  useEffect(() => {
    checkHealth();
    const interval = setInterval(checkHealth, 30000);
    return () => clearInterval(interval);
  }, []);

  const navItems = [
    { id: "composer", label: "Composer", icon: PenTool },
    { id: "scheduler", label: "Scheduler & Calendar", icon: Calendar },
    { id: "feed", label: "Posts Feed", icon: Layers },
    { id: "analytics", label: "Analytics & Metrics", icon: BarChart3 },
  ];

  return (
    <header
      className="sticky top-0 z-40 w-full border-b backdrop-blur-md transition-colors"
      style={{ backgroundColor: "#101318E6", borderColor: "#262B33" }}
    >
      <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-3 sm:px-6">
        {/* Brand */}
        <div className="flex items-center gap-3">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-tr from-[#3FBF7F] to-[#3DBFAE] text-[#0B1410] font-bold shadow-md shadow-[#3FBF7F]/10">
            <Radio size={20} />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="font-bold text-gray-100 text-base sm:text-lg tracking-tight">
                SocialSphere
              </span>
              <span className="hidden sm:inline-block rounded-full bg-[#3FBF7F]/10 px-2 py-0.5 text-[10px] font-medium text-[#3FBF7F] border border-[#3FBF7F]/20">
                Full-Stack v3.3.1
              </span>
            </div>
            <p className="text-[10px] text-gray-500 font-mono hidden md:block">
              Himanshu Goyal • UID: 24BDA70369
            </p>
          </div>
        </div>

        {/* Navigation Tabs */}
        <nav className="flex items-center gap-1 sm:gap-1.5 overflow-x-auto py-1">
          {navItems.map((item) => {
            const Icon = item.icon;
            const active = activeTab === item.id;
            return (
              <button
                key={item.id}
                type="button"
                onClick={() => onTabChange(item.id)}
                className={`flex items-center gap-2 rounded-lg px-3 py-1.5 text-xs sm:text-sm font-medium transition-all ${
                  active
                    ? "bg-[#1E232B] text-gray-100 shadow-sm border border-gray-700/60"
                    : "text-gray-400 hover:bg-[#171B21] hover:text-gray-200"
                }`}
              >
                <Icon size={15} className={active ? "text-[#3FBF7F]" : "text-gray-400"} />
                <span className="whitespace-nowrap">{item.label}</span>
              </button>
            );
          })}
        </nav>

        {/* Backend Status & Auth controls */}
        <div className="flex items-center gap-3">
          {/* Backend Health Pill */}
          <button
            type="button"
            onClick={checkHealth}
            title="Click to re-check backend connection"
            className="hidden md:flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-[11px] font-mono transition-colors"
            style={{
              borderColor: backendOnline ? "#3FBF7F40" : "#E0546B40",
              backgroundColor: backendOnline ? "#13281E" : "#2B1419",
              color: backendOnline ? "#3FBF7F" : "#E0546B",
            }}
          >
            <span
              className={`h-1.5 w-1.5 rounded-full ${
                backendOnline ? "bg-[#3FBF7F] animate-pulse" : "bg-[#E0546B]"
              }`}
            />
            <span>{backendOnline ? "Backend Live" : "Backend Offline"}</span>
            <RefreshCw size={10} className={checkingHealth ? "animate-spin" : ""} />
          </button>

          {/* User Controls */}
          {isAuthenticated ? (
            <div className="flex items-center gap-2 pl-2 border-l border-gray-800">
              <div className="flex items-center gap-2">
                <div className="flex h-7 w-7 items-center justify-center rounded-full bg-gray-800 border border-gray-700 text-gray-300 text-xs font-semibold uppercase">
                  {user?.username?.[0] || "U"}
                </div>
                <div className="hidden lg:block text-left">
                  <p className="text-xs font-medium text-gray-200 leading-none">
                    {user?.username}
                  </p>
                  <p className="text-[10px] text-gray-500 font-mono mt-0.5 leading-none">
                    {isAdmin ? "ADMIN" : "USER"}
                  </p>
                </div>
              </div>

              <button
                type="button"
                onClick={logout}
                title="Sign Out"
                className="flex h-7 w-7 items-center justify-center rounded-lg border border-gray-800 text-gray-400 hover:border-gray-700 hover:text-gray-200 transition-colors"
              >
                <LogOut size={13} />
              </button>
            </div>
          ) : (
            <button
              type="button"
              onClick={onOpenAuth}
              className="flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-semibold transition-all"
              style={{ backgroundColor: "#3FBF7F", color: "#0B1410" }}
            >
              <LogIn size={13} />
              <span>Sign In</span>
            </button>
          )}
        </div>
      </div>
    </header>
  );
}
