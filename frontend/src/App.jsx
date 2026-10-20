import { useState } from "react";
import { AuthProvider } from "./context/AuthContext";
import Navbar from "./components/Navbar";
import PostComposer from "./components/PostComposer";
import SchedulerModule from "./components/SchedulerModule";
import PostFeed from "./components/PostFeed";
import AnalyticsDashboard from "./components/AnalyticsDashboard";
import AuthModal from "./components/AuthModal";
import Toast from "./components/Toast";

function AppContent() {
  const [activeTab, setActiveTab] = useState("composer");
  const [isAuthOpen, setIsAuthOpen] = useState(false);
  const [toasts, setToasts] = useState([]);
  const [schedulerDraft, setSchedulerDraft] = useState(null);
  const [refreshFeedTrigger, setRefreshFeedTrigger] = useState(0);

  const addToast = ({ type = "info", title, message, correlationId }) => {
    const id = Date.now() + Math.random().toString(36).substring(2, 6);
    const newToast = { id, type, title, message, correlationId };
    setToasts((prev) => [...prev, newToast]);

    // Auto dismiss after 4.5 seconds
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 4500);
  };

  const dismissToast = (id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  };

  const handleNavigateToScheduler = (draftData) => {
    setSchedulerDraft(draftData);
    setActiveTab("scheduler");
    addToast({
      type: "info",
      title: "Draft Loaded in Scheduler",
      message: "Configured scheduled delivery parameters for Kafka async processing.",
    });
  };

  const handlePostCreated = () => {
    setRefreshFeedTrigger((n) => n + 1);
  };

  return (
    <div
      className="min-h-screen w-full text-gray-100 flex flex-col"
      style={{
        backgroundColor: "#101318",
        fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif",
      }}
    >
      {/* Top Navigation */}
      <Navbar
        activeTab={activeTab}
        onTabChange={setActiveTab}
        onOpenAuth={() => setIsAuthOpen(true)}
      />

      {/* Main Content Area */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-4 sm:p-6 lg:p-8">
        {activeTab === "composer" && (
          <PostComposer
            onNotify={addToast}
            onNavigateToScheduler={handleNavigateToScheduler}
            onPostCreated={handlePostCreated}
          />
        )}

        {activeTab === "scheduler" && (
          <SchedulerModule
            initialDraft={schedulerDraft}
            onNotify={addToast}
            onPostCreated={handlePostCreated}
          />
        )}

        {activeTab === "feed" && (
          <PostFeed
            key={refreshFeedTrigger}
            onNotify={addToast}
          />
        )}

        {activeTab === "analytics" && (
          <AnalyticsDashboard
            key={refreshFeedTrigger}
            onNotify={addToast}
          />
        )}
      </main>

      {/* Footer */}
      <footer
        className="w-full border-t py-4 text-center text-xs text-gray-500 font-mono"
        style={{ borderColor: "#262B33", backgroundColor: "#0C0E13" }}
      >
        <p>
          Experiment 3.3.1: Full-Stack Integration • React (Vite) + Spring Boot + Apache Kafka + PostgreSQL
        </p>
        <p className="mt-1 text-[11px] text-gray-600">
          Student: Himanshu Goyal (24BDA70369) • Section: 24 BDS-4(B) • Course: 24CSP-337
        </p>
      </footer>

      {/* Authentication Modal */}
      <AuthModal
        isOpen={isAuthOpen}
        onClose={() => setIsAuthOpen(false)}
        onNotify={addToast}
      />

      {/* Floating Notifications */}
      <Toast toasts={toasts} onDismiss={dismissToast} />
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
}
