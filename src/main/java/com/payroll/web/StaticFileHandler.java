package com.payroll.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * Serves static web assets (HTML, CSS, JS, images, fonts) from the filesystem
 * or from the application classpath with proper MIME types, CORS headers,
 * and Single Page Application (SPA) routing fallback.
 */
public class StaticFileHandler implements HttpHandler {

    private final File baseDir;
    private final String classpathPrefix;
    private static final Map<String, String> MIME_TYPES = new HashMap<>();

    static {
        MIME_TYPES.put("html", "text/html; charset=UTF-8");
        MIME_TYPES.put("htm", "text/html; charset=UTF-8");
        MIME_TYPES.put("css", "text/css; charset=UTF-8");
        MIME_TYPES.put("js", "application/javascript; charset=UTF-8");
        MIME_TYPES.put("json", "application/json; charset=UTF-8");
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("jpg", "image/jpeg");
        MIME_TYPES.put("jpeg", "image/jpeg");
        MIME_TYPES.put("gif", "image/gif");
        MIME_TYPES.put("svg", "image/svg+xml");
        MIME_TYPES.put("ico", "image/x-icon");
        MIME_TYPES.put("pdf", "application/pdf");
        MIME_TYPES.put("woff", "font/woff");
        MIME_TYPES.put("woff2", "font/woff2");
        MIME_TYPES.put("ttf", "font/ttf");
        MIME_TYPES.put("eot", "application/vnd.ms-fontobject");
        MIME_TYPES.put("txt", "text/plain; charset=UTF-8");
    }

    public StaticFileHandler(File baseDir, String classpathPrefix) {
        this.baseDir = baseDir;
        this.classpathPrefix = classpathPrefix;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod().toUpperCase();
        if ("OPTIONS".equals(method)) {
            addCorsHeaders(exchange);
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        if (!"GET".equals(method) && !"HEAD".equals(method)) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        URI uri = exchange.getRequestURI();
        String path = uri.getPath();

        // Disallow path traversal
        if (path.contains("..")) {
            send404(exchange);
            return;
        }

        if (path.isEmpty() || path.equals("/")) {
            path = "/index.html";
        }

        // Try local file first
        File targetFile = new File(baseDir, path.startsWith("/") ? path.substring(1) : path);
        if (targetFile.isDirectory()) {
            targetFile = new File(targetFile, "index.html");
        }

        byte[] content = null;
        String contentType = "application/octet-stream";

        if (targetFile.exists() && targetFile.isFile()) {
            content = Files.readAllBytes(targetFile.toPath());
            contentType = getMimeType(targetFile.getName());
        } else {
            // Try classpath resource
            String resourcePath = classpathPrefix + (path.startsWith("/") ? path : "/" + path);
            try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
                if (in != null) {
                    content = in.readAllBytes();
                    contentType = getMimeType(path);
                }
            } catch (Exception ignored) {}
        }

        // SPA Fallback: If not found and not an asset file with extension, serve index.html
        if (content == null && !path.contains(".")) {
            File indexFile = new File(baseDir, "index.html");
            if (indexFile.exists() && indexFile.isFile()) {
                content = Files.readAllBytes(indexFile.toPath());
                contentType = "text/html; charset=UTF-8";
            } else {
                String resourcePath = classpathPrefix + "/index.html";
                try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
                    if (in != null) {
                        content = in.readAllBytes();
                        contentType = "text/html; charset=UTF-8";
                    }
                } catch (Exception ignored) {}
            }
        }

        if (content == null) {
            send404(exchange);
            return;
        }

        addCorsHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
        exchange.sendResponseHeaders(200, content.length);

        if (!"HEAD".equals(method)) {
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(content);
            }
        }
        exchange.close();
    }

    private void send404(HttpExchange exchange) throws IOException {
        String msg = "404 Not Found";
        byte[] bytes = msg.getBytes();
        addCorsHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(404, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
        exchange.close();
    }

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization, Accept");
    }

    private String getMimeType(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot >= 0 && dot < filename.length() - 1) {
            String ext = filename.substring(dot + 1).toLowerCase();
            return MIME_TYPES.getOrDefault(ext, "application/octet-stream");
        }
        return "application/octet-stream";
    }
}
