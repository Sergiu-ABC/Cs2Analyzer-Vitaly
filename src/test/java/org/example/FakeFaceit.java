package org.example;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/* Minimal in-process stand-in for the FACEIT Data API, so tests never touch the network. */
class FakeFaceit implements AutoCloseable {

    record Reply(int status, String body) {}

    final List<String> requests = new CopyOnWriteArrayList<>();
    final Map<String, AtomicInteger> hits = new ConcurrentHashMap<>();
    private final HttpServer server;

    FakeFaceit(Function<String, Reply> router) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String raw = exchange.getRequestURI().getRawPath()
                    + (exchange.getRequestURI().getRawQuery() != null ? "?" + exchange.getRequestURI().getRawQuery() : "");
            requests.add(raw);
            hits.computeIfAbsent(exchange.getRequestURI().getPath(), k -> new AtomicInteger()).incrementAndGet();
            Reply reply = router.apply(raw);
            byte[] bytes = reply.body == null ? new byte[0] : reply.body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(reply.status, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
            exchange.close();
        });
        server.start();
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
