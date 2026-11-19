package br.edu.ifpr.mcp_server.mcp.utils;

import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;

public interface McpResourceDefinition {
   /*
    * Registra o resource no servidor MCP.
    */
   void register(SyncSpecification<SingleSessionSyncSpecification> mcp)
        throws Exception;
   /*
    * Obtém o conteúdo solicitado pelo cliente MCP.
    */
   ReadResourceResult read(McpSyncServerExchange exchange,
                           ReadResourceRequest request);
}
