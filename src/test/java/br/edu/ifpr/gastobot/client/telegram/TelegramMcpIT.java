package br.edu.ifpr.gastobot.client.telegram;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.edu.ifpr.gastobot.client.mcp.StdioMcpConnection;

/** Integra a adaptação Telegram ao servidor MCP real, sem token nem rede Telegram. */
class TelegramMcpIT {
    @TempDir Path directory;

    @Test
    void registraConsultaEIsolaUsuariosPeloMcpReal() {
        try (var mcp = StdioMcpConnection.conectar(Path.of("server/mcp_server01_server.jar"), directory.resolve("telegram.db"))) {
            var service = new TelegramCommandService(mcp,
                    Clock.fixed(Instant.parse("2026-11-22T01:00:00Z"), ZoneId.of("America/Sao_Paulo")));
            var handler = new TelegramUpdateHandler(new TelegramCommandParser(), service);
            String created = handler.responder(new TelegramMessage(111, 111, true, false,
                    "/saida 35,90 pix Almoço")).orElseThrow();
            assertTrue(created.startsWith("Lançamento registrado."), created);
            assertTrue(created.contains("R$ 35,90"));
            assertTrue(created.contains("2026-11-21"));
            assertFalse(created.contains("ID:"));
            var listed = mcp.executar("listar_lancamentos", Map.of("usuarioId", "telegram:111",
                    "dataInicio", "2026-11-21", "dataFim", "2026-11-21"));
            String content = ((io.modelcontextprotocol.spec.McpSchema.TextContent) listed.content().getFirst()).text();
            String id = new tools.jackson.databind.json.JsonMapper().readTree(content)
                    .path("lancamentos").get(0).path("id").asString();
            String found = handler.responder(new TelegramMessage(111, 111, true, false,
                    "/consultar " + id)).orElseThrow();
            assertTrue(found.startsWith("Lançamento encontrado."), found);
            assertTrue(found.contains("Almoço"));
            assertEquals("Lançamento não encontrado para o seu usuário.",
                    handler.responder(new TelegramMessage(222, 222, true, false, "/consultar " + id)).orElseThrow());
            assertTrue(mcp.executar("buscar_lancamento", Map.of("id", id, "usuarioId", "111")).isError());
            assertTrue(handler.responder(new TelegramMessage(111, 111, true, false,
                    "/entrada 0,00 dinheiro")).orElseThrow().startsWith("Dados inválidos"));
        }
    }

    @Test
    void cadastroGuiadoGravaUmaUnicaVezApenasDepoisDaDescricao() throws Exception {
        Path banco = directory.resolve("conversa.db");
        try (var mcp = StdioMcpConnection.conectar(Path.of("server/mcp_server01_server.jar"), banco)) {
            var handler = new TelegramUpdateHandler(new TelegramCommandParser(),
                    new TelegramCommandService(mcp, Clock.fixed(Instant.parse("2026-11-22T01:00:00Z"),
                            ZoneId.of("America/Sao_Paulo"))));
            for (String texto : new String[]{"/saida", "35,90", "pix"}) {
                handler.responder(new TelegramMessage(111, 111, true, false, texto));
            }
            try (var connection = java.sql.DriverManager.getConnection("jdbc:sqlite:" + banco);
                    var statement = connection.createStatement();
                    var result = statement.executeQuery("SELECT count(*) FROM lancamentos")) {
                assertTrue(result.next());
                assertEquals(0, result.getInt(1));
            }
            String resposta = handler.responder(new TelegramMessage(111, 111, true, false, "Almoço")).orElseThrow();
            assertTrue(resposta.startsWith("Lançamento registrado."), resposta);
            assertTrue(resposta.contains("R$ 35,90"));
            handler.responder(new TelegramMessage(111, 111, true, false, "ok"));
            try (var connection = java.sql.DriverManager.getConnection("jdbc:sqlite:" + banco);
                    var statement = connection.createStatement();
                    var result = statement.executeQuery("SELECT count(*) FROM lancamentos")) {
                assertTrue(result.next());
                assertEquals(1, result.getInt(1));
            }
        }
    }
}
