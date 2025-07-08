import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class PayrollServer {
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/payroll", new PayrollHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Server started on http://localhost:8080");
    }

    static class PayrollHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String response = """
            [
              { "name": "Alice", "hoursWorked": 40, "hourlyRate": 20, "totalPay": 800 },
              { "name": "Bob", "hoursWorked": 32, "hourlyRate": 22, "totalPay": 704 }
            ]
            """;
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}
