# Auction Platform Backend

This project is the backend for a real-time auction platform. It provides a RESTful API for managing users, items, and bidding. The backend is built with Spring Boot and uses a SQLite database.

## Features

*   **User Management:**
    *   User registration and login.
    *   Secure authentication using JSON Web Tokens (JWT).
    *   Refresh token mechanism for seamless sessions.
*   **Item Management:**
    *   Publishing items for auction.
    *   Retrieving a list of all items.
*   **Bidding:**
    *   Real-time bidding on items (to be implemented).

## Technologies Used

*   **Framework:** Spring Boot
*   **Language:** Java
*   **Database:** SQLite
*   **Security:** Spring Security, JWT
*   **API Documentation:** Swagger (Springdoc)

## Getting Started

### Prerequisites

*   Java 26 or higher
*   Gradle

### Running the Application

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/your-username/auction-backend.git
    ```
2.  **Navigate to the project directory:**
    ```bash
    cd auction-backend
    ```
3.  **Run the application using Gradle:**
    ```bash
    ./gradlew bootRun
    ```
The application will start on port `8080`.

## API Endpoints

The API is documented using Swagger. You can access the Swagger UI at:
[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

### User Endpoints

*   `POST /users/register`: Register a new user.
*   `POST /users/login`: Log in a user and receive an access token and refresh token.
*   `POST /users/refresh`: Refresh an access token using a refresh token.

### Item Endpoints

*   `POST /items/publish`: Publish a new item for auction.
*   `GET /items`: Get a list of all items.

## How It Works

The application follows a standard layered architecture:

*   **Controller Layer (`*Controller.java`):**
    *   Exposes the REST API endpoints.
    *   Handles incoming HTTP requests and responses.
    *   Delegates business logic to the service layer.
*   **Service Layer (`*Service.java`):**
    *   Contains the core business logic of the application.
    *   Interacts with the repository layer to access the database.
*   **Repository Layer (`*Repository.java`):**
    *   Provides an interface for data access using Spring Data JPA.
    *   Interacts with the SQLite database.

### Authentication Flow

1.  **Login:** A user logs in with their credentials. The server validates them and returns an access token (short-lived) and a refresh token (long-lived).
2.  **API Access:** For protected endpoints, the user sends the access token in the `Authorization` header.
3.  **Token Expiration:** If the access token is expired, the server returns a `401 Unauthorized` error.
4.  **Token Refresh:** The client then uses the refresh token to request a new access token from the `/users/refresh` endpoint.
5.  **Retry:** The client retries the original request with the new access token.
