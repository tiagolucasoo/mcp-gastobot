package br.edu.ifpr.gastobot.config;

import static org.junit.jupiter.api.Assertions.*;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TelegramConfigTest {
    @Test
    void exigeApenasTokenEUsaFusoPadraoSemExporSegredo() {
        var config = TelegramConfig.carregar(Map.of("TELEGRAM_BOT_TOKEN", " 123:test-only "));
        assertEquals("123:test-only", config.token());
        assertEquals(ZoneId.of("America/Sao_Paulo"), config.timeZone());
        assertFalse(config.toString().contains("test-only"));
        assertThrows(IllegalArgumentException.class, () -> TelegramConfig.carregar(Map.of()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "abc", "123:", "abc:123", "123:secret/invalid"})
    void rejeitaTokenComFormatoInvalido(String token) {
        var error = assertThrows(IllegalArgumentException.class,
                () -> TelegramConfig.carregar(Map.of("TELEGRAM_BOT_TOKEN", token)));
        assertTrue(error.getMessage().contains("TELEGRAM_BOT_TOKEN"));
    }

    @Test
    void permiteConfigurarFusoERejeitaFusoInexistente() {
        assertEquals(ZoneId.of("UTC"), TelegramConfig.carregar(Map.of(
                "TELEGRAM_BOT_TOKEN", "123:test-only", "GASTOBOT_TIME_ZONE", "UTC")).timeZone());
        assertThrows(DateTimeException.class, () -> TelegramConfig.carregar(Map.of(
                "TELEGRAM_BOT_TOKEN", "123:test-only", "GASTOBOT_TIME_ZONE", "invalido")));
    }
}
