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

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| POST | `/register` | Register user | No |
| POST | `/login` | Login user | No |
| POST | `/chat` | Send message | Yes |
| GET | `/chat/sessions` | Session list | Yes |
| POST | `/chat/session` | Create session | Yes |
| GET | `/chat/models` | Model list | Yes |
| GET | `/chat/history` | Chat history | Yes |
| GET/POST/DELETE | `/chat/presets` | Preset management | Yes |
| POST | `/chat/session/{id}/image` | Upload image | Yes |
| GET | `/chat/admin/usage` | Usage stats | Yes |

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
