# Image Hosting Service

A secure, AI-powered image-hosting REST API built with Spring Boot.

Users can upload and manage images that are private by default. Images are stored in a private Cloudflare R2 bucket and delivered through authorization-aware API endpoints. RabbitMQ queues background Gemini analysis that generates searchable objects, descriptive tags, and prominent colors.

## Features

- Registration, login, logout, and database-backed sessions
- Automatic login after registration
- JPEG, PNG, and WebP uploads up to 10 MB
- Private-by-default image access
- Public/private visibility management
- Personal image library with pagination
- Public gallery using 100 × 100 thumbnails
- RabbitMQ-backed Gemini image analysis
- Bounded tagging retries and dead-letter handling
- RabbitMQ publisher confirmations and unroutable-message detection
- AI-processing status tracking
- Search through AI-generated objects, tags, and colors
- Expiring and revocable private sharing links
- Owner-only image deletion
- Interactive Swagger API documentation
- Application logging for important business operations
- Centralized log shipping to Grafana Cloud Loki
- Application health, liveness, and readiness monitoring

## Technology

- Java 26
- Spring Boot 4.1
- Spring Security
- Spring Session JDBC
- Spring JDBC
- Spring Boot Actuator
- PostgreSQL
- RabbitMQ
- Flyway
- Cloudflare R2
- Google Gemini
- Thumbnailator
- SLF4J and Logback
- Loki4j Logback Appender
- Grafana Cloud Loki
- Maven
- Docker
- Docker Compose
- Railway
- GitHub Actions
- GitHub Container Registry

## Privacy model

Every uploaded image is private by default.

The uploader can immediately view the image, thumbnail, metadata, and AI-processing status. Other users cannot access it unless the owner explicitly makes it public or creates a valid temporary sharing link.

Public images can appear in the gallery and search results. Making an image private again removes public access.

## Architecture

```text
API client
    |
    v
Spring Boot API
    |-- PostgreSQL: users, sessions, image metadata, and AI tags
    |-- Cloudflare R2: original images and thumbnails
    `-- RabbitMQ: durable image-tagging jobs
            |
            v
        Gemini: image analysis
```

PostgreSQL uses numeric internal identifiers for database relationships. Public API resources use UUIDs so internal database identifiers are not exposed.

### Image-tagging flow

After an image is stored, the API publishes its internal database ID to a durable RabbitMQ queue. A Spring listener consumes the message and calls the existing image-tagging processor, which downloads the image from Cloudflare R2, sends it to Gemini, and stores the generated tags in PostgreSQL.

Before reporting a dispatch as successful, the API waits briefly for RabbitMQ to confirm that the tagging message was accepted and routed to the tagging queue. A rejected, unroutable, or unconfirmed message causes the image-tagging status to be marked `FAILED`.

Tagging uses two total processing attempts with backoff between attempts. When both attempts fail, the image is marked `FAILED` and the rejected message is routed to `image.tagging.failed.queue`. Successful messages are acknowledged and removed from the main queue.

## Configuration

Configuration is supplied through environment variables. Secrets must never be committed to the repository.

| Variable | Description |
| --- | --- |
| `DB_URL` | PostgreSQL JDBC URL |
| `DB_USER` | PostgreSQL username |
| `DB_PASSWORD` | PostgreSQL password |
| `RABBITMQ_HOST` | RabbitMQ host, defaults to `localhost` during development |
| `RABBITMQ_PORT` | RabbitMQ AMQP port, defaults to `5672` |
| `RABBITMQ_USER` | RabbitMQ username |
| `RABBITMQ_PASSWORD` | RabbitMQ password |
| `OBJECT_STORAGE_ENDPOINT` | Cloudflare R2 S3 endpoint |
| `OBJECT_STORAGE_REGION` | Object-storage region |
| `OBJECT_STORAGE_ACCESS_KEY_ID` | Cloudflare R2 access key |
| `OBJECT_STORAGE_SECRET_ACCESS_KEY` | Cloudflare R2 secret key |
| `OBJECT_STORAGE_BUCKET` | Private Cloudflare R2 bucket |
| `GEMINI_API_KEY` | Google Gemini API key |
| `GEMINI_MODEL` | Gemini model, defaults to `gemini-3.5-flash-lite` |
| `RESET_PASSWORD_URL` | Frontend URL used to create password-reset links |
| `RESEND_API_KEY` | Resend API key |
| `RESEND_FROM_EMAIL` | Verified sender used for password-reset emails |
| `RESEND_PASSWORD_RESET_TEMPLATE` | Published Resend password-reset template identifier |
| `SESSION_TIMEOUT` | Production session timeout, defaults to `30m` |
| `PORT` | HTTP port, defaults to `8080` |

## Local infrastructure

Docker Compose runs development-only PostgreSQL and RabbitMQ containers. It is not used for Railway deployment.

Create the local Docker Compose environment file from the committed example and replace the placeholder passwords:

```bash
cp .env.example .env
```

This `.env` file configures only the local PostgreSQL and RabbitMQ containers. Spring Boot does not automatically load it as the complete application configuration. Supply the application variables from the configuration table through your IDE, shell, or deployment platform.

Start the containers:

```bash
docker compose up -d
```

Check their health:

```bash
docker compose ps
```

Local service addresses:

```text
Docker PostgreSQL:    localhost:5433
RabbitMQ AMQP:        localhost:5672
RabbitMQ dashboard:   http://localhost:15672
```

When running Spring Boot outside Docker, configure it to use the Docker PostgreSQL instance with:

```text
DB_URL=jdbc:postgresql://localhost:5433/image_hosting
DB_USER=image_hosting
DB_PASSWORD=<value from .env>
RABBITMQ_USER=image_hosting
RABBITMQ_PASSWORD=<value from .env>
```

Stop the local containers with:

```bash
docker compose down
```

Named volumes preserve PostgreSQL and RabbitMQ data across normal container restarts. Running `docker compose down -v` also deletes those volumes and their stored development data.

### Observability configuration

The following variables are required only when the `loki` profile is active:

| Variable | Description |
| --- | --- |
| `LOKI_URL` | Grafana Cloud Loki base URL |
| `LOKI_USERNAME` | Grafana Cloud Loki username or tenant ID |
| `LOKI_API_TOKEN` | Grafana Cloud token with `logs:write` permission |
| `ENVIRONMENT` | Environment label, such as `development` or `production` |

The Loki API token is a secret. It must never be committed to the repository or included in application logs.

## Application profiles

The application uses separate Spring profiles:

- `dev` for local development
- `test` for automated tests
- `prod` for Railway deployment
- `loki` for shipping logs to Grafana Cloud Loki

Start the development profile with:

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

Start local development with Grafana Loki log shipping with:

```bash
SPRING_PROFILES_ACTIVE=dev,loki ./mvnw spring-boot:run
```

For production log shipping, use:

```text
SPRING_PROFILES_ACTIVE=prod,loki
ENVIRONMENT=production
```

## API

The API uses the following base path:

```text
http://localhost:8080/api/v1
```

Main endpoint groups:

- `/api/v1/auth` — registration, login, logout, and session information
- `/api/v1/images` — upload, gallery, search, ownership, and visibility
- `/api/v1/images/{imageId}/share-links` — private sharing-link management
- `/api/v1/shares/{token}` — anonymous access through a valid sharing token

The complete API contract is available in the [OpenAPI specification](docs/openapi.yaml).

## Swagger

When the application is running with the `dev` profile:

```text
Swagger UI:   http://localhost:8080/swagger-ui.html
OpenAPI JSON: http://localhost:8080/v3/api-docs
OpenAPI YAML: http://localhost:8080/v3/api-docs.yaml
```

Swagger UI and the generated OpenAPI endpoints are disabled with the `prod` profile. The committed [OpenAPI specification](docs/openapi.yaml) remains available as the production API contract.

Authentication uses an opaque `SESSION` cookie. Authenticated state-changing operations also require CSRF protection.

## Observability

Observability makes it possible to understand the application’s behavior while it is running.

The project currently uses application logs and health checks.

### Application logging

The application records important business events, including:

- User registration
- Successful authentication
- Failed authentication attempts
- Image uploads
- Image visibility changes
- Image deletion
- Image-tagging start, completion, skipping, and failure

The application uses standard log levels:

- `DEBUG` for detailed development information
- `INFO` for normal business operations
- `WARN` for suspicious or recoverable situations
- `ERROR` for unexpected failures

Sensitive information must never be logged, including:

- Passwords and password hashes
- Session identifiers
- CSRF tokens
- API keys
- Grafana tokens
- Storage credentials
- Private storage object keys

### Grafana Cloud Loki

When the `loki` profile is active, the Loki4j Logback appender sends application logs to Grafana Cloud Loki.

```text
Spring Boot application
    |
    v
SLF4J and Logback
    |
    v
Loki4j Logback Appender
    |
    v
Grafana Cloud Loki
    |
    v
Grafana Explore
```

Logs continue to appear in the application console while also being shipped to Loki.

The following Loki labels are attached to logs:

- `app`
- `env`
- `level`

The thread and logger names are stored as structured metadata.

Example LogQL query for development logs:

```logql
{app="image-hosting-service", env="development"}
```

Example LogQL query for production logs:

```logql
{app="image-hosting-service", env="production"}
```

Example query for application errors:

```logql
{app="image-hosting-service", level="ERROR"}
```

Example query for failed authentication attempts:

```logql
{app="image-hosting-service"} |= "Authentication failed"
```

### Health checks

Spring Boot Actuator provides the application health endpoint:

```text
GET /actuator/health
```

Local health URL:

```text
http://localhost:8080/actuator/health
```

A healthy application returns:

```json
{
  "status": "UP"
}
```

The health system can check application availability, database connectivity, disk space, liveness, and readiness.

Detailed health information is available during local development. Production responses hide internal details for security.

Application metrics are not currently exposed publicly.

## Testing

Run the complete test suite with:

```bash
./mvnw clean verify
```

Automated tests use isolated configuration and must not call production PostgreSQL, Cloudflare R2, or Gemini services.

## Docker

Build the application image:

```bash
docker build -t image-hosting-service .
```

Run the container with the required environment variables:

```bash
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL \
  -e DB_USER \
  -e DB_PASSWORD \
  -e RABBITMQ_HOST \
  -e RABBITMQ_PORT \
  -e RABBITMQ_USER \
  -e RABBITMQ_PASSWORD \
  -e OBJECT_STORAGE_ENDPOINT \
  -e OBJECT_STORAGE_REGION \
  -e OBJECT_STORAGE_ACCESS_KEY_ID \
  -e OBJECT_STORAGE_SECRET_ACCESS_KEY \
  -e OBJECT_STORAGE_BUCKET \
  -e GEMINI_API_KEY \
  -e GEMINI_MODEL \
  -e RESET_PASSWORD_URL \
  -e RESEND_API_KEY \
  -e RESEND_FROM_EMAIL \
  -e RESEND_PASSWORD_RESET_TEMPLATE \
  -e SESSION_TIMEOUT \
  image-hosting-service
```

To run the container with Grafana Loki log shipping:

```bash
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod,loki \
  -e ENVIRONMENT=production \
  -e DB_URL \
  -e DB_USER \
  -e DB_PASSWORD \
  -e RABBITMQ_HOST \
  -e RABBITMQ_PORT \
  -e RABBITMQ_USER \
  -e RABBITMQ_PASSWORD \
  -e OBJECT_STORAGE_ENDPOINT \
  -e OBJECT_STORAGE_REGION \
  -e OBJECT_STORAGE_ACCESS_KEY_ID \
  -e OBJECT_STORAGE_SECRET_ACCESS_KEY \
  -e OBJECT_STORAGE_BUCKET \
  -e GEMINI_API_KEY \
  -e GEMINI_MODEL \
  -e RESET_PASSWORD_URL \
  -e RESEND_API_KEY \
  -e RESEND_FROM_EMAIL \
  -e RESEND_PASSWORD_RESET_TEMPLATE \
  -e SESSION_TIMEOUT \
  -e LOKI_URL \
  -e LOKI_USERNAME \
  -e LOKI_API_TOKEN \
  image-hosting-service
```

## Deployment

The backend and PostgreSQL database are designed for deployment on Railway. Original images and thumbnails remain in a private Cloudflare R2 bucket.

Railway-specific configuration is kept outside the application’s business logic so the Docker image can later be moved to another hosting provider.

For production log shipping, the Railway service uses the `prod,loki` profiles and sends application logs to Grafana Cloud Loki.

## Security

- Passwords are hashed with bcrypt
- Sessions use opaque identifiers
- Session cookies are HTTP-only
- Production cookies are secure
- Authenticated mutations use CSRF protection
- Cloudflare R2 remains private
- Image access is controlled by the backend
- Ownership is verified for private resources
- Public UUIDs are used in API URLs
- Sharing tokens are securely generated and stored as hashes
- Invalid, expired, and revoked sharing links are rejected
- Credentials and tokens are excluded from logs
- Inaccessible private images return non-enumerating responses

## Documentation

- [OpenAPI specification](docs/openapi.yaml)
- [Database ERD](docs/database-erd.md)

## License

This project is licensed under the [MIT License](LICENSE).
