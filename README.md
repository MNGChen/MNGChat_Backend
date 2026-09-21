# MNGChat Backend

Spring Boot chat application backend with multi-AI model support, user authentication, and image upload.


## Frontend Repository

The **[frontend](https://github.com/MNGChen/MNGChat.git)** repository for this project can be found at:

## Tech Stack

- Spring Boot 4.0.2 + Java 21
- PostgreSQL + Spring Data JPA
- Redis cache
- JWT authentication
- S3-compatible object storage
- Gradle build

## Features

- User registration/login with JWT
- Chat session management
- Multi-AI model support
- Chat history persistence
- User-defined chat presets
- Image upload via S3
- Redis caching for chat history
- Admin usage statistics

## Getting Started

### Prerequisites

- JDK 21+
- PostgreSQL
- Redis
- S3-compatible object storage 

## API Endpoints

The server listens on `http://localhost:8080` by default. All protected endpoints require:

```http
Authorization: Bearer <token>
```

| Method | Path | Request | Response | Auth |
|--------|------|---------|----------|------|
| POST | `/register` | JSON: `username`, `email`, `password` | `{"token":"..."}` | No |
| POST | `/login` | JSON: `email`, `password` | `{"token":"..."}` | No |
| POST | `/chat` | JSON: `sessionId`, `message`, optional `model`, `systemPrompt`, `temperature` | Reply, selected model, and token counts | Yes |
| GET | `/chat/sessions` | — | User's sessions, newest first | Yes |
| POST | `/chat/session` | — | Newly created session | Yes |
| PATCH | `/chat/session/{sessionId}/title` | JSON: `title` | `200 OK` | Yes |
| DELETE | `/chat/session/{sessionId}` | — | `204 No Content` | Yes |
| GET | `/chat/models` | — | Available model names | Yes |
| GET | `/chat/history?sessionId={sessionId}` | — | Messages in the session | Yes |
| GET | `/chat/presets` | — | User's saved presets | Yes |
| POST | `/chat/presets` | JSON: `name`, optional `systemPrompt`, `temperature` | Created preset (`201 Created`) | Yes |
| DELETE | `/chat/presets/{presetId}` | — | `204 No Content` | Yes |
| POST | `/chat/session/{sessionId}/image` | `multipart/form-data`, field: `file` (image only; max 10 MB) | Uploaded image message | Yes |
| GET | `/chat/admin/usage` | — | Account, request, token, and per-user usage | `CHAT_ADMIN_EMAILS` only |

`GET /chat/models` currently returns `gpt-5.4` and `gpt-5.4-mini`; omit `model` to use `gpt-5.4`. Set `CHAT_ADMIN_EMAILS` to a comma-separated allowlist to use the usage endpoint.

## Project Structure

```
com.chen.chat_backend/
├── config/          # Configuration classes
├── controler/       # REST controllers
├── dto/             # Data transfer objects
├── entity/          # JPA entities
├── enums/           # Enumerations
├── repository/      # Data access layer
├── request/         # Request objects
├── security/        # JWT security
└── service/         # Business logic
```
