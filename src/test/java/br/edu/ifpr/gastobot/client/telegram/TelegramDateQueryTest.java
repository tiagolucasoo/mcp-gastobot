package br.edu.ifpr.gastobot.client.telegram;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TelegramDateQueryTest {
    @ParameterizedTest
    @ValueSource(strings = {"22/11/2026", "2026-11-22"})
    void dataUnicaEquivaleAoMesmoDia(String texto) {
        assertEquals(Map.of("dataInicio", "2026-11-22", "dataFim", "2026-11-22", "pagina", "1"),
                TelegramDateQuery.interpretar(texto).opcoes());
    }

    @ParameterizedTest
    @ValueSource(strings = {"01/11/2026 22/11/2026 pagina=2", "2026-11-01 a 2026-11-22 pagina=2",
            "01/11/2026 até 22/11/2026 pagina=2"})
    void normalizaPeriodoEPagina(String texto) {
        assertEquals(Map.of("dataInicio", "2026-11-01", "dataFim", "2026-11-22", "pagina", "2"),
                TelegramDateQuery.interpretar(texto).opcoes());
    }

    @ParameterizedTest
    @ValueSource(strings = {"31/02/2026", "2026-02-29", "22/11/2026 01/11/2026", "",
            "01/11/2026 pagina=0", "01/11/2026 pagina=abc", "01/11/2026 pagina=99999999999", "01/11/2026 extra extra"})
    void rejeitaDatasPeriodosEPaginasInvalidos(String texto) {
        assertThrows(IllegalArgumentException.class, () -> TelegramDateQuery.interpretar(texto));
    }

    @Test
    void comandoCompletoUsaPeriodoOuDataUnica() {
        var parser = new TelegramCommandParser();
        assertEquals("2026-11-22", parser.parse("/consultar 22/11/2026").opcoes().get("dataFim"));
        assertEquals("2026-11-01", parser.parse("/consultar 01/11/2026 22/11/2026").opcoes().get("dataInicio"));
    }
}
