import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

public class CloudXGamingHub {
    private static Map<String, String> sessions = new HashMap<>();

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/", ex -> sendFile(ex, "templates/login.html"));
        server.createContext("/signup", ex -> sendFile(ex, "templates/signup.html"));
        server.createContext("/dashboard", CloudXGamingHub::dashboardHandler);
        server.createContext("/loginAction", CloudXGamingHub::loginHandler);
        server.createContext("/signupAction", CloudXGamingHub::signupHandler);
        server.createContext("/bookSlot", CloudXGamingHub::bookHandler);

        server.setExecutor(null);
        server.start();
        System.out.println("🚀 Server running on http://localhost:8080/");
    }

    private static void sendFile(HttpExchange ex, String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        ex.sendResponseHeaders(200, bytes.length);
        OutputStream os = ex.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private static void signupHandler(HttpExchange ex) throws IOException {
        if ("POST".equals(ex.getRequestMethod())) {
            String form = new String(ex.getRequestBody().readAllBytes());
            Map<String, String> data = parseForm(form);
            boolean ok = db.registerUser(data.get("name"), data.get("pass"));
            redirect(ex, ok ? "/" : "/signup?error=exists");
        }
    }

    private static void loginHandler(HttpExchange ex) throws IOException {
        if ("POST".equals(ex.getRequestMethod())) {
            String form = new String(ex.getRequestBody().readAllBytes());
            Map<String, String> data = parseForm(form);
            boolean ok = db.loginUser(data.get("name"), data.get("pass"));
            if (ok) {
                String token = UUID.randomUUID().toString();
                sessions.put(token, data.get("name"));
                redirectWithCookie(ex, "/dashboard", "session=" + token);
            } else {
                redirect(ex, "/?error=invalid");
            }
        }
    }

    private static void dashboardHandler(HttpExchange ex) throws IOException {
        String user = getUserFromCookie(ex);
        if (user == null) {
            redirect(ex, "/");
            return;
        }
        String html = Files.readString(Paths.get("templates/dashboard.html")).replace("{{user}}", user);
        ex.sendResponseHeaders(200, html.getBytes().length);
        ex.getResponseBody().write(html.getBytes());
        ex.close();
    }

    private static void bookHandler(HttpExchange ex) throws IOException {
        String user = getUserFromCookie(ex);
        if (user == null) {
            redirect(ex, "/");
            return;
        }
        String form = new String(ex.getRequestBody().readAllBytes());
        Map<String, String> data = parseForm(form);
        db.bookSlot(user, data.get("game"), data.get("slot"), Integer.parseInt(data.get("price")));
        redirect(ex, "/dashboard");
    }

    private static Map<String, String> parseForm(String form) {
        Map<String, String> map = new HashMap<>();
        for (String pair : form.split("&")) {
            String[] kv = pair.split("=");
            if (kv.length == 2)
                map.put(URLDecoder.decode(kv[0], java.nio.charset.StandardCharsets.UTF_8),
                        URLDecoder.decode(kv[1], java.nio.charset.StandardCharsets.UTF_8));
        }
        return map;
    }

    private static void redirect(HttpExchange ex, String url) throws IOException {
        ex.getResponseHeaders().add("Location", url);
        ex.sendResponseHeaders(302, -1);
        ex.close();
    }

    private static void redirectWithCookie(HttpExchange ex, String url, String cookie) throws IOException {
        ex.getResponseHeaders().add("Set-Cookie", cookie);
        redirect(ex, url);
    }

    private static String getUserFromCookie(HttpExchange ex) {
        List<String> cookies = ex.getRequestHeaders().get("Cookie");
        if (cookies == null) return null;
        for (String c : cookies) {
            for (String part : c.split(";")) {
                if (part.trim().startsWith("session=")) {
                    String token = part.trim().substring(8);
                    return sessions.get(token);
                }
            }
        }
        return null;
    }
}
