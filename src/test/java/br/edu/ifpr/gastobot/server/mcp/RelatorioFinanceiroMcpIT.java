package br.edu.ifpr.gastobot.server.mcp;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import br.edu.ifpr.gastobot.client.mcp.StdioMcpConnection;
import br.edu.ifpr.gastobot.client.telegram.TelegramCommandParser;
import br.edu.ifpr.gastobot.client.telegram.TelegramCommandService;
import br.edu.ifpr.gastobot.client.telegram.TelegramMessage;
import br.edu.ifpr.gastobot.client.telegram.TelegramUpdateHandler;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class RelatorioFinanceiroMcpIT {
    @TempDir Path directory;
    private final JsonMapper json = new JsonMapper();

    private JsonNode dados(CallToolResult result) {
        assertFalse(result.isError(), result.toString());
        return json.readTree(((TextContent) result.content().getFirst()).text());
    }

    private void gravar(StdioMcpConnection mcp, String usuario, String data, String tipo, String valor) {
        dados(mcp.executar("registrar_lancamento", Map.of("usuarioId", usuario, "tipo", tipo,
                "valor", valor, "formaPagamento", "PIX", "data", data, "descricao", "descrição não deve aparecer no relatório")));
    }

    @Test
    void totaisDoDiaSemanaEMesIncluemTodosOsRegistrosESomenteOUsuario() {
        try (var mcp = StdioMcpConnection.conectar(Path.of("server/mcp_server01_server.jar"), directory.resolve("relatorio.db"))) {
            gravar(mcp, "telegram:111", "2026-11-22", "ENTRADA", "100.10");
            gravar(mcp, "telegram:111", "2026-11-22", "SAIDA", "20.05");
            gravar(mcp, "telegram:111", "2026-11-16", "ENTRADA", "50.00");
            gravar(mcp, "telegram:111", "2026-11-01", "SAIDA", "5.00");
            gravar(mcp, "telegram:111", "2026-11-30", "ENTRADA", "2.00");
            gravar(mcp, "telegram:222", "2026-11-22", "ENTRADA", "999.00");
            gravar(mcp, "telegram:111", "2026-10-31", "ENTRADA", "999.00");
            for (int i = 0; i < 12; i++) {
                gravar(mcp, "telegram:111", "2026-11-05", "ENTRADA", "1.00");
            }
            var result = dados(mcp.executar("relatorio_financeiro", Map.of("usuarioId", "telegram:111", "dataReferencia", "2026-11-22")));
            assertEquals(3, result.path("periodos").size());
            var dia = result.path("periodos").get(0);
            var semana = result.path("periodos").get(1);
            var mes = result.path("periodos").get(2);
            assertEquals("100.10", dia.path("totalEntradas").asString());
            assertEquals("20.05", dia.path("totalSaidas").asString());
            assertEquals(2, dia.path("quantidade").asLong());
            assertEquals("150.10", semana.path("totalEntradas").asString());
            assertEquals("20.05", semana.path("totalSaidas").asString());
            assertEquals(3, semana.path("quantidade").asLong());
            assertEquals("164.10", mes.path("totalEntradas").asString());
            assertEquals("25.05", mes.path("totalSaidas").asString());
            assertEquals(15, mes.path("quantidadeEntradas").asLong());
            assertEquals(2, mes.path("quantidadeSaidas").asLong());
            assertEquals(17, mes.path("quantidade").asLong());
            assertFalse(mes.has("lancamentos"));
            assertFalse(mes.has("id"));

            var handler = new TelegramUpdateHandler(new TelegramCommandParser(), new TelegramCommandService(mcp,
                    Clock.fixed(Instant.parse("2026-11-22T15:00:00Z"), ZoneId.of("America/Sao_Paulo"))));
            String resposta = handler.responder(new TelegramMessage(111, 111, true, false, "/relatorio")).orElseThrow();
            assertTrue(resposta.contains("Entradas: R$ 164,10 | Quantidade: 15"));
            assertTrue(resposta.contains("Saídas: R$ 25,05 | Quantidade: 2"));
            assertTrue(resposta.contains("Quantidade total: 17"));
            assertFalse(resposta.contains("ID:"));
            assertFalse(resposta.contains("descrição não deve aparecer"));
            assertFalse(resposta.contains("Saldo"));

            var vazio = dados(mcp.executar("relatorio_financeiro", Map.of("usuarioId", "telegram:333", "dataReferencia", "2026-11-22")));
            for (var periodo : vazio.path("periodos")) {
                assertEquals("0.00", periodo.path("totalEntradas").asString());
                assertEquals("0.00", periodo.path("totalSaidas").asString());
                assertEquals(0, periodo.path("quantidade").asLong());
            }
            assertTrue(mcp.executar("relatorio_financeiro", Map.of("usuarioId", "telegram:111", "dataReferencia", "2026-02-30")).isError());
            assertTrue(mcp.executar("relatorio_financeiro", Map.of("dataReferencia", "2026-11-22")).isError());
        }
    }
}
