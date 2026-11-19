package br.edu.ifpr.mcp_server.mcp.utils;

import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

public interface McpToolDefinition {
   /*
    * Registra a ferramena no servidor MCP.
    */
   void register(SyncSpecification<SingleSessionSyncSpecification> mcp)
               throws Exception;
   /*
    * Executa a ferramenta solicitada pelo cliente MCP.
    */
   CallToolResult run(McpSyncServerExchange exchange, CallToolRequest request);
}
