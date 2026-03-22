# NGINX API Gateway - Architecture & Routing Diagram

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            CLIENT APPLICATIONS                              │
│                    (Web, Mobile, Desktop, CLI, etc.)                        │
└────────────────────────────────────┬────────────────────────────────────────┘
                                     │ HTTP/HTTPS (Port 80/443)
                                     │
                          ┌──────────▼──────────┐
                          │  NGINX API Gateway  │
                          │   (Reverse Proxy)   │
                          │  Routing | Auth     │
                          │  Caching | Limits   │
                          └──────────┬──────────┘
                                     │
         ┌───────────────────────────┼───────────────────────────┐
         │                           │                           │
         │                           │                           │
    ┌────▼─────┐            ┌────────▼────────┐         ┌────────▼────────┐
    │ /api/auth │            │  /api/v1/*     │         │ /api/recommendation
    │ /api/users│            │  /api/v1/*     │         │ /api/ratings/
    │           │            │                │         │                 │
    │user-      │            │ movie-service  │         │recommendation-  │
    │service    │            │                │         │service          │
    │ 3000      │            │ :8080 (8081)   │         │ :8080           │
    └────┬──────┘            └────────┬───────┘         └────────┬────────┘
         │                           │                           │
    ┌────▼──────┐            ┌───────▼─────────┐         ┌───────▼─────────┐
    │ Auth Mgmt  │            │ Movie Database  │         │ Recommendation  │
    │ JWT Tokens │            │ Redis Cache     │         │ PostgreSQL DB   │
    │ PostgreSQL │            │                 │         │ RabbitMQ Queue  │
    └────────────┘            └─────────────────┘         └─────────────────┘
         │                           │                           │
         └───────────────────────────┼───────────────────────────┘
                                     │
                          ┌──────────▼──────────┐
                          │ PostgreSQL Database │
                          │   movie_db          │
                          │   user_db           │
                          │   recommendation_db │
                          └─────────────────────┘
```

## Detailed Request Flow

### 1. Authentication Flow
```
Client Request:
  POST /api/auth/login
  Content-Type: application/json
  {"email": "user@example.com", "password": "password"}

↓ NGINX processes request (no auth check needed for login)

↓ Routes to user-service:3000

Server Response:
  200 OK
  {"token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", "userId": 123}

Client receives JWT token and uses it for subsequent requests.
```

### 2. Authenticated API Request (Recommendations)
```
Client Request:
  GET /api/recommendation/user/123
  Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

↓ NGINX checks Authorization header is present
  ✓ Header found → Forward to service
  ✗ Header missing → Return 401 Unauthorized

↓ Routes to recommendation-service:8080

Service validates JWT token

Server Response:
  200 OK
  [{"movieId": 1, "score": 0.95}, {"movieId": 2, "score": 0.87}, ...]
```

### 3. Public API Request (Movies)
```
Client Request:
  GET /api/v1/movies/popular?page=0&size=20

↓ NGINX processes request (optional caching)
  ✓ Cache hit (< 10 min old) → Return cached response
  ✓ Cache miss or expired → Forward to service

↓ Routes to movie-service:8080

Service queries Redis cache and PostgreSQL

Server Response:
  200 OK
  (Response cached for 10 minutes)
  [{"id": 1, "title": "Inception", ...}, ...]
```

## NGINX Request Processing Pipeline

```
                         Incoming Request
                              │
                              ▼
                    ┌──────────────────┐
                    │  Parse Headers   │
                    │  Check CORS      │
                    │  Handle OPTIONS  │
                    └─────────┬────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │ Rate Limiting    │
         ┌─────────▶│ Zone Check       │◀─────────┐
         │          │ (5 or 100 r/s)   │          │
         │          └─────────┬────────┘          │
         │                    │                   │
         │    ┌───────────────▼───────────────┐  │
    Limit│    │ Compare with limits           │  │
    Exceeded  │ If exceeded: return 429       │  │OK
         │    └───────┬───────────────────────┘  │
         │            │                          │
         └────────────┼──────────────────────────┘
                      │
                      ▼
              ┌──────────────────────┐
              │ Check URL path       │
              │ Match location {}    │
              │ Determine upstream   │
              └──────────┬───────────┘
                         │
              ┌──────────┴──────────┐
              │                     │
         ┌────▼─────┐    ┌─────────▼────┐
         │ Check     │    │ No auth check │
         │ Auth      │    │ (public)      │
         │ Header    │    └─────┬───────┘ │
         │ (private  │          │         │
         │ routes)   │          │         │
         └────┬─────┘          │         │
              │                |         │
         ┌────▼────┐      ┌────▼─────┐ │
         │Missing? │      │Check      │ │
         │ 401     │      │Cache      │ │
         │Unauthd  │      └──┬───┬──┬─┘ │
         └────┬────┘         │   │  │   │
              │              │   │  │   │
         ┌────▼────┐    ┌────▼───▼──▼──┐
         │Forward  │    │Upstream      │
         │Present  │    │Condition     │
         └────┬────┘    └──┬───────┬──┬┘
              │            │       │  │
              └────┬───────┘       │  │
                   │               │  │
            ┌──────▼───────────────▼──▼─────┐
            │ tcp_nodelay, keepalive, etc.  │
            │ Proxy to upstream server      │
            └──────┬──────────────────────┬─┘
                   │                      │
            ┌──────▼────┐        ┌────────▼────┐
            │If GET     │        │Send other   │
            │and 200:   │        │requests as- │
            │Cache resp │        │is:          │
            │(10 min)   │        │POST, DELETE │
            └──────┬────┘        └────────┬────┘
                   │                      │
                   └──────────┬───────────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │ Add Headers      │
                    │ CORS             │
                    │ X-Forwarded-*    │
                    │ X-Cache-Status   │
                    └──────────┬───────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │ Apply Compression│
                    │ (Gzip if enabled)│
                    └──────────┬───────┘
                              │
                              ▼
                    ┌──────────────────┐
                    │ Send Response    │
                    │ to Client        │
                    └──────────────────┘
```

## Routing Decision Tree

```
Request arrives at NGINX:80 or :443

├─ Path: /health
│  └─> Return 200 "OK" (health check)
│
├─ Path matches: /api/auth/* or /api/users/*
│  ├─> Rate limit: 5 r/s (burst 10)
│  └─> Forward to user-service:3000
│      ├─ POST /api/auth/login               ✓ Public
│      ├─ POST /api/auth/register            ✓ Public
│      ├─ POST /api/auth/refresh             ✓ Public (but needs old token)
│      ├─ GET  /api/users/profile            ✓ Needs auth
│      └─ PUT  /api/users/profile            ✓ Needs auth
│
├─ Path matches: /api/v1/*
│  ├─> Rate limit: 100 r/s (burst 50)
│  ├─> Cache GET responses for 10 minutes
│  └─> Forward to movie-service:8080
│      ├─ GET /api/v1/movies/popular         ✓ Public (cached)
│      ├─ GET /api/v1/movies?ids=1,2,3       ✓ Public (cached)
│      ├─ GET /api/v1/movies/genres          ✓ Public (cached)
│      └─ GET /api/v1/movies/recommend/{id}  ✓ Public but needs token for personalization
│
├─ Path matches: /api/recommendation/*
│  ├─> Require Authorization header
│  │   ├─ Header missing? → Return 401
│  │   └─ Header present? → Continue
│  ├─> Rate limit: 100 r/s (burst 50)
│  ├─> Request timeout: 60s
│  └─> Forward to recommendation-service:8080
│      └─ GET /api/recommendation/user/{userId}   ✓ Auth required
│
├─ Path matches: /api/ratings/*
│  ├─> Require Authorization header
│  │   ├─ Header missing? → Return 401
│  │   └─ Header present? → Continue
│  ├─> Rate limit: 100 r/s (burst 50)
│  ├─> Request timeout: 30s
│  └─> Forward to recommendation-service:8080
│      ├─ GET /api/ratings/all               ✓ Auth required
│      └─ GET /api/ratings/details           ✓ Auth required
│
├─ Path: /metrics
│  ├─> Optional: Restrict by IP
│  └─> Forward to recommendation-service:8080/actuator/metrics
│
├─ Path: /prometheus
│  ├─> Optional: Restrict by IP
│  └─> Forward to prometheus:9090
│
├─ Path: /grafana
│  ├─> Optional: Restrict by IP
│  └─> Forward to grafana:3000
│
├─ Path: /
│  └─> Return 404 "Not Found, use /api/* routes"
│
└─ Path: /.*hidden files (.htaccess, etc)
   └─> Return 403 "Forbidden"
```

## Performance Characteristics

### Caching Strategy
```
GET /api/v1/movies/popular
  ├─ Cache Hit (< 10 min):
  │  ├─ Time: ~5ms (zero upstream latency)
  │  └─ Status: X-Cache-Status: HIT
  │
  └─ Cache Miss:
     ├─ Query PostgreSQL & Redis
     ├─ Time: ~50-200ms
     └─ Status: X-Cache-Status: MISS
     └─ Store in NGINX cache for next 10 minutes
```

### Rate Limiting Impact
```
Scenario: 100 requests per second (API limit)
├─ Requests 1-100:    ✓ Pass immediately
├─ Requests 101-150:  ✓ Pass (burst allowance)
├─ Request 151:       ✗ 429 Too Many Requests
│
Scenario: 5 requests per second (Auth limit)
├─ Requests 1-5:      ✓ Pass immediately
├─ Requests 6-15:     ✓ Pass (burst allowance)
├─ Request 16:        ✗ 429 Too Many Requests
```

### Connection Management
```
Connection Lifecycle:
├─ Client → NGINX:       Persistent TCP connection
├─ NGINX → Upstream:     Connection pooling (keepalive 32)
│                        Reused for subsequent requests
│                        Closed after keep-alive timeout
└─ Response → Client:    HTTP Keep-Alive or Close
```

## Security Layers

```
1. Initial Request
   └─> Firewall/WAF (optional)

2. NGINX Layer
   └─> Rate limiting
   └─> IP blocking (optional)
   └─> DDoS protection (nginx-module-geoip, etc.)

3. Service Authentication
   ├─> JWT validation
   ├─> User-service verifies token signature
   └─> Role-based access control (optional)

4. Service Authorization
   ├─> Database connection limits
   ├─> Redis access controls
   └─> Internal service communication (optional: mTLS)

5. Data Layer
   └─> PostgreSQL: User/password auth
   └─> Redis: Optional password
```

## Upstream Service Communication

```
NGINX Proxy Headers:
├─ Sent TO upstream:
│  ├─ Host: original_host
│  ├─ X-Real-IP: client_ip
│  ├─ X-Forwarded-For: client_ip, proxy_ip
│  ├─ X-Forwarded-Proto: http or https
│  └─ Authorization: Bearer <token> (forwarded as-is)
│
└─ Received FROM upstream:
   ├─ Content-Type: application/json
   ├─ Content-Length
   └─ Any custom headers
```

## Monitoring & Observability

```
NGINX exposes metrics via:
├─ Access logs
│  └─ /var/log/nginx/access.log
│  └─ Format: remote_addr, timestamp, request, status, bytes_sent, etc.
│
├─ Error logs
│  └─ /var/log/nginx/error.log
│  └─ Connection errors, upstream issues, etc.
│
├─ Health endpoint
│  └─ GET /health
│  └─ Returns: 200 OK
│
└─ Prometheus metrics (via upstream)
   └─ GET /metrics
   └─ Scraped by Prometheus
   └─ Visualized in Grafana
```
