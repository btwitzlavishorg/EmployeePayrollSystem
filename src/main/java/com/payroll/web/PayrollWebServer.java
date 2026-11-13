package com.payroll.web;

import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.Executors;

/**
 * Embedded High-Performance Web Server using standard Java 11 JDK HttpServer.
 * Serves static frontend assets and REST API endpoints concurrently.
 */
public class PayrollWebServer {

    private final int port;
    private HttpServer server;

    public PayrollWebServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(16));

        ApiController controller = new ApiController();
        server.createContext("/api", new ApiHandler(controller));

        File frontendDir = resolveFrontendDir();
        server.createContext("/", new StaticFileHandler(frontendDir, "/frontend"));

        server.start();
        System.out.println("[PayrollWebServer] HTTP Server successfully started on port " + port);
        System.out.println("[PayrollWebServer] Frontend root: " +
                (frontendDir != null && frontendDir.exists() ? frontendDir.getAbsolutePath() : "classpath:/frontend"));
    }

    private File resolveFrontendDir() {
        File cwdFrontend = new File("frontend");
        if (cwdFrontend.isDirectory() && new File(cwdFrontend, "index.html").exists()) {
            return cwdFrontend;
        }

        File resourcesFrontend = new File("src/main/resources/frontend");
        if (resourcesFrontend.isDirectory() && new File(resourcesFrontend, "index.html").exists()) {
            return resourcesFrontend;
        }

        try {
            URI codeSource = PayrollWebServer.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            File jarOrClasses = new File(codeSource);
            File jarDir = jarOrClasses.isFile() ? jarOrClasses.getParentFile() : jarOrClasses;
            File besideJar = new File(jarDir, "frontend");
            if (besideJar.isDirectory() && new File(besideJar, "index.html").exists()) {
                return besideJar;
            }
        } catch (Exception ignored) {}

        // Classpath fallback handled inside StaticFileHandler
        return new File("frontend");
    }

    public void stop() {
        if (server != null) {
            server.stop(1);
            System.out.println("[PayrollWebServer] HTTP Server stopped.");
        }
    }

    public int getPort() {
        return port;
    }
}
