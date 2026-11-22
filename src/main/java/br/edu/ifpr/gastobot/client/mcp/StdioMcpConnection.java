package br.edu.ifpr.gastobot.client.mcp;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import br.edu.ifpr.gastobot.config.DatabaseConfig;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import tools.jackson.databind.json.JsonMapper;

/** Conexão MCP compartilhada pelo terminal e pelo bot, sem acesso direto ao banco. */
public final class StdioMcpConnection implements McpGateway, AutoCloseable {
    private final McpSyncClient client;

    private StdioMcpConnection(McpSyncClient client) {
        this.client = client;
    }

    public static StdioMcpConnection conectar() {
        return conectar(Path.of(System.getProperty("gastobot.server.jar", "server/mcp_server01_server.jar")),
                DatabaseConfig.caminho());
    }

    public static StdioMcpConnection conectar(Path servidorJar, Path banco) {
        Path jar = servidorJar.toAbsolutePath().normalize();
        if (!Files.isRegularFile(jar)) {
            throw new IllegalStateException("Compile o servidor primeiro. JAR não encontrado: " + jar);
        }
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        var parameters = ServerParameters.builder(java)
                .args("-D" + DatabaseConfig.PROPERTY + "=" + banco.toAbsolutePath().normalize(),
                        "-jar", jar.toString()).build();
        var transport = new StdioClientTransport(parameters, new JacksonMcpJsonMapper(new JsonMapper()));
        transport.setStdErrorHandler(System.err::println);
        var client = McpClient.sync(transport).build();
        try {
            client.initialize();
            return new StdioMcpConnection(client);
        } catch (RuntimeException exception) {
            client.closeGracefully();
            throw exception;
        }
    }

    public List<Tool> ferramentas() {
        return client.listTools().tools();
    }

    @Override
    public CallToolResult executar(String ferramenta, Map<String, Object> argumentos) {
        return client.callTool(CallToolRequest.builder(ferramenta).arguments(argumentos).build());
    }

    @Override
    public void close() {
        client.closeGracefully();
    }
}
