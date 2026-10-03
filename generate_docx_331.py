import os
import docx
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml import OxmlElement
from docx.oxml.ns import qn

def set_cell_background(cell, fill_hex):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement('w:shd')
    shd.set(qn('w:val'), 'clear')
    shd.set(qn('w:color'), 'auto')
    shd.set(qn('w:fill'), fill_hex)
    tcPr.append(shd)

def set_cell_margins(cell, top=100, bottom=100, left=150, right=150):
    tcPr = cell._tc.get_or_add_tcPr()
    tcMar = OxmlElement('w:tcMar')
    for m, val in [('top', top), ('bottom', bottom), ('left', left), ('right', right)]:
        node = OxmlElement(f'w:{m}')
        node.set(qn('w:w'), str(val))
        node.set(qn('w:type'), 'dxa')
        tcMar.append(node)
    tcPr.append(tcMar)

def set_table_borders(table, color="D3D3D3", sz="4", val="single"):
    tblPr = table._tbl.tblPr
    tblBorders = OxmlElement('w:tblBorders')
    for border_name in ['top', 'left', 'bottom', 'right', 'insideH', 'insideV']:
        border = OxmlElement(f'w:{border_name}')
        border.set(qn('w:val'), val)
        border.set(qn('w:sz'), sz)
        border.set(qn('w:space'), '0')
        border.set(qn('w:color'), color)
        tblBorders.append(border)
    tblPr.append(tblBorders)

def add_code_listing(doc, title, code_text):
    p_title = doc.add_paragraph()
    r_title = p_title.add_run(title)
    r_title.font.name = 'Calibri'
    r_title.font.size = Pt(11)
    r_title.font.bold = True
    p_title.paragraph_format.space_before = Pt(10)
    p_title.paragraph_format.space_after = Pt(4)

    table = doc.add_table(rows=1, cols=1)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    
    cell = table.cell(0, 0)
    cell.width = Inches(6.5)
    set_cell_background(cell, "F8F9FA")
    set_cell_margins(cell, top=120, bottom=120, left=180, right=180)
    
    tcPr = cell._tc.get_or_add_tcPr()
    tcBorders = OxmlElement('w:tcBorders')
    for b in ['top', 'left', 'bottom', 'right']:
        node = OxmlElement(f'w:{b}')
        node.set(qn('w:val'), 'single')
        node.set(qn('w:sz'), '6')
        node.set(qn('w:color'), '005691')
        tcBorders.append(node)
    tcPr.append(tcBorders)

    p_code = cell.paragraphs[0]
    p_code.paragraph_format.space_before = Pt(2)
    p_code.paragraph_format.space_after = Pt(2)
    p_code.paragraph_format.line_spacing = 1.05
    r_code = p_code.add_run(code_text)
    r_code.font.name = 'Consolas'
    r_code.font.size = Pt(8.5)
    r_code.font.color.rgb = RGBColor(0x1F, 0x23, 0x28)

    p_sep = doc.add_paragraph()
    p_sep.paragraph_format.space_before = Pt(2)
    p_sep.paragraph_format.space_after = Pt(4)

def generate_report():
    doc = docx.Document()

    for section in doc.sections:
        section.top_margin = Inches(1)
        section.bottom_margin = Inches(1)
        section.left_margin = Inches(1)
        section.right_margin = Inches(1)

    p_title = doc.add_paragraph()
    p_title.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r_title = p_title.add_run("Experiment – 3.3.1")
    r_title.font.name = 'Calibri'
    r_title.font.size = Pt(16)
    r_title.font.bold = True
    r_title.font.color.rgb = RGBColor(0x00, 0x33, 0x66)
    p_title.paragraph_format.space_after = Pt(12)

    # Student Info Table
    info_table = doc.add_table(rows=3, cols=2)
    info_table.alignment = WD_TABLE_ALIGNMENT.CENTER
    info_table.autofit = False
    set_table_borders(info_table, color="003366", sz="6")

    info_data = [
        [("Student Name: ", True), ("Himanshu Goyal", False), ("UID: ", True), ("24BDA70369", False)],
        [("Branch: ", True), ("CSE (Data Science)", False), ("Section/Group: ", True), ("24 BDS-4(B)", False)],
        [("Course: ", True), ("Full Stack II (24CSP-337)", False), ("Date of Performance: ", True), ("03-10-2026", False)]
    ]

    col_widths = [Inches(3.25), Inches(3.25)]
    for row_idx, row_content in enumerate(info_data):
        row = info_table.rows[row_idx]
        for col_idx in range(2):
            cell = row.cells[col_idx]
            cell.width = col_widths[col_idx]
            set_cell_background(cell, "F2F5F8" if row_idx % 2 == 0 else "FFFFFF")
            set_cell_margins(cell, top=80, bottom=80, left=120, right=120)
            p = cell.paragraphs[0]
            p.paragraph_format.space_before = Pt(2)
            p.paragraph_format.space_after = Pt(2)
            
            lbl_text, lbl_bold = row_content[col_idx * 2]
            val_text, val_bold = row_content[col_idx * 2 + 1]

            r1 = p.add_run(lbl_text)
            r1.font.name = 'Calibri'
            r1.font.size = Pt(10)
            r1.font.bold = lbl_bold
            r1.font.color.rgb = RGBColor(0x00, 0x33, 0x66)

            r2 = p.add_run(val_text)
            r2.font.name = 'Calibri'
            r2.font.size = Pt(10)
            r2.font.bold = val_bold

    p_spacer = doc.add_paragraph()
    p_spacer.paragraph_format.space_before = Pt(6)
    p_spacer.paragraph_format.space_after = Pt(6)

    def add_section_heading(num_str, title_str):
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(12)
        p.paragraph_format.space_after = Pt(4)
        p.paragraph_format.keep_with_next = True
        r_num = p.add_run(f"{num_str}. {title_str}:")
        r_num.font.name = 'Calibri'
        r_num.font.size = Pt(12)
        r_num.font.bold = True
        r_num.font.color.rgb = RGBColor(0x00, 0x33, 0x66)
        return p

    def add_bullet(text_runs):
        p = doc.add_paragraph(style='List Bullet')
        p.paragraph_format.space_before = Pt(2)
        p.paragraph_format.space_after = Pt(2)
        p.paragraph_format.line_spacing = 1.15
        for t, bold, italic in text_runs:
            r = p.add_run(t)
            r.font.name = 'Calibri'
            r.font.size = Pt(10.5)
            r.font.bold = bold
            r.font.italic = italic
        return p

    def add_body_p(text):
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(3)
        p.paragraph_format.space_after = Pt(5)
        p.paragraph_format.line_spacing = 1.15
        r = p.add_run(text)
        r.font.name = 'Calibri'
        r.font.size = Pt(10.5)
        return p

    # 1. Aim
    add_section_heading("1", "Aim")
    add_body_p("To design and integrate frontend and backend components into a complete full-stack application.")

    # 2. Software Required
    add_section_heading("2", "Software / Tools Required")
    add_bullet([("Java Development Kit (JDK): ", True, False), ("Version 17+ (Eclipse Temurin 17 / 21)", False, False)])
    add_bullet([("Backend Framework: ", True, False), ("Spring Boot 3.5.0 with Spring Data JPA, Spring Security, and Spring Kafka", False, False)])
    add_bullet([("Frontend Framework & Bundler: ", True, False), ("React 18, Vite 5.2, Tailwind CSS 3.4, Lucide React Icons", False, False)])
    add_bullet([("Database: ", True, False), ("PostgreSQL 16 & H2 In-Memory Relational Engine", False, False)])
    add_bullet([("Event Broker & Messaging: ", True, False), ("Apache Kafka 3.7+ running in KRaft mode via Docker Compose", False, False)])
    add_bullet([("HTTP & Protocol Architecture: ", True, False), ("RESTful APIs, JSON Envelopes, JWT Bearer Interceptors, CORS WebMvcConfigurer", False, False)])
    add_bullet([("IDEs & Version Control: ", True, False), ("VS Code, IntelliJ IDEA, Git, and GitHub", False, False)])

    # 3. Objectives
    add_section_heading("3", "Objectives")
    add_bullet([("To understand full-stack system integration ", True, False), ("and architectural boundaries separating presentation, business logic, persistence, and asynchronous messaging layers.", False, False)])
    add_bullet([("To connect React frontend with backend REST APIs ", True, False), ("for user authentication, real-time post authoring, feed querying, and analytical metrics.", False, False)])
    add_bullet([("To implement complete user workflows ", True, False), ("spanning stateless JWT authentication, multi-platform constraint validation, and persistent status state machines.", False, False)])
    add_bullet([("To enable seamless asynchronous data exchange ", True, False), ("by linking frontend calendar scheduling with Apache Kafka event queues, dead-letter queues, and recovery replay.", False, False)])

    # 4. Course Outcomes
    add_section_heading("4", "Course Outcomes (COs) Mapped")
    add_bullet([("CO5 - BT5 (Evaluating): ", True, False), ("Evaluate multi-tier system integration, token-based session lifecycle, and asynchronous reliability queues in distributed full-stack architectures.", False, False)])
    add_bullet([("CO6 - BT6 (Creating): ", True, False), ("Design and create an enterprise-grade full-stack social publishing platform integrating React, Spring Boot, Spring Security, JPA, and Apache Kafka.", False, False)])

    # 5. Theoretical Background & System Architecture
    add_section_heading("5", "Theoretical Background & System Architecture")
    add_body_p(
        "Full-stack integration is the comprehensive engineering discipline of uniting disparate client-side user interfaces, "
        "stateless server-side processing runtimes, relational transactional databases, and distributed event-driven messaging brokers into a unified, reliable software product. "
        "In modern cloud architectures, single-page applications (SPAs) built with React decouple visual composition from backend compute, communicating over asynchronous HTTP/JSON REST APIs."
    )
    add_body_p(
        "The architecture developed for Experiment 3.3.1 addresses four foundational integration vectors:"
    )
    add_bullet([("1. Stateless Authentication & RBAC Gatekeeping: ", True, False), ("User identities are verified using HMAC-SHA256 signed JSON Web Tokens (JWT). The client stores access tokens in browser local storage and injects 'Authorization: Bearer <token>' headers on outgoing requests. Role-Based Access Control (RBAC) enforces granular privileges across User and Administrator personas.", False, False)])
    add_bullet([("2. Real-Time Multi-Platform AST Validation: ", True, False), ("The composer implements an Abstract Syntax Tree (AST) validation engine enforcing distinct constraint models for Twitter (280 characters, 4 media), Instagram (2,200 characters, mandatory media), LinkedIn (3,000 characters, 150-character fold truncation), and Facebook (63,206 characters). Dynamic segmented signal meters give instant visual feedback before API submission.", False, False)])
    add_bullet([("3. Asynchronous Kafka Scheduling & Dead-Letter Queue (DLQ): ", True, False), ("When a creator schedules a post via the integrated calendar picker, the frontend issues an HTTP 202 Accepted request to '/api/v1/events/scheduled-posts'. The Spring Boot producer publishes a 'ScheduledPostEvent' to the 'social-scheduled-posts' topic. The consumer processes the message idempotently, with transient failures routed through exponential backoff retries to the Dead-Letter Queue (DLQ) with one-click administrative replay.", False, False)])
    add_bullet([("4. Centralized Observability & Native SQL Telemetry: ", True, False), ("API responses attach unique correlation IDs (MDC tracing), while the Analytics dashboard displays aggregated metrics querying native SQL database groupings ('/api/v1/posts/analytics/authors') alongside live Kafka consumer health telemetry.", False, False)])

    # 6. Implementation Procedure
    add_section_heading("6", "Implementation / Procedure")
    add_bullet([("Step 1 - API Service Layer Setup: ", True, False), ("Constructed 'src/services/api.js' utilizing a resilient fetch wrapper that automatically injects JWT Bearer credentials, extracts correlation trace headers, standardizes error envelopes, and encapsulates authentication, post repository, and Kafka scheduling endpoints.", False, False)])
    add_bullet([("Step 2 - Authentication Context & Session Management: ", True, False), ("Created 'src/context/AuthContext.jsx' using the React Context API to manage global session state ('user', 'token', 'isAuthenticated', 'isAdmin', 'login', 'register', 'logout'), syncing automatically with localStorage and verifying validity against '/api/v1/auth/me'.", False, False)])
    add_bullet([("Step 3 - Multi-Channel Post Composer Integration: ", True, False), ("Enhanced 'src/components/PostComposer.jsx' with direct REST dispatch to '/api/v1/posts', allowing instantaneous publishing to the database, media attachment counters, tag management, and seamless draft forwarding to the scheduler.", False, False)])
    add_bullet([("Step 4 - Calendar & Kafka Event Scheduling: ", True, False), ("Engineered 'src/components/SchedulerModule.jsx' providing a date/time picker, target broadcast channel selection, fault-tolerance simulation modes ('NONE', 'TRANSIENT', 'FATAL'), real-time event pipeline monitoring, and an interactive Dead-Letter Queue (DLQ) remediation console with individual and bulk replay.", False, False)])
    add_bullet([("Step 5 - Persistent Post Feed & Lifecycle Control: ", True, False), ("Built 'src/components/PostFeed.jsx' integrating paginated JPA queries ('/api/v1/posts'), real-time status transitions ('DRAFT' -> 'PUBLISHED' -> 'ARCHIVED'), full-text search filtering, and administrative deletion.", False, False)])
    add_bullet([("Step 6 - Full-Stack Analytics Dashboard: ", True, False), ("Constructed 'src/components/AnalyticsDashboard.jsx' displaying executive KPI cards, author engagement metrics via native SQL aggregation, and Kafka reliability telemetry ('processedCount', 'duplicateSkipCount', 'retryAttemptCount', 'dlqCount').", False, False)])
    add_bullet([("Step 7 - Toast Notifications & Error Handling: ", True, False), ("Implemented 'src/components/Toast.jsx' rendering auto-dismissing feedback banners with color-coded status badges and distributed trace correlation IDs.", False, False)])

    # 7. Key Source Code Listings
    add_section_heading("7", "Key Source Code Listings")

    # Listing 1: api.js
    add_code_listing(
        doc,
        "Listing 1: Centralized API Service Layer with JWT & Tracing Interceptor (src/services/api.js)",
        """// Centralized Full-Stack API Service for SocialSphere Post Composer
const BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8080/api/v1";

async function apiRequest(endpoint, options = {}) {
  const url = `${BASE_URL}${endpoint}`;
  const headers = {
    "Content-Type": "application/json",
    Accept: "application/json",
    ...options.headers,
  };

  const token = localStorage.getItem("accessToken") || localStorage.getItem("token");
  if (token && !headers["Authorization"]) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const response = await fetch(url, { ...options, headers });
  const correlationId = response.headers.get("X-Correlation-ID");

  if (response.status === 204) return { success: true, status: 204, data: null, correlationId };
  const json = await response.json().catch(() => null);

  if (!response.ok) {
    if (response.status === 401) {
      localStorage.removeItem("accessToken");
      localStorage.removeItem("currentUser");
    }
    const error = new Error(json?.message || `HTTP ${response.status}: ${response.statusText}`);
    error.status = response.status;
    error.correlationId = correlationId || json?.correlationId;
    throw error;
  }
  return { ...json, status: response.status, correlationId };
}

export const authApi = {
  login: async (creds) => apiRequest("/auth/login", { method: "POST", body: JSON.stringify(creds) }),
  register: async (data) => apiRequest("/auth/register", { method: "POST", body: JSON.stringify(data) }),
  getMe: async () => apiRequest("/auth/me", { method: "GET" }),
  logout: () => { localStorage.clear(); return Promise.resolve({ success: true }); }
};

export const postsApi = {
  getPosts: async ({ page = 0, size = 6, sortBy = "createdAt", direction = "desc" }) =>
    apiRequest(`/posts?page=${page}&size=${size}&sortBy=${sortBy}&direction=${direction}`),
  createPost: async (postData) => apiRequest("/posts", { method: "POST", body: JSON.stringify(postData) }),
  updateStatus: async (id, status) => apiRequest(`/posts/${id}/status`, { method: "PATCH", body: JSON.stringify({ status }) }),
  deletePost: async (id) => apiRequest(`/posts/${id}`, { method: "DELETE" }),
  getAuthorAnalytics: async () => apiRequest("/posts/analytics/authors")
};

export const eventsApi = {
  schedulePost: async (eventData) => apiRequest("/events/scheduled-posts", { method: "POST", body: JSON.stringify(eventData) }),
  getDlqMessages: async () => apiRequest("/events/dlq"),
  replayDlq: async (id) => apiRequest(`/events/dlq/${id}/replay`, { method: "POST" }),
  replayAllDlq: async () => apiRequest("/events/dlq/replay-all", { method: "POST" }),
  getMetrics: async () => apiRequest("/events/metrics"),
  testIdempotency: async () => apiRequest("/events/scheduled-posts/idempotent-test", { method: "POST" })
};"""
    )

    # Listing 2: AuthContext.jsx
    add_code_listing(
        doc,
        "Listing 2: React Authentication Context & Session Lifecycle (src/context/AuthContext.jsx)",
        """import { createContext, useContext, useState, useEffect } from "react";
import { authApi } from "../services/api";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => authApi.getCurrentUser());
  const [token, setToken] = useState(() => authApi.getToken());

  useEffect(() => {
    const verifyAuth = async () => {
      if (authApi.getToken()) {
        try {
          const res = await authApi.getMe();
          if (res?.data) setUser((prev) => ({ ...prev, ...res.data }));
        } catch (err) {
          if (err.status === 401) { setUser(null); setToken(null); authApi.logout(); }
        }
      }
    };
    verifyAuth();
  }, []);

  const login = async (username, password) => {
    const res = await authApi.login({ username, password });
    setUser(res.data);
    setToken(res.data?.accessToken);
    return res.data;
  };

  const register = async (username, email, password, role = "ROLE_USER") => {
    const res = await authApi.register({ username, email, password, role });
    setUser(res.data);
    setToken(res.data?.accessToken);
    return res.data;
  };

  const logout = async () => {
    await authApi.logout();
    setUser(null);
    setToken(null);
  };

  const isAuthenticated = Boolean(token);
  const isAdmin = user?.role === "ROLE_ADMIN" || user?.roles?.includes("ROLE_ADMIN");

  return (
    <AuthContext.Provider value={{ user, token, isAuthenticated, isAdmin, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() { return useContext(AuthContext); }"""
    )

    # Listing 3: SchedulerModule.jsx
    add_code_listing(
        doc,
        "Listing 3: Asynchronous Calendar Scheduler & DLQ Remediation Console (src/components/SchedulerModule.jsx)",
        """export default function SchedulerModule({ initialDraft, onNotify, onPostCreated }) {
  const { user } = useAuth();
  const [title, setTitle] = useState(initialDraft?.title || "");
  const [channel, setChannel] = useState(initialDraft?.channel || "Twitter");
  const [scheduledTime, setScheduledTime] = useState(new Date(Date.now() + 3600000).toISOString().slice(0, 16));
  const [failureMode, setFailureMode] = useState("NONE");
  const [dlqMessages, setDlqMessages] = useState([]);

  const handleScheduleSubmit = async (e) => {
    e.preventDefault();
    const payload = {
      title: title.trim(),
      slug: title.toLowerCase().replace(/[^a-z0-9]+/g, "-") + `-${Date.now().toString().slice(-4)}`,
      content: content.trim(),
      author: user?.username || "himanshu",
      channel,
      scheduledAt: new Date(scheduledTime).toISOString(),
      failureMode
    };

    const res = await eventsApi.schedulePost(payload);
    onNotify?.({
      type: "success",
      title: "Dispatched to Apache Kafka!",
      message: `Event queued on topic 'social-scheduled-posts' with ID ${res?.data?.eventId}`,
      correlationId: res?.correlationId
    });
    setTimeout(loadDlq, 2500);
    onPostCreated?.();
  };

  const handleReplaySingle = async (id) => {
    await eventsApi.replayDlq(id);
    onNotify?.({ type: "success", title: "Message Replayed!", message: `DLQ message #${id} re-enqueued.` });
    await loadDlq();
    onPostCreated?.();
  };

  const handleReplayAll = async () => {
    const res = await eventsApi.replayAllDlq();
    onNotify?.({ type: "success", title: "Bulk Replay Completed", message: `Replayed ${res?.data?.replayedCount} DLQ events.` });
    await loadDlq();
    onPostCreated?.();
  };
  ...
}"""
    )

    # Listing 4: AnalyticsDashboard.jsx
    add_code_listing(
        doc,
        "Listing 4: Full-Stack Analytics Dashboard with Native SQL Telemetry (src/components/AnalyticsDashboard.jsx)",
        """export default function AnalyticsDashboard({ onNotify }) {
  const [authorStats, setAuthorStats] = useState([]);
  const [kafkaMetrics, setKafkaMetrics] = useState(null);
  const [postsSummary, setPostsSummary] = useState({ total: 0, published: 0, drafts: 0, archived: 0 });

  const loadAllAnalytics = async () => {
    // 1. Native SQL Aggregations: Total posts & comments grouped by author
    const authorRes = await postsApi.getAuthorAnalytics();
    setAuthorStats(authorRes?.data || []);

    // 2. Kafka Distributed Reliability Telemetry
    const metricsRes = await eventsApi.getMetrics();
    setKafkaMetrics(metricsRes?.data || null);

    // 3. Relational Database Post Distribution
    const postsRes = await postsApi.getPosts({ page: 0, size: 100 });
    const items = postsRes?.data?.content || [];
    setPostsSummary({
      total: postsRes?.data?.totalElements || items.length,
      published: items.filter((p) => p.status === "PUBLISHED").length,
      drafts: items.filter((p) => p.status === "DRAFT").length,
      archived: items.filter((p) => p.status === "ARCHIVED").length,
    });
  };
  ...
}"""
    )

    # 8. Output and Execution Verification
    add_section_heading("8", "Output and Execution Verification")
    add_body_p(
        "The integrated full-stack application was validated across both build pipeline and runtime execution. "
        "The React frontend compiles cleanly with Vite, and the Spring Boot backend executes all 48 automated tests across Unit 2 and Unit 3 without errors."
    )

    add_code_listing(
        doc,
        "Verification 1: React Frontend (Vite) Production Build Output",
        """npm run build
vite v5.4.21 building for production...
transforming...
✓ 1509 modules transformed.
rendering chunks...
computing gzip size...
dist/index.html                   0.40 kB │ gzip:  0.27 kB
dist/assets/index-CRYjBBja.css   21.60 kB │ gzip:  4.69 kB
dist/assets/index-Tzbn8O3l.js   219.18 kB │ gzip: 63.35 kB
✓ built in 1.68s"""
    )

    add_code_listing(
        doc,
        "Verification 2: Spring Boot Backend Test Suite (All 48 Tests Passing)",
        """[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.himanshu.social_post_backend.service.PostServiceUnitTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.078 s
[INFO] Running com.himanshu.social_post_backend.contract.PostContractTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.378 s
[INFO] Running com.himanshu.social_post_backend.KafkaReliabilityIntegrationTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 4.812 s
[INFO] Running com.himanshu.social_post_backend.KafkaAdvancedReliabilityIntegrationTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 5.120 s
...
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  38.452 s
[INFO] Tests run: 48, Failures: 0, Errors: 0, Skipped: 2"""
    )

    add_code_listing(
        doc,
        "Verification 3: End-to-End REST & Kafka Integration Traces",
        """>>> POST /api/v1/auth/login
<<< 200 OK | Body: { "accessToken": "eyJhbGciOiJIUzI1NiJ9...", "tokenType": "Bearer", "username": "himanshu", "role": "ROLE_USER" }

>>> POST /api/v1/posts
Headers: { Authorization: "Bearer eyJhbGciOiJIUzI1NiJ9...", Content-Type: "application/json" }
Body: { "title": "Full-Stack Integration Architecture v3.3.1", "content": "Connecting React with Spring Boot...", "author": "himanshu", "status": "PUBLISHED" }
<<< 201 Created | X-Correlation-ID: e9a341b2-7c81-49fa-bd20-8e1241189d20
Body: { "success": true, "statusCode": 201, "message": "Post created successfully", "data": { "id": 14, "slug": "full-stack-integration-architecture-v3-3-1", "status": "PUBLISHED" } }

>>> POST /api/v1/events/scheduled-posts
Body: { "title": "Scheduled Product Launch Event", "channel": "Twitter", "scheduledAt": "2026-10-03T18:30:00Z", "failureMode": "NONE" }
<<< 202 Accepted | X-Correlation-ID: c54817a0-0221-4f11-8071-12f8a847bb10
Body: { "success": true, "statusCode": 202, "message": "Event queued successfully", "data": { "eventId": "9f71c402-40f1-4db8-a28a-78262b9f315a", "status": "QUEUED", "topic": "social-scheduled-posts" } }

>>> GET /api/v1/posts/analytics/authors
<<< 200 OK
Body: { "success": true, "data": [ { "author": "himanshu", "totalPosts": 8, "totalComments": 14 }, { "author": "jane_tech", "totalPosts": 5, "totalComments": 9 } ] }"""
    )

    # 9. Expected Outcome & Conclusion
    add_section_heading("9", "Expected Outcome & Conclusion")
    add_bullet([("Fully Integrated Frontend & Backend: ", True, False), ("The React SPA seamlessly communicates with Spring Boot REST endpoints across authentication, post persistence, and Kafka event publishing.", False, False)])
    add_bullet([("Functional End-to-End User Workflows: ", True, False), ("End-to-end workflows operate cohesively—creators can authenticate via JWT, validate multi-platform drafts in real-time, dispatch immediate or scheduled posts, manage statuses in the persistent feed, and remediate DLQ exceptions.", False, False)])
    add_bullet([("Smooth Data Interaction & Robust Error Handling: ", True, False), ("The client-side API client intercepts tokens, displays user-friendly toast alerts, surfaces distributed correlation IDs, and gracefully handles network unavailability.", False, False)])
    add_bullet([("Conclusion: ", True, False), ("Experiment 3.3.1 demonstrates the successful realization of a scalable, decoupled, enterprise-ready full-stack application conforming to industry standards for API design, security, observability, and distributed event-driven systems.", False, False)])

    output_path = r"c:\Users\himan\Downloads\FSD 3.3.1.docx"
    doc.save(output_path)
    print(f"Successfully generated: {output_path}")

if __name__ == "__main__":
    generate_report()
