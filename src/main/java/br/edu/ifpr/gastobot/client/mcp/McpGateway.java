package br.edu.ifpr.gastobot.client.mcp;

import java.util.Map;

import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

/** Porta de acesso às ferramentas, usada pelos adaptadores de interface do cliente. */
public interface McpGateway {
    CallToolResult executar(String ferramenta, Map<String, Object> argumentos);
}
