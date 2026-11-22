package br.edu.ifpr.gastobot.client.telegram;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TelegramApiClientTest {
    private final HttpClient http = mock(HttpClient.class);
    @SuppressWarnings("unchecked")
    private final HttpResponse<String> response = mock(HttpResponse.class);
    private final TelegramApiClient api = new TelegramApiClient(http, "123:test-only");

    @SuppressWarnings("unchecked")
    private void resposta(int code, String body) throws Exception {
        when(response.statusCode()).thenReturn(code);
        when(response.body()).thenReturn(body);
        doReturn(response).when(http).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void interpretaMensagemEAtualizacaoSemTexto() throws Exception {
        resposta(200, """
                {"ok":true,"result":[
                  {"update_id":7,"message":{"chat":{"id":123,"type":"private"},
                    "from":{"id":123,"is_bot":false},"text":"/start"}},
                  {"update_id":8,"message":{"photo":[]}},
                  {"update_id":9,"edited_message":{"text":"/saida 10 pix"}},
                  {"message":{}}
                ]}
                """);
        var updates = api.receber(0);
        assertEquals(3, updates.size());
        assertEquals(new TelegramMessage(123, 123, true, false, "/start"), updates.getFirst().mensagem());
        assertNull(updates.get(1).mensagem());
        assertNull(updates.get(2).mensagem());
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 409, 429, 500})
    void traduzStatusHttpSemExporTokenOuCorpo(int code) throws Exception {
        resposta(code, "123:test-only");
        var exception = assertThrows(TelegramApiException.class, () -> api.receber(0));
        assertEquals(code, exception.codigo());
        assertFalse(exception.toString().contains("test-only"));
        assertNull(exception.getCause());
    }

    @Test
    void rejeitaEnvelopeDeErroOuJsonInvalido() throws Exception {
        resposta(200, "{\"ok\":false,\"error_code\":429}");
        assertEquals(429, assertThrows(TelegramApiException.class, () -> api.receber(0)).codigo());
        resposta(200, "not json");
        assertThrows(TelegramApiException.class, () -> api.receber(0));
        resposta(200, "{\"ok\":true,\"result\":{}}");
        assertThrows(TelegramApiException.class, () -> api.receber(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sanitizaFalhaDeRedeEPreservaInterrupcao() throws Exception {
        doThrow(new IOException("https://api.telegram.org/bot123:test-only/getUpdates"))
                .when(http).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        var exception = assertThrows(TelegramApiException.class, () -> api.receber(0));
        assertFalse(exception.toString().contains("test-only"));
        assertNull(exception.getCause());
        doThrow(new InterruptedException()).when(http).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        assertThrows(InterruptedException.class, () -> api.receber(0));
    }
}
