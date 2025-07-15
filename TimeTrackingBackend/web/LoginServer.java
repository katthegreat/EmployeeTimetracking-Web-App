package TimeTrackingBackend;

import static spark.Spark.*;
import com.google.gson.Gson;
import spark.Spark;

public class LoginServer {
    public static void main(String[] args) {
        port(4567);
        Gson gson = new Gson();

        // Enable CORS
        Spark.options("/*", (request, response) -> {
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }
            
            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }
            return "OK";
        });

        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.header("Access-Control-Allow-Headers", "Content-Type, Authorization, Accept");
            
            // Enhanced logging
            System.out.println("\n=== Received Request ===");
            System.out.println("Method: " + request.requestMethod());
            System.out.println("Path: " + request.pathInfo());
            System.out.println("Content-Type: " + request.contentType());
            System.out.println("Headers: " + request.headers());
            System.out.println("Body: " + request.body());
            System.out.println("=======================");
        });

        // Main login endpoint
        post("/api/login", "application/json", (request, response) -> {
            try {
                LoginRequest loginRequest = gson.fromJson(request.body(), LoginRequest.class);
                response.type("application/json");

                // Authentication logic
                if ("employee".equals(loginRequest.username) && "password".equals(loginRequest.password)) {
                    return gson.toJson(new LoginResponse(true, "employee", "John Doe"));
                } else if ("admin".equals(loginRequest.username) && "adminpass".equals(loginRequest.password)) {
                    return gson.toJson(new LoginResponse(true, "admin", "Admin User"));
                }
                
                response.status(401); // Unauthorized
                return gson.toJson(new LoginResponse(false, null, null));
                
            } catch (Exception e) {
                response.status(400); // Bad Request
                return gson.toJson(new ErrorResponse("Invalid request format: " + e.getMessage()));
            }
        });

        // Error handling
        Spark.notFound((request, response) -> {
            response.type("application/json");
            response.status(404);
            return gson.toJson(new ErrorResponse("Endpoint not found"));
        });

        Spark.exception(Exception.class, (exception, request, response) -> {
            response.type("application/json");
            response.status(500);
            response.body(gson.toJson(new ErrorResponse("Internal server error: " + exception.getMessage())));
            exception.printStackTrace(); // Log the exception for debugging
        });
    }

    // Data classes with proper constructors
    static class LoginRequest {
        String username;
        String password;
        
        public LoginRequest() {} // Default constructor for Gson
    }

    static class LoginResponse {
        boolean success;
        String role;
        String employeeName;
        
        public LoginResponse(boolean success, String role, String employeeName) {
            this.success = success;
            this.role = role;
            this.employeeName = employeeName;
        }
    }

    static class ErrorResponse {
        String error;
        
        public ErrorResponse(String error) {
            this.error = error;
        }
    }
}