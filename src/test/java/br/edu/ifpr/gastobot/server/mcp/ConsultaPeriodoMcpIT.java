package br.edu.ifpr.gastobot.server.mcp;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.time.Clock;
import java.util.HashSet;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import br.edu.ifpr.gastobot.client.mcp.StdioMcpConnection;
import br.edu.ifpr.gastobot.client.telegram.*;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class ConsultaPeriodoMcpIT {
    @TempDir Path directory;
    private final JsonMapper json = new JsonMapper();

    private JsonNode dados(CallToolResult result) {
        assertFalse(result.isError(), result.toString());
        return json.readTree(((TextContent) result.content().getFirst()).text());
    }

    @Test
    void listaPeriodoInclusivoComPaginasDataUnicaEConsultaGuiada() {
        try (var mcp = StdioMcpConnection.conectar(Path.of("server/mcp_server01_server.jar"), directory.resolve("consulta.db"))) {
            for (int i = 0; i < 11; i++) {
                dados(mcp.executar("registrar_lancamento", Map.of("usuarioId", "telegram:111", "tipo", "SAIDA",
                        "valor", "1.00", "formaPagamento", "PIX", "data", i == 0 ? "2026-11-01" : "2026-11-22",
                        "descricao", "registro " + i)));
            }
            for (var extra : Map.of("telegram:222", "2026-11-22", "telegram:111", "2026-10-31").entrySet()) {
                dados(mcp.executar("registrar_lancamento", Map.of("usuarioId", extra.getKey(), "tipo", "ENTRADA",
                        "valor", "1.00", "formaPagamento", "PIX", "data", extra.getValue(), "descricao", "fora do filtro")));
            }
            var args = new java.util.HashMap<String, Object>(Map.of("usuarioId", "telegram:111", "dataInicio", "2026-11-01", "dataFim", "2026-11-22"));
            var first = dados(mcp.executar("listar_lancamentos", args));
            assertEquals(10, first.path("lancamentos").size());
            assertTrue(first.path("temProximaPagina").asBoolean());
            assertEquals("2026-11-01", first.path("lancamentos").get(0).path("data").asString());
            var ids = new HashSet<String>();
            for (var item : first.path("lancamentos")) {
                assertEquals("telegram:111", item.path("usuarioId").asString());
                ids.add(item.path("id").asString());
            }
            args.put("pagina", 2);
            var second = dados(mcp.executar("listar_lancamentos", args));
            assertEquals(1, second.path("lancamentos").size());
            assertFalse(second.path("temProximaPagina").asBoolean());
            assertTrue(ids.add(second.path("lancamentos").get(0).path("id").asString()));
            args.put("pagina", 1);
            args.put("dataFim", "2026-11-01");
            assertEquals(1, dados(mcp.executar("listar_lancamentos", args)).path("lancamentos").size());
            args.put("usuarioId", "telegram:333");
            assertEquals(0, dados(mcp.executar("listar_lancamentos", args)).path("lancamentos").size());
            args.put("dataFim", "2026-10-31");
            assertTrue(mcp.executar("listar_lancamentos", args).isError());

            var handler = new TelegramUpdateHandler(new TelegramCommandParser(), new TelegramCommandService(mcp, Clock.systemUTC()));
            assertTrue(handler.responder(new TelegramMessage(111, 111, true, false, "/consultar")).orElseThrow().contains("data"));
            String resposta = handler.responder(new TelegramMessage(111, 111, true, false, "01/11/2026 até 22/11/2026")).orElseThrow();
            assertEquals(10, resposta.lines().filter(line -> line.contains(" | R$ ")).count());
            assertTrue(resposta.contains("pagina=2"));
            resposta = handler.responder(new TelegramMessage(111, 111, true, false, "/consultar 2026-11-01 2026-11-22 pagina=2")).orElseThrow();
            assertEquals(1, resposta.lines().filter(line -> line.contains(" | R$ ")).count());
            assertTrue(handler.responder(new TelegramMessage(333, 333, true, false, "/consultar 22/11/2026"))
                    .orElseThrow().startsWith("Nenhum lançamento"));
        }
    }
}
