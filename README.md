# Image Hosting Service

A secure, AI-powered image-hosting REST API built with Spring Boot.

Users can upload and manage images that are private by default. Images are stored in a private Cloudflare R2 bucket and delivered through authorization-aware API endpoints. Gemini analyzes uploaded images asynchronously to generate searchable objects, descriptive tags, and prominent colors.

## Features

- Registration, login, logout, and database-backed sessions
- Automatic login after registration
- JPEG, PNG, and WebP uploads up to 10 MB
- Private-by-default image access
- Public/private visibility management
- Personal image library with pagination
- Public gallery using 100 × 100 thumbnails
- Asynchronous Gemini image analysis
- AI-processing status tracking
- Search through AI-generated objects, tags, and colors
- Expiring and revocable private sharing links
- Owner-only image deletion
- Interactive Swagger API documentation

## Technology

- Java 26
- Spring Boot 4.1
- Spring Security
- Spring Session JDBC
- Spring JDBC
- PostgreSQL
- Flyway
- Cloudflare R2
- Google Gemini
- Thumbnailator
- Maven
- Docker
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
    `-- Gemini: asynchronous image analysis
```

PostgreSQL uses numeric internal identifiers for database relationships. Public API resources use UUIDs so internal database identifiers are not exposed.

## Configuration

Configuration is supplied through environment variables. Secrets must never be committed to the repository.

| Variable | Description |
| --- | --- |
| `DB_URL` | PostgreSQL JDBC URL |
| `DB_USER` | PostgreSQL username |
| `DB_PASSWORD` | PostgreSQL password |
| `OBJECT_STORAGE_ENDPOINT` | Cloudflare R2 S3 endpoint |
| `OBJECT_STORAGE_REGION` | Object-storage region |
| `OBJECT_STORAGE_ACCESS_KEY_ID` | Cloudflare R2 access key |
| `OBJECT_STORAGE_SECRET_ACCESS_KEY` | Cloudflare R2 secret key |
| `OBJECT_STORAGE_BUCKET` | Private Cloudflare R2 bucket |
| `GEMINI_API_KEY` | Google Gemini API key |
| `PORT` | HTTP port, defaults to `8080` |

## Application profiles

The application uses separate Spring profiles:

- `dev` for local development
- `test` for automated tests
- `prod` for Railway deployment

Start the development profile with:

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
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

When the application is running:

```text
Swagger UI:   http://localhost:8080/swagger-ui.html
OpenAPI JSON: http://localhost:8080/v3/api-docs
OpenAPI YAML: http://localhost:8080/v3/api-docs.yaml
```

Authentication uses an opaque `SESSION` cookie. Authenticated state-changing operations also require CSRF protection.

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
  -e OBJECT_STORAGE_ENDPOINT \
  -e OBJECT_STORAGE_REGION \
  -e OBJECT_STORAGE_ACCESS_KEY_ID \
  -e OBJECT_STORAGE_SECRET_ACCESS_KEY \
  -e OBJECT_STORAGE_BUCKET \
  -e GEMINI_API_KEY \
  image-hosting-service
```

## Deployment

The backend and PostgreSQL database are designed for deployment on Railway. Original images and thumbnails remain in a private Cloudflare R2 bucket.

Railway-specific configuration is kept outside the application’s business logic so the Docker image can later be moved to another hosting provider.

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