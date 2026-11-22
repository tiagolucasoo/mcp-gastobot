package br.edu.ifpr.gastobot.client.telegram;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TelegramCommandParserTest {
    private final TelegramCommandParser parser = new TelegramCommandParser();

    @Test
    void interpretaSaidaComDescricaoDataESufixoDoBot() {
        var command = parser.parse(" /saida@GastoBot 35,90 crédito data=2026-11-21 Almoço no centro ");
        assertEquals("registrar", command.nome());
        assertEquals(Map.of("tipo", "SAIDA", "valor", "35,90", "forma", "CARTAO_CREDITO",
                "data", "2026-11-21", "descricao", "Almoço no centro"), command.opcoes());
    }

    @Test
    void interpretaRegistroExplicitoEEntradaSemDescricao() {
        assertEquals(Map.of("tipo", "ENTRADA", "valor", "1500", "forma", "TRANSFERENCIA"),
                parser.parse("/entrada 1500 transferência").opcoes());
        assertEquals(Map.of("tipo", "SAIDA", "valor", "35.90", "forma", "PIX", "descricao", "Almoço"),
                parser.parse("/registrar saída 35.90 pix Almoço").opcoes());
        assertEquals("CARTAO_DEBITO", parser.parse("/saida 10 debito").opcoes().get("forma"));
        assertEquals("DINHEIRO", parser.parse("/entrada 10 dinheiro").opcoes().get("forma"));
        assertEquals("BOLETO", parser.parse("/saida 10 boleto").opcoes().get("forma"));
    }

    @Test
    void consultaNaoPermiteParametroDeOutroUsuario() {
        assertEquals(new TelegramCommand("consultar", Map.of("id", "f5cf9298-848b-46a7-9179-9e97361b5804")), parser.parse("/consultar f5cf9298-848b-46a7-9179-9e97361b5804"));
        assertThrows(IllegalArgumentException.class, () -> parser.parse("/consultar uuid outro-usuario"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/saida", "/entrada 10", "/registrar", "/registrar errado 10 pix", "/saida 10 errado", "/consultar"})
    void formatoInvalidoRetornaErroUtil(String text) {
        assertThrows(IllegalArgumentException.class, () -> parser.parse(text));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/start", "/ajuda", "/help", "", "texto livre", "/desconhecido"})
    void inicioEAjudaNaoCriamLancamento(String text) {
        assertEquals("ajuda", parser.parse(text).nome());
    }

    @Test
    void limitaDescricaoEPreservaImutabilidade() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("/saida 10 pix " + "a".repeat(501)));
        assertThrows(UnsupportedOperationException.class,
                () -> parser.parse("/saida 10 pix").opcoes().put("usuarioId", "outro"));
        assertEquals("ajuda", parser.parse(null).nome());
    }
}
