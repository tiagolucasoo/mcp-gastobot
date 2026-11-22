package br.edu.ifpr.gastobot.server;

import br.edu.ifpr.gastobot.config.DatabaseConfig;
import br.edu.ifpr.gastobot.controller.LancamentoController;
import br.edu.ifpr.gastobot.repository.sqlite.SqliteConnectionFactory;
import br.edu.ifpr.gastobot.repository.sqlite.SqliteLancamentoRepository;
import br.edu.ifpr.gastobot.server.mcp.LancamentoTools;
import br.edu.ifpr.gastobot.service.LancamentoService;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import tools.jackson.databind.json.JsonMapper;

public final class GastoBotServer {
    private GastoBotServer() {
    }

    public static McpSyncServer iniciar() {
        var repository = new SqliteLancamentoRepository(new SqliteConnectionFactory(DatabaseConfig.caminho()));
        var controller = new LancamentoController(new LancamentoService(repository));
        var transport = new StdioServerTransportProvider(new JacksonMcpJsonMapper(new JsonMapper()));
        var specification = McpServer.sync(transport)
                .serverInfo("GastoBot", "0.0.1")
                .capabilities(ServerCapabilities.builder().tools(false).build());
        for (var tool : new LancamentoTools(controller).specifications()) {
            specification.tools(tool);
        }
        return specification.build();
    }

    public static void main(String[] args) {
        McpSyncServer server = iniciar();
        Runtime.getRuntime().addShutdownHook(new Thread(server::closeGracefully));
    }
}
