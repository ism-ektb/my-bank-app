package ru.ism.frontapp.client;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

final class StubHttpServer implements AutoCloseable {

    private final HttpServer server;
    private final Map<String, Response> responses = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Request> requests = new ConcurrentLinkedQueue<>();

    StubHttpServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requests.add(new Request(
                    exchange.getRequestMethod(),
                    exchange.getRequestURI().getPath(),
                    exchange.getRequestURI().getRawQuery(),
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            Response response = responses.getOrDefault(exchange.getRequestURI().getPath(),
                    new Response(404, "application/json", ""));
            byte[] body = response.body().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", response.contentType());
            exchange.sendResponseHeaders(response.status(), body.length == 0 ? -1 : body.length);
            if (body.length > 0) {
                exchange.getResponseBody().write(body);
            }
            exchange.close();
        });
        server.start();
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    void respond(String path, int status, String contentType, String body) {
        responses.put(path, new Response(status, contentType, body));
    }

    Request lastRequest() {
        return requests.peek() == null ? null : requests.peek();
    }

    int requestCount() {
        return requests.size();
    }

    List<Request> requests() {
        return List.copyOf(requests);
    }

    @Override
    public void close() {
        server.stop(0);
    }

    record Request(String method, String path, String query, String body) {
    }

    private record Response(int status, String contentType, String body) {
    }
}
