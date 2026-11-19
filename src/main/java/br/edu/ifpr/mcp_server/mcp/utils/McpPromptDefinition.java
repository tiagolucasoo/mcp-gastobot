package br.edu.ifpr.mcp_server.mcp.utils;

import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;

public interface McpPromptDefinition {
   void register(SyncSpecification<SingleSessionSyncSpecification> mcp)
        throws Exception;

   GetPromptResult run(McpSyncServerExchange exchange,
                       GetPromptRequest request);
}
