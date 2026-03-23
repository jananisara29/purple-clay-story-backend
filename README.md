# 💜 Purple Clay Story — AI-Powered Jewelry E-Commerce Backend

> Handmade cold porcelain clay jewelry meets GPT-4 and DALL-E 3.  
> A production-grade Spring Boot backend with AI-powered descriptions, customization previews, a shopping chatbot, and personalized recommendations.

---

## 🛠 Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| Security | Spring Security + JWT + RBAC |
| Database | MySQL 8 |
| Cache | Redis 7 |
| AI | OpenAI GPT-4o + DALL-E 3 |
| Docs | Swagger / OpenAPI 3 |
| DevOps | Docker + Docker Compose |
| Testing | JUnit 5 + Mockito |

---

## ✨ Features

### 🔐 Auth
- JWT-based stateless authentication
- Role-based access control — `ADMIN` and `CUSTOMER`
- Register / Login / Refresh token / Profile

### 🪬 Product & Category Management
- Full CRUD with soft delete
- Advanced search + filter (category, price range, customizable, keyword)
- Pagination + sorting
- Redis caching with per-cache TTL strategy

### 🤖 AI — GPT-4 Description Generator
- Auto-generates luxury jewelry copy for each product
- Async batch generation for entire catalog
- Brand-aware prompt tuned for Purple Clay Story

### 🎨 AI — Product Customization (DALL-E 3)
- Customer selects color, shape, hook type
- GPT-4 builds an optimized DALL-E prompt
- DALL-E 3 generates 1024×1024 product preview image
- Java AWT composites AI preview onto base product image
- Results cached in Redis (6hr TTL) — avoids duplicate DALL-E calls

### 💬 AI — Chatbot "Priya"
- GPT-4 powered shopping assistant
- Multi-turn conversation with full DB-persisted history
- Injected with live product catalog as context
- Session-based — works for guests and logged-in users

### 🎯 AI — Recommendation Engine
- Tracks browse events (async, zero latency impact)
- Aggregates purchase history + browse signals per user
- GPT-4 selects and ranks product IDs from catalog
- Graceful fallback to latest products for new users
- 5-minute Redis cache per user

### 📊 Observability
- MDC-based structured logging (requestId + userEmail on every log line)
- Request logging filter — status + duration for every API call
- Redis cache audit — TTL health check, hit/miss counters
- Admin endpoints: cache eviction, full Redis flush

---

## 📁 Project Structure

```
src/main/java/com/purpleclay/jewelry/
├── ai/                        # All AI services (GPT-4, DALL-E, chatbot)
├── config/                    # Redis, OpenAI, Security, Swagger config
├── controller/                # REST controllers
├── exception/                 # Global exception handler + custom exceptions
├── model/
│   ├── dto/                   # Request/response records
│   ├── entity/                # JPA entities
│   └── enums/                 # Role, OrderStatus
├── repository/                # JPA repositories with custom JPQL
├── security/
│   ├── filter/                # JWT auth filter + request logging filter
│   └── jwt/                   # JWT utility
├── service/                   # Business logic
└── util/                      # ProductMapper, LogContext, CacheMetrics
```

---

## 🚀 Running Locally

### Prerequisites
- Docker + Docker Compose
- OpenAI API key

### Setup

```bash
# Clone the repo
git clone https://github.com/YOUR_USERNAME/purple-clay-story-backend.git
cd purple-clay-story-backend

# Create .env from example
cp .env.example .env
# Edit .env and add your OPENAI_API_KEY

# Start everything (app + MySQL + Redis)
docker-compose up --build
```

### Access
| URL | Description |
|-----|-------------|
| `http://localhost:8080/swagger-ui.html` | Interactive API docs |
| `http://localhost:8080/api-docs` | OpenAPI JSON spec |
| `http://localhost:8080/actuator/health` | Health check |

---

## 🔑 Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_USERNAME` | MySQL username | `purpleclay` |
| `DB_PASSWORD` | MySQL password | `purpleclay123` |
| `REDIS_PASSWORD` | Redis password | `redis123` |
| `JWT_SECRET` | Min 256-bit secret | — |
| `OPENAI_API_KEY` | OpenAI API key | — |

---

## 📡 API Overview

### Auth
```
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh
GET    /api/v1/auth/me
```

### Products & Categories
```
GET    /api/v1/products?search=&categoryId=&minPrice=&maxPrice=&customizable=&page=&size=
GET    /api/v1/products/latest
GET    /api/v1/products/{id}
POST   /api/v1/products                     [ADMIN]
PATCH  /api/v1/products/{id}               [ADMIN]
DELETE /api/v1/products/{id}               [ADMIN]

GET    /api/v1/categories
GET    /api/v1/categories/{id}
POST   /api/v1/categories                  [ADMIN]
PUT    /api/v1/categories/{id}             [ADMIN]
DELETE /api/v1/categories/{id}             [ADMIN]
```

### AI Endpoints
```
POST   /api/v1/ai/descriptions/product/{id}            [ADMIN]
PUT    /api/v1/ai/descriptions/product/{id}/regenerate [ADMIN]
POST   /api/v1/ai/descriptions/batch                   [ADMIN]

GET    /api/v1/ai/customize/options/{productId}
POST   /api/v1/ai/customize/preview

POST   /api/v1/ai/chat/session
POST   /api/v1/ai/chat/session/{sessionId}/message
GET    /api/v1/ai/chat/session/{sessionId}/history
DELETE /api/v1/ai/chat/session/{sessionId}

GET    /api/v1/ai/recommendations/me
GET    /api/v1/ai/recommendations/similar/{productId}
```

### Admin — Cache
```
GET    /api/v1/admin/cache/audit
DELETE /api/v1/admin/cache/evict/{cacheName}
DELETE /api/v1/admin/cache/flush-all
```

---

## 🧪 Tests

```bash
# Run all unit tests
mvn test

# Run with coverage report
mvn test jacoco:report
```

Test classes:
- `AuthServiceTest` — register, login, duplicate email
- `ProductServiceTest` — CRUD, soft delete, stock management
- `AIDescriptionServiceTest` — GPT-4 integration, fallback on failure
- `AICustomizationServiceTest` — cache hit/miss, overlay fallback
- `AIChatbotServiceTest` — session, history, multi-turn
- `AIRecommendationServiceTest` — signal-based, fallback, cache
- `CacheAuditServiceTest` — TTL validation, eviction
- `JewelryApplicationIntegrationTest` — full Spring context load

---

## 🏗 Architecture Decisions

| Decision | Reason |
|----------|--------|
| Soft delete for products | Order history integrity |
| Redis per-cache TTL | Products (15m), recommendations (5m), categories (1h), customization (6h) |
| Async browse tracking | Zero latency impact on product detail page |
| GPT-4 for recommendations | Semantic understanding vs pure collaborative filtering |
| DALL-E result caching | Cost control — same options hit cache, not DALL-E again |
| MDC structured logging | requestId traces full request across all log lines |

---

## 👩‍💻 Built By

**Janani Saravanan** — Java Backend Engineer  
[LinkedIn](https://linkedin.com/in/janani-sara) · [GitHub](https://github.com/janani-sara)
