import { useState, useEffect } from "react";
import { postsApi } from "../services/api";
import { useAuth } from "../context/AuthContext";
import {
  Layers,
  RefreshCw,
  Trash2,
  Tag,
  Clock,
  User,
  ChevronLeft,
  ChevronRight,
  CheckCircle2,
  Archive,
  FileText,
  Search,
  MessageSquare,
} from "lucide-react";

export default function PostFeed({ onNotify }) {
  const { isAdmin } = useAuth();
  const [posts, setPosts] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [actionInProgress, setActionInProgress] = useState(null);

  const fetchPosts = async (targetPage = page) => {
    setLoading(true);
    try {
      const res = await postsApi.getPosts({
        page: targetPage,
        size: 6,
        sortBy: "createdAt",
        direction: "desc",
      });

      if (res?.data?.content) {
        setPosts(res.data.content);
        setTotalPages(res.data.totalPages || 1);
        setTotalElements(res.data.totalElements || 0);
      } else if (Array.isArray(res?.data)) {
        setPosts(res.data);
        setTotalElements(res.data.length);
      }
    } catch (err) {
      console.warn("Failed to load posts:", err.message);
      onNotify?.({
        type: "error",
        title: "Feed Loading Error",
        message: err.message,
        correlationId: err.correlationId,
      });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPosts(page);
  }, [page]);

  const handleStatusChange = async (postId, newStatus) => {
    setActionInProgress(postId);
    try {
      await postsApi.updateStatus(postId, newStatus);
      onNotify?.({
        type: "success",
        title: "Status Updated",
        message: `Post #${postId} updated to ${newStatus}.`,
      });
      // Update local state smoothly
      setPosts((prev) =>
        prev.map((p) => (p.id === postId ? { ...p, status: newStatus } : p))
      );
    } catch (err) {
      onNotify?.({
        type: "error",
        title: "Update Failed",
        message: err.message,
        correlationId: err.correlationId,
      });
    } finally {
      setActionInProgress(null);
    }
  };

  const handleDeletePost = async (postId) => {
    if (!window.confirm(`Are you sure you want to delete post #${postId}?`)) return;

    setActionInProgress(postId);
    try {
      await postsApi.deletePost(postId);
      onNotify?.({
        type: "success",
        title: "Post Deleted",
        message: `Post #${postId} removed from backend database.`,
      });
      fetchPosts(page);
    } catch (err) {
      onNotify?.({
        type: "error",
        title: "Delete Failed",
        message: err.message,
        correlationId: err.correlationId,
      });
    } finally {
      setActionInProgress(null);
    }
  };

  const filteredPosts = posts.filter(
    (p) =>
      p.title?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.content?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.author?.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="space-y-6">
      {/* Feed Header */}
      <div className="flex flex-col sm:flex-row sm:items-baseline sm:justify-between gap-2">
        <div>
          <p className="text-[11px] uppercase tracking-[0.2em] text-[#3FBF7F] font-mono">
            Persistent REST Repository
          </p>
          <h1 className="mt-1 text-2xl font-bold text-gray-100 sm:text-3xl tracking-tight">
            Backend Posts Feed
          </h1>
          <p className="text-xs text-gray-400 mt-1">
            Real-time synchronization with Spring Boot JPA entities, pagination, and dynamic status updates.
          </p>
        </div>

        <div className="flex items-center gap-2">
          {/* Search box */}
          <div className="relative">
            <Search size={13} className="absolute left-2.5 top-2.5 text-gray-500" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search feed..."
              className="rounded-lg border bg-[#171B21] pl-8 pr-3 py-1.5 text-xs text-gray-100 placeholder-gray-500 outline-none focus:border-[#3FBF7F]"
              style={{ borderColor: "#262B33" }}
            />
          </div>

          <button
            type="button"
            onClick={() => fetchPosts(page)}
            disabled={loading}
            className="flex items-center gap-1.5 rounded-lg border border-gray-700 bg-[#1E232B] px-3 py-1.5 text-xs font-semibold text-gray-200 hover:bg-[#252C36] transition-colors"
          >
            <RefreshCw size={13} className={loading ? "animate-spin text-[#3FBF7F]" : ""} />
            <span>Refresh</span>
          </button>
        </div>
      </div>

      {/* Posts Grid */}
      {loading && posts.length === 0 ? (
        <div className="py-16 text-center text-xs text-gray-500 font-mono">
          <RefreshCw size={24} className="mx-auto mb-2 animate-spin text-[#3FBF7F]" />
          Connecting to backend and fetching posts...
        </div>
      ) : filteredPosts.length === 0 ? (
        <div
          className="rounded-2xl border border-dashed p-12 text-center text-gray-500"
          style={{ borderColor: "#262B33" }}
        >
          <FileText size={32} className="mx-auto mb-2 text-gray-600" />
          <p className="text-sm font-medium text-gray-400">No posts found</p>
          <p className="text-xs text-gray-600 mt-1">
            Publish a post from the Composer or schedule an event to see it appear here.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {filteredPosts.map((post) => {
            const isPublishing = actionInProgress === post.id;
            const statusConfig = {
              PUBLISHED: {
                badge: "border-[#3FBF7F]/30 bg-[#3FBF7F]/10 text-[#3FBF7F]",
                label: "Published",
                icon: CheckCircle2,
              },
              DRAFT: {
                badge: "border-[#E0A83D]/30 bg-[#E0A83D]/10 text-[#E0A83D]",
                label: "Draft",
                icon: Clock,
              },
              ARCHIVED: {
                badge: "border-gray-700 bg-gray-800 text-gray-400",
                label: "Archived",
                icon: Archive,
              },
            }[post.status] || {
              badge: "border-gray-700 text-gray-400",
              label: post.status,
              icon: FileText,
            };

            const StatusIcon = statusConfig.icon;

            return (
              <div
                key={post.id}
                className="rounded-2xl border p-5 flex flex-col justify-between shadow-md transition-all hover:border-gray-700"
                style={{ backgroundColor: "#171B21", borderColor: "#262B33" }}
              >
                <div className="space-y-3">
                  {/* Status & ID */}
                  <div className="flex items-center justify-between">
                    <span className="font-mono text-[10px] text-gray-500">
                      ID #{post.id} • {post.slug || "post"}
                    </span>
                    <span
                      className={`flex items-center gap-1 rounded-full px-2 py-0.5 text-[10px] font-mono border ${statusConfig.badge}`}
                    >
                      <StatusIcon size={10} />
                      {statusConfig.label}
                    </span>
                  </div>

                  {/* Title & Content */}
                  <div>
                    <h3 className="font-bold text-gray-100 text-base line-clamp-1">
                      {post.title}
                    </h3>
                    <p className="mt-1 text-xs text-gray-400 line-clamp-3 leading-relaxed">
                      {post.content}
                    </p>
                  </div>

                  {/* Tags */}
                  {post.tags && post.tags.length > 0 && (
                    <div className="flex flex-wrap gap-1.5 pt-1">
                      {post.tags.map((t, i) => (
                        <span
                          key={i}
                          className="rounded bg-[#101318] px-2 py-0.5 text-[10px] font-mono text-gray-400 border border-gray-800"
                        >
                          #{t}
                        </span>
                      ))}
                    </div>
                  )}
                </div>

                {/* Metadata & Actions */}
                <div className="mt-4 pt-3 border-t border-gray-800/80 flex items-center justify-between text-xs text-gray-500">
                  <div className="flex items-center gap-1.5 font-mono text-[11px]">
                    <User size={12} className="text-gray-400" />
                    <span className="text-gray-300">{post.author}</span>
                  </div>

                  <div className="flex items-center gap-2">
                    {/* Status change selector */}
                    <select
                      value={post.status}
                      disabled={isPublishing}
                      onChange={(e) => handleStatusChange(post.id, e.target.value)}
                      className="rounded border border-gray-800 bg-[#101318] px-2 py-1 text-[10px] font-mono text-gray-300 outline-none focus:border-[#3FBF7F]"
                    >
                      <option value="DRAFT">DRAFT</option>
                      <option value="PUBLISHED">PUBLISHED</option>
                      <option value="ARCHIVED">ARCHIVED</option>
                    </select>

                    <button
                      type="button"
                      onClick={() => handleDeletePost(post.id)}
                      disabled={isPublishing}
                      title="Delete post"
                      className="p-1 rounded text-gray-500 hover:text-[#E0546B] transition-colors"
                    >
                      <Trash2 size={13} />
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Pagination Footer */}
      <div className="flex items-center justify-between border-t border-gray-800/80 pt-4 text-xs text-gray-400">
        <p className="font-mono text-[11px]">
          Showing <span className="text-gray-200 font-semibold">{posts.length}</span> of{" "}
          <span className="text-gray-200 font-semibold">{totalElements}</span> database records
        </p>

        <div className="flex items-center gap-2">
          <button
            type="button"
            disabled={page === 0 || loading}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            className="flex items-center gap-1 rounded-lg border border-gray-800 bg-[#171B21] px-3 py-1.5 text-xs text-gray-300 hover:bg-[#1E232B] disabled:opacity-40 disabled:cursor-not-allowed transition-all"
          >
            <ChevronLeft size={13} />
            <span>Previous</span>
          </button>

          <span className="font-mono text-[11px] px-2">
            Page {page + 1} of {Math.max(1, totalPages)}
          </span>

          <button
            type="button"
            disabled={page + 1 >= totalPages || loading}
            onClick={() => setPage((p) => p + 1)}
            className="flex items-center gap-1 rounded-lg border border-gray-800 bg-[#171B21] px-3 py-1.5 text-xs text-gray-300 hover:bg-[#1E232B] disabled:opacity-40 disabled:cursor-not-allowed transition-all"
          >
            <span>Next</span>
            <ChevronRight size={13} />
          </button>
        </div>
      </div>
    </div>
  );
}
