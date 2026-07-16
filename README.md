# Image Hosting Service

A secure, AI-powered image hosting platform built with Spring Boot.

The application allows users to upload, manage, search, and securely share images. Uploaded images are stored in private object storage and automatically analyzed by a vision-capable AI model to generate searchable objects, descriptive tags, and prominent colors.

## Project Status

🚧 **In development**

The project is currently in the design and implementation phase.

## Features

### Authentication and Sessions

* User registration
* User login and logout
* Secure password hashing with bcrypt
* Opaque database-backed sessions
* Session expiration
* Secure cookie-based authentication

### Image Management

* Upload images up to 10 MB
* View all images uploaded by the authenticated user
* Delete owned images
* Public and private image visibility
* Secure image access through backend proxy endpoints
* Image metadata including file type, dimensions, and file size

### AI Image Tagging

Each uploaded image is analyzed by a vision-capable AI model.

The generated metadata includes:

* Objects visible in the image
* Descriptive tags such as setting, mood, weather, and time of day
* Up to three prominent colors

AI-generated metadata is stored in PostgreSQL using JSONB.

### Search and Discovery

* Search public images using free text
* Search through AI-generated objects, tags, and colors
* View the latest 50 public images
* Use generated thumbnails for faster image browsing

### Image Processing

* Asynchronous AI image tagging
* Image-processing status tracking
* 100 × 100 thumbnail generation
* AI-tagging duration measurement
* Failure handling for unsuccessful image processing

### Private Image Sharing

Users can generate temporary share links for private images.

Share links:

* Use secure random tokens
* Expire after a selected period
* Can be revoked by the image owner
* Allow access without requiring an account

## Technology Stack

### Backend

* Java
* Spring Boot
* Spring Security
* Spring Session JDBC
* Spring JDBC
* PostgreSQL
* Flyway

### Storage and Image Processing

* Backblaze B2
* S3-compatible private object storage
* Image-processing library for thumbnail generation

### AI Integration

* Vision-capable AI model
* Structured AI responses
* JSON-based image metadata

### Infrastructure

* Docker
* GitHub Actions
* GitHub Container Registry
* Cloud deployment

### Frontend

A lightweight frontend application will be created to demonstrate the backend functionality.

## Application Flow

### Image Upload

1. An authenticated user uploads an image.
2. The backend validates the file type and size.
3. The original image is stored in private object storage.
4. Image metadata is saved in PostgreSQL.
5. A thumbnail is generated.
6. AI analysis starts asynchronously.
7. Generated tags, objects, and colors are stored in the database.
8. The image becomes searchable when processing is complete.

### Image Access

Images are stored in a private bucket and are not exposed directly.

The backend verifies access permissions and streams the requested image from object storage to the client.

Public images can be viewed by everyone. Private images can only be accessed by their owner or through a valid share link.

## Security

The application is designed with the following security measures:

* Bcrypt password hashing
* Opaque session identifiers
* Database-backed session expiration
* HTTP-only authentication cookies
* Image ownership validation
* Private object-storage buckets
* Secure share-link tokens
* File-size restrictions
* Image-content validation
* Random object-storage keys
* Restricted image formats

## Documentation

Additional project documentation is available in the [`docs`](./docs) directory.

The documentation will include:

* Project scope
* Database entity-relationship diagram
* API design
* Application architecture
* Deployment information

## Testing

The project will include automated tests for:

* Authentication and session management
* Image upload validation
* Image ownership and authorization
* Public and private image access
* Image deletion
* AI-tagging success and failure
* Search functionality
* Thumbnail generation
* Share-link expiration and revocation

External AI and object-storage services will be replaced with test implementations during automated testing.

## CI/CD

GitHub Actions will be used to:

* Run automated tests
* Build the Spring Boot application
* Build a Docker image
* Push the Docker image to GitHub Container Registry

## Getting Started

Local development and deployment instructions will be added as the project implementation progresses.

The final documentation will include:

* Required software
* Environment variables
* Database setup
* Object-storage configuration
* AI-provider configuration
* Local development instructions
* Docker commands

## License

This project is developed for educational and portfolio purposes.
