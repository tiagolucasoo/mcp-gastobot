package br.edu.ifpr.gastobot.client.telegram;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class TelegramApiClient implements TelegramGateway {
    private final HttpClient http;
    private final URI endpoint;
    private final JsonMapper json = new JsonMapper();

    public TelegramApiClient(HttpClient http, String token) {
        this(http, URI.create("https://api.telegram.org/bot" + token + "/"));
    }

    TelegramApiClient(HttpClient http, URI endpoint) {
        this.http = java.util.Objects.requireNonNull(http);
        this.endpoint = java.util.Objects.requireNonNull(endpoint);
    }

    @Override
    public List<TelegramUpdate> receber(long offset) throws InterruptedException {
        JsonNode result = chamar("getUpdates", Map.of("offset", offset, "timeout", 25,
                "allowed_updates", List.of("message")));
        if (!result.isArray()) {
            throw new TelegramApiException(0);
        }
        var updates = new ArrayList<TelegramUpdate>();
        for (var update : result) {
            if (!update.path("update_id").isIntegralNumber()) {
                continue;
            }
            updates.add(new TelegramUpdate(update.path("update_id").asLong(), mensagem(update.path("message"))));
        }
        return List.copyOf(updates);
    }

    private TelegramMessage mensagem(JsonNode message) {
        if (!message.path("text").isString() || !message.path("from").path("id").isIntegralNumber()
                || !message.path("chat").path("id").isIntegralNumber()) {
            return null;
        }
        return new TelegramMessage(message.path("chat").path("id").asLong(),
                message.path("from").path("id").asLong(),
                "private".equals(message.path("chat").path("type").asString()),
                message.path("from").path("is_bot").asBoolean(false), message.path("text").asString());
    }

    @Override
    public void enviar(long chatId, String texto) throws InterruptedException {
        chamar("sendMessage", Map.of("chat_id", chatId, "text", texto));
    }

    private JsonNode chamar(String metodo, Map<String, Object> dados) throws InterruptedException {
        try {
            var request = HttpRequest.newBuilder(endpoint.resolve(metodo))
                    .timeout(Duration.ofSeconds(40)).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(dados))).build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new TelegramApiException(response.statusCode());
            }
            JsonNode envelope = json.readTree(response.body());
            if (!envelope.path("ok").asBoolean(false)) {
                throw new TelegramApiException(envelope.path("error_code").asInt(0));
            }
            return envelope.path("result");
        } catch (IOException exception) {
            throw new TelegramApiException(0);
        } catch (TelegramApiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new TelegramApiException(0);
        }
    }
}
