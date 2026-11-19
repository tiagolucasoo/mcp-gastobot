package br.edu.ifpr.mcp_server.mcp.resources;

import java.util.List;

import br.edu.ifpr.mcp_server.mcp.utils.McpConsts;
import br.edu.ifpr.mcp_server.mcp.utils.McpResourceDefinition;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.Resource;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;

public class AboutResource implements McpResourceDefinition {
   @Override
   // Registra o resource app://about no servidor MCP
   public void register(SyncSpecification<SingleSessionSyncSpecification> mcp)
               throws Exception {
      Resource resource = Resource.builder(McpConsts.RESOURCE_ABOUT_URI,
                                           McpConsts.RESOURCE_ABOUT_NAME)
                                  .title("Sobre o servidor MCP")
                                  .description(
                                     "Informações gerais sobre o servidor MCP.")
                                  .mimeType("text/plain")
                                  .build();

      SyncResourceSpecification spec = new SyncResourceSpecification(resource,
                                                                    this::read);

      mcp.resources(spec);
   }

   @Override
   // Retorna informações gerais do servidor MCP
   public ReadResourceResult read(McpSyncServerExchange exchange,
                                  ReadResourceRequest request) {
      String content = """
                       Nome: '%s'\n
                       Versão: '%s'\n
                       Descrição: '%s'.
                       """.formatted(McpConsts.APP_NAME,McpConsts.APP_VERSION,
                                     McpConsts.APP_DESCRIPTION);

      TextResourceContents text =
	    	            TextResourceContents.builder(McpConsts.RESOURCE_ABOUT_URI,
                                                     content)
                                            .mimeType("text/plain")
                                            .build();

      return ReadResourceResult.builder(List.of(text)).build();
   }
}
