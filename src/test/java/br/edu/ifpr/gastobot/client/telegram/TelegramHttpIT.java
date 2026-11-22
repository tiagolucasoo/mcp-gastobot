package br.edu.ifpr.gastobot.client.telegram;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Usa servidor HTTP local falso. Nenhuma chamada ou mensagem vai para o Telegram. */
class TelegramHttpIT {
    @Test
    void enviaJsonUtf8ERecebeUpdatesComOffset() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var json = new JsonMapper();
        List<JsonNode> bodies = java.util.Collections.synchronizedList(new ArrayList<>());
        List<String> paths = java.util.Collections.synchronizedList(new ArrayList<>());
        server.createContext("/", exchange -> {
            paths.add(exchange.getRequestURI().getPath());
            bodies.add(json.readTree(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            String resposta = exchange.getRequestURI().getPath().endsWith("getUpdates")
                    ? "{\"ok\":true,\"result\":[]}" : "{\"ok\":true,\"result\":{\"message_id\":1}}";
            byte[] bytes = resposta.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (var output = exchange.getResponseBody()) {
                output.write(bytes);
            }
            exchange.close();
        });
        server.start();
        try (var http = HttpClient.newHttpClient()) {
            var api = new TelegramApiClient(http, URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/bot123:test-only/"));
            assertTrue(api.receber(42).isEmpty());
            api.enviar(123, "Almoço R$ 35,90");
            assertEquals(List.of("/bot123:test-only/getUpdates", "/bot123:test-only/sendMessage"), paths);
            assertEquals(42, bodies.getFirst().path("offset").asLong());
            assertEquals(25, bodies.getFirst().path("timeout").asInt());
            assertEquals("message", bodies.getFirst().path("allowed_updates").get(0).asString());
            assertEquals(123, bodies.get(1).path("chat_id").asLong());
            assertEquals("Almoço R$ 35,90", bodies.get(1).path("text").asString());
            assertFalse(bodies.get(1).has("parse_mode"));
        } finally {
            server.stop(0);
        }
    }
}
