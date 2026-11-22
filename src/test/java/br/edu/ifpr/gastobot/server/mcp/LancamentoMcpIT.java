package br.edu.ifpr.gastobot.server.mcp;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.edu.ifpr.gastobot.config.DatabaseConfig;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import tools.jackson.databind.json.JsonMapper;

/** Inicia o JAR real como subprocesso e usa o protocolo MCP por STDIO. */
class LancamentoMcpIT {
    @TempDir Path directory;
    private final JsonMapper json = new JsonMapper();

    private McpSyncClient conectar() {
        Path jar = Path.of("server/mcp_server01_server.jar").toAbsolutePath();
        assertTrue(Files.isRegularFile(jar), "Execute com mvn -Pserver verify");
        var params = ServerParameters.builder(Path.of(System.getProperty("java.home"), "bin", "java").toString())
                .args("-D" + DatabaseConfig.PROPERTY + "=" + directory.resolve("banco de teste.db"),
                        "-jar", jar.toString()).build();
        var transport = new StdioClientTransport(params, new JacksonMcpJsonMapper(json));
        transport.setStdErrorHandler(System.err::println);
        var client = McpClient.sync(transport).build();
        try {
            client.initialize();
            return client;
        } catch (RuntimeException exception) {
            client.closeGracefully();
            throw exception;
        }
    }

    private CallToolResult chamar(McpSyncClient client, String name, Map<String, Object> args) {
        return client.callTool(CallToolRequest.builder(name).arguments(args).build());
    }

    private String campo(CallToolResult result, String name) {
        String content = ((TextContent) result.content().getFirst()).text();
        return json.readTree(content).path(name).asText();
    }

    @Test
    void descobreRegistraReiniciaEConsultaComIsolamentoPorUsuario() {
        McpSyncClient client = conectar();
        String id;
        try {
            assertEquals(Set.of("registrar_lancamento", "buscar_lancamento", "listar_lancamentos", "relatorio_financeiro"),
                    client.listTools().tools().stream().map(tool -> tool.name()).collect(Collectors.toSet()));
            var created = chamar(client, "registrar_lancamento", Map.of("usuarioId", "123", "tipo", "SAIDA",
                    "valor", "35.90", "formaPagamento", "PIX", "descricao", "Almoço",
                    "data", "2026-11-21"));
            assertFalse(created.isError());
            assertEquals("35.90", campo(created, "valor"));
            id = campo(created, "id");
        } finally {
            client.closeGracefully();
        }
        client = conectar();
        try {
            var found = chamar(client, "buscar_lancamento", Map.of("id", id, "usuarioId", "123"));
            assertFalse(found.isError());
            assertEquals(id, campo(found, "id"));
            assertEquals("Almoço", campo(found, "descricao"));
            var denied = chamar(client, "buscar_lancamento", Map.of("id", id, "usuarioId", "456"));
            assertTrue(denied.isError());
            assertEquals("NAO_ENCONTRADO", campo(denied, "codigo"));
        } finally {
            client.closeGracefully();
        }
    }

    @Test
    void devolveErroDeValidacaoSemGravar() throws Exception {
        McpSyncClient client = conectar();
        try {
            var invalid = chamar(client, "registrar_lancamento", Map.of("usuarioId", "123", "tipo", "ENTRADA",
                    "valor", "-10.00", "formaPagamento", "DINHEIRO", "data", "2026-11-21"));
            assertTrue(invalid.isError());
            assertEquals("DADOS_INVALIDOS", campo(invalid, "codigo"));
            try (var connection = java.sql.DriverManager.getConnection("jdbc:sqlite:" + directory.resolve("banco de teste.db"));
                    var statement = connection.createStatement();
                    var result = statement.executeQuery("SELECT count(*) FROM lancamentos")) {
                assertTrue(result.next());
                assertEquals(0, result.getInt(1));
            }
        } finally {
            client.closeGracefully();
        }
    }
}
