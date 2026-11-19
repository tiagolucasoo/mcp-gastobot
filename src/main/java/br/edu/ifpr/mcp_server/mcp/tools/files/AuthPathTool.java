package br.edu.ifpr.mcp_server.mcp.tools.files;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.edu.ifpr.mcp_server.mcp.utils.McpConsts;
import br.edu.ifpr.mcp_server.mcp.utils.McpToolDefinition;
import br.edu.ifpr.mcp_server.services.files.AuthPathService;
import br.edu.ifpr.mcp_server.utils.json.schema.SchemaLoader;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;

public class AuthPathTool implements McpToolDefinition {
   private static final Logger LOG =LoggerFactory.getLogger(AuthPathTool.class);

   private static final String JSON_SCHEMA =
                                          "/json-schemas/auth-path.schema.json";

   @Override
   public void register(SyncSpecification<SingleSessionSyncSpecification> mcp)
               throws Exception {
      Map<String, Object> inputSchema = SchemaLoader.readFromFile(JSON_SCHEMA);

      Tool tool = Tool.builder(McpConsts.APT_TOOL_NAME,inputSchema)
                      .title("Diretório Autorizado")
                      .description("Recupera o diretório (caminho) " +
                                   "autorizado para operações com arquivos " +
                                   "no servidor.")
                      .build();

      SyncToolSpecification spec = SyncToolSpecification.builder()
                                                        .tool(tool)
                                                        .callHandler(this::run)
                                                        .build();

      mcp.tools(spec);
   }

   @Override
   public CallToolResult run(McpSyncServerExchange exchange,
                             CallToolRequest request) {
      try {
         String authPath = AuthPathService.authPath();

         return createResult(authPath,false);
      }
      catch (Exception e) {
         return createResult(e.getMessage(),true);
      }
   }

   private CallToolResult createResult(String message, boolean error) {
      String      msg         = message == null || message.trim().isEmpty()
                                ? "Erro desconhecido na recuperação do " +
                                  "diretório autorizado."
                                : message;
      TextContent textContent = TextContent.builder(msg).build();

      return CallToolResult.builder()
                           .content(List.of(textContent))
                           .isError(error)
                           .build();
   }
}
