// Centralized Full-Stack API Service for SocialSphere Post Composer
// Integrates React frontend with Spring Boot backend (Exp 3.3.1)

const BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8080/api/v1";

/**
 * Enhanced fetch wrapper with:
 * - Bearer JWT token injection
 * - Standardized JSON request / response handling
 * - Correlation ID tracking
 * - Uniform error extraction
 */
async function apiRequest(endpoint, options = {}) {
  const url = `${BASE_URL}${endpoint}`;
  const headers = {
    "Content-Type": "application/json",
    Accept: "application/json",
    ...options.headers,
  };

  // Inject JWT Bearer Token if stored
  const token = localStorage.getItem("accessToken") || localStorage.getItem("token");
  if (token && !headers["Authorization"]) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const config = {
    ...options,
    headers,
  };

  try {
    const response = await fetch(url, config);

    // Capture correlation ID for distributed observability
    const correlationId = response.headers.get("X-Correlation-ID") || response.headers.get("x-correlation-id");

    // Handle 204 No Content
    if (response.status === 204) {
      return { success: true, status: 204, data: null, correlationId };
    }

    const json = await response.json().catch(() => null);

    if (!response.ok) {
      // 401 Unauthorized handling
      if (response.status === 401) {
        // Clear invalid tokens
        localStorage.removeItem("accessToken");
        localStorage.removeItem("token");
        localStorage.removeItem("currentUser");
      }

      const errorMessage =
        json?.message ||
        json?.error ||
        json?.errors?.[0] ||
        `HTTP Error ${response.status}: ${response.statusText}`;

      const error = new Error(errorMessage);
      error.status = response.status;
      error.data = json;
      error.correlationId = correlationId || json?.correlationId;
      throw error;
    }

    return {
      ...json,
      status: response.status,
      correlationId: correlationId || json?.correlationId,
    };
  } catch (err) {
    if (err.name === "TypeError" && err.message.includes("fetch")) {
      const netError = new Error(
        "Cannot connect to Spring Boot backend at " +
          BASE_URL +
          ". Please verify the server is running."
      );
      netError.isNetworkError = true;
      throw netError;
    }
    throw err;
  }
}

// -------------------------------------------------------------
// Authentication Endpoints (/api/v1/auth)
// -------------------------------------------------------------
export const authApi = {
  login: async (credentials) => {
    const res = await apiRequest("/auth/login", {
      method: "POST",
      body: JSON.stringify(credentials),
    });
    if (res.data?.accessToken) {
      localStorage.setItem("accessToken", res.data.accessToken);
      localStorage.setItem("token", res.data.accessToken);
      localStorage.setItem("currentUser", JSON.stringify(res.data));
    }
    return res;
  },

  register: async (userData) => {
    const res = await apiRequest("/auth/register", {
      method: "POST",
      body: JSON.stringify(userData),
    });
    if (res.data?.accessToken) {
      localStorage.setItem("accessToken", res.data.accessToken);
      localStorage.setItem("token", res.data.accessToken);
      localStorage.setItem("currentUser", JSON.stringify(res.data));
    }
    return res;
  },

  getMe: async () => {
    return apiRequest("/auth/me", { method: "GET" });
  },

  logout: () => {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("token");
    localStorage.removeItem("currentUser");
    return Promise.resolve({ success: true });
  },

  getCurrentUser: () => {
    try {
      const stored = localStorage.getItem("currentUser");
      return stored ? JSON.parse(stored) : null;
    } catch {
      return null;
    }
  },

  getToken: () => {
    return localStorage.getItem("accessToken") || localStorage.getItem("token");
  },
};

// -------------------------------------------------------------
// Posts Management Endpoints (/api/v1/posts)
// -------------------------------------------------------------
export const postsApi = {
  getPosts: async ({ page = 0, size = 10, sortBy = "createdAt", direction = "desc" } = {}) => {
    return apiRequest(`/posts?page=${page}&size=${size}&sortBy=${sortBy}&direction=${direction}`, {
      method: "GET",
    });
  },

  getPostById: async (id) => {
    return apiRequest(`/posts/${id}`, { method: "GET" });
  },

  createPost: async (postData) => {
    return apiRequest("/posts", {
      method: "POST",
      body: JSON.stringify(postData),
    });
  },

  updateStatus: async (id, status) => {
    return apiRequest(`/posts/${id}/status`, {
      method: "PATCH",
      body: JSON.stringify({ status }),
    });
  },

  deletePost: async (id) => {
    return apiRequest(`/posts/${id}`, { method: "DELETE" });
  },

  getAuthorAnalytics: async () => {
    return apiRequest("/posts/analytics/authors", { method: "GET" });
  },
};

// -------------------------------------------------------------
// Event-Driven Kafka Scheduling & Reliability (/api/v1/events)
// -------------------------------------------------------------
export const eventsApi = {
  schedulePost: async (eventData) => {
    return apiRequest("/events/scheduled-posts", {
      method: "POST",
      body: JSON.stringify(eventData),
    });
  },

  testIdempotency: async () => {
    return apiRequest("/events/scheduled-posts/idempotent-test", {
      method: "POST",
    });
  },

  simulateFailure: async (mode = "TRANSIENT") => {
    return apiRequest(`/events/scheduled-posts/simulate-failure?mode=${mode}`, {
      method: "POST",
    });
  },

  getDlqMessages: async () => {
    return apiRequest("/events/dlq", { method: "GET" });
  },

  replayDlq: async (id) => {
    return apiRequest(`/events/dlq/${id}/replay`, { method: "POST" });
  },

  replayAllDlq: async () => {
    return apiRequest("/events/dlq/replay-all", { method: "POST" });
  },

  getMetrics: async () => {
    return apiRequest("/events/metrics", { method: "GET" });
  },

  getIdempotencyStatus: async (eventId) => {
    return apiRequest(`/events/idempotency/${eventId}`, { method: "GET" });
  },
};

// -------------------------------------------------------------
// Health Check / System Status
// -------------------------------------------------------------
export const systemApi = {
  checkBackendHealth: async () => {
    try {
      const res = await fetch(`${BASE_URL}/posts?page=0&size=1`, {
        method: "GET",
        headers: { Accept: "application/json" },
      });
      return { online: res.ok || res.status === 401 || res.status === 403, status: res.status };
    } catch {
      return { online: false, status: 0 };
    }
  },
};
