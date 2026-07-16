# Project Requirements

## Overview

The Image Hosting Service is a web application that allows users to upload, manage, search, and securely share images.

Uploaded images are stored in private object storage and analyzed by a vision-capable AI model. The generated image metadata is stored in PostgreSQL and used to support free-text image search.

## Objectives

The project demonstrates:

* Secure user authentication
* Database-backed session management
* File upload validation
* Private object storage
* Image processing
* AI integration
* Asynchronous operations
* PostgreSQL JSONB usage
* Authorization and resource ownership
* Automated testing
* Docker deployment
* Continuous integration

## Functional Requirements

### User Authentication

The system must support:

* User registration
* User login
* User logout
* Password hashing with bcrypt
* Opaque session identifiers
* Session storage in PostgreSQL
* Session expiration
* Cookie-based authentication

### Image Upload

Authenticated users must be able to:

* Upload image files
* Upload files up to 10 MB
* Select public or private visibility
* View the processing status of uploaded images

The system must validate:

* File size
* Supported file types
* Image content
* Image dimensions
* File integrity

### Image Management

Authenticated users must be able to:

* View all images they uploaded
* View image metadata
* Delete images they own
* Access their private images
* Create temporary sharing links for private images

Users must not be able to modify or delete images owned by another user.

### Public Images

Public images must:

* Be visible without authentication
* Appear on the homepage
* Appear in public search results
* Be accessible through the backend API

The homepage must display the latest 50 public images.

### Private Images

Private images must:

* Be visible only to their owner
* Be excluded from the public homepage
* Be excluded from public search results
* Be accessible through valid temporary share links

### Object Storage

The system must:

* Store original image files in private object storage
* Store generated thumbnails in private object storage
* Store only file references and metadata in PostgreSQL
* Deliver image content through backend proxy endpoints
* Prevent direct public access to the storage bucket

An S3-compatible storage provider such as Backblaze B2 may be used.

### AI Image Tagging

Each uploaded image must be analyzed by a vision-capable AI model.

The AI response must include:

#### Objects

Physical objects visible in the image, such as:

* Person
* Car
* Tree
* Animal
* Building

#### Tags

Descriptive information, such as:

* Setting
* Weather
* Mood
* Activity
* Time of day
* Scene type

#### Colors

Up to three prominent colors selected from:

* Black
* White
* Red
* Green
* Yellow
* Blue
* Brown
* Orange
* Pink
* Purple
* Gray

AI-generated metadata must be stored using PostgreSQL JSONB.

### Asynchronous Processing

AI image tagging must run asynchronously.

The upload request must not wait for AI processing to complete.

Images must support the following processing states:

* Pending
* Processing
* Completed
* Failed

The system should record:

* Processing start time
* Processing completion time
* Processing duration
* Processing errors

### Thumbnail Generation

The system must generate a square thumbnail for each uploaded image.

Thumbnail requirements:

* Dimensions: 100 × 100 pixels
* Used on the homepage
* Used in search results
* Used in the user image library
* Stored in private object storage

### Image Search

Users and guests must be able to search public images using free text.

Search must use AI-generated:

* Objects
* Tags
* Colors

Private images must never appear in public search results.

Search results should return image metadata and thumbnail URLs instead of full image content.

### Expiring Share Links

Authenticated users must be able to create temporary share links for private images.

A share link must:

* Belong to one image
* Use a secure random token
* Have an expiration date
* Be revocable by the image owner
* Allow access without authentication
* Stop working after expiration
* Stop working after revocation

Supported expiration options may include:

* One hour
* One day
* Seven days

## Frontend Requirements

A lightweight frontend application will be created to demonstrate the backend functionality.

The frontend should include:

* Registration page
* Login page
* Homepage
* Search page
* Upload page
* User image library
* Image details page
* Share-link management

The frontend should prioritize functionality and clear API integration over complex visual design.

## Security Requirements

The system must include:

* Bcrypt password hashing
* Opaque session identifiers
* Database-backed session expiration
* HTTP-only authentication cookies
* Image ownership validation
* Private storage buckets
* Secure share-link tokens
* File-size restrictions
* Image-content validation
* Restricted supported image formats
* Random object-storage keys
* Protection against unauthorized access

Sensitive configuration values must be provided through environment variables and must not be committed to the repository.

## Testing Requirements

Automated tests should cover:

* Registration
* Login
* Logout
* Session authentication
* Session expiration
* Upload validation
* Image ownership
* Public image access
* Private image access
* Image deletion
* AI-tagging success
* AI-tagging failure
* Search functionality
* Thumbnail generation
* Share-link access
* Share-link expiration
* Share-link revocation

External AI and object-storage services should be replaced with test implementations during automated testing.

## Deployment Requirements

The application must:

* Run inside a Docker container
* Use PostgreSQL as its database
* Use environment-based configuration
* Be deployable to a cloud platform

## Continuous Integration

GitHub Actions should:

* Run automated tests
* Build the application
* Build a Docker image
* Push the Docker image to GitHub Container Registry

## Documentation

The repository should include:

* Project README
* Project requirements
* Database entity-relationship diagram
* API documentation
* Architecture diagram
* Environment-variable documentation
* Local setup instructions
* Docker instructions
* Testing instructions
* Deployment information

