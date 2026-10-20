# SocialSphere — Full-Stack Social Post Platform (Exp 3.3.1)

A complete, production-grade full-stack application integrating a modern React (Vite + Tailwind CSS) client with a Spring Boot REST API and Apache Kafka event-driven streaming backend.

**Student Profile:**
- **Student Name:** Himanshu Goyal
- **UID:** 24BDA70369
- **Branch:** CSE (Data Science)
- **Section / Group:** 24 BDS-4(B)
- **Course:** Full Stack II (24CSP-337)
- **Mapped Course Outcomes:** CO5 - BT5, CO6 - BT6

---

## Aim & Objectives
- **Aim:** To design and integrate frontend and backend components into a complete full-stack application.
- **Objectives:**
  1. Understand full-stack system integration and architectural boundaries.
  2. Connect React frontend with Spring Boot backend REST APIs and Kafka event queues.
  3. Implement end-to-end user workflows: authentication, composing, scheduling, feed curation, and analytics.
  4. Enable seamless data exchange, state management, and distributed error handling.

---

## Architectural Workflow & Modules

```
+-------------------------------------------------------------------------------+
|                            React Frontend (Vite)                              |
|                                                                               |
|  +------------------+  +-------------------+  +---------------+  +---------+  |
|  |  Post Composer   |  | Calendar Scheduler|  |  Posts Feed   |  |Analytics|  |
|  |  (Multi-Channel) |  |  (Kafka Pipeline) |  |  (JPA Sync)   |  |Dashboard|  |
|  +--------+---------+  +---------+---------+  +-------+-------+  +----+----+  |
|           |                      |                    |               |       |
|           +----------------------+--------------------+---------------+       |
|                                  |                                            |
|                       +----------v-----------+                                |
|                       |   Central API Client  |                               |
|                       |  (JWT Auth & Tracing)|                                |
+-----------------------+----------+-----------+--------------------------------+
                                   | HTTP / JSON (CORS)
                                   v
+-------------------------------------------------------------------------------+
|                        Spring Boot Backend (Port 8080)                        |
|                                                                               |
|  +------------------+  +-------------------+  +---------------+               |
|  |   AuthController |  |  Kafka Controller |  |Post Controller|               |
|  |  (/api/v1/auth)  |  |  (/api/v1/events) |  |(/api/v1/posts)|               |
|  +--------+---------+  +---------+---------+  +-------+-------+               |
|           |                      |                    |                       |
|           v                      v                    v                       |
|     JWT Validation         Apache Kafka          Spring Data JPA              |
|   (HMAC-SHA256 & RBAC)  (Producer/Consumer)     (PostgreSQL/H2 DB)            |
+-------------------------------------------------------------------------------+
```

### Module Breakdown

| Module | Location | Integrated Backend API | Description |
| :--- | :--- | :--- | :--- |
| **Authentication Flow** | `src/context/AuthContext.jsx`, `src/components/AuthModal.jsx` | `POST /api/v1/auth/login`, `POST /api/v1/auth/register`, `GET /api/v1/auth/me` | JWT stateless authentication, token persistence in `localStorage`, role-based badges (`USER` / `ADMIN`), automatic Bearer header injection. |
| **Multi-Platform Composer** | `src/components/PostComposer.jsx` | `POST /api/v1/posts` | Real-time constraint validation (Twitter 280, Instagram media, LinkedIn, Facebook), segmented signal meters, and direct publishing to backend repository. |
| **Calendar & Scheduler** | `src/components/SchedulerModule.jsx` | `POST /api/v1/events/scheduled-posts`, `GET /api/v1/events/dlq`, `POST /api/v1/events/dlq/{id}/replay` | Date & time picker, target channel selection, asynchronous Kafka event dispatch (`202 Accepted`), failure simulation, and Dead-Letter Queue (DLQ) remediation. |
| **Posts Feed** | `src/components/PostFeed.jsx` | `GET /api/v1/posts`, `PATCH /api/v1/posts/{id}/status`, `DELETE /api/v1/posts/{id}` | Real-time synchronized post list with pagination, search filtering, dynamic status updating (`DRAFT`, `PUBLISHED`, `ARCHIVED`), and deletion. |
| **Analytics Dashboard** | `src/components/AnalyticsDashboard.jsx` | `GET /api/v1/posts/analytics/authors`, `GET /api/v1/events/metrics` | High-level KPI cards, author engagement metrics via native SQL aggregation, and Kafka reliability telemetry. |
| **Toast & Observability** | `src/components/Toast.jsx`, `src/services/api.js` | `X-Correlation-ID` header capture | Uniform notification toast system rendering status messages, validation warnings, and distributed trace IDs. |

---

## Running the Full-Stack Application

### 1. Start the Spring Boot Backend & Database
```bash
# In c:\Users\himan\Downloads\social-post-backend\social-post-backend
./mvnw spring-boot:run
```
*Backend runs on `http://localhost:8080`.*

### 2. Start Apache Kafka (Optional Docker KRaft mode)
```bash
docker compose -f docker-compose-kafka.yml up -d
```
*Broker on `localhost:9092`, Kafka-UI on `http://localhost:8085`.*

### 3. Start the React Frontend Development Server
```bash
# In c:\Users\himan\Downloads\post-composer
npm install
npm run dev
```
*Vite frontend opens on `http://localhost:5173`.*

### 4. Build for Production Deployment
```bash
npm run build
```
*Compiles static bundle to `dist/` ready for Vercel, Netlify, or Spring Boot static hosting.*
