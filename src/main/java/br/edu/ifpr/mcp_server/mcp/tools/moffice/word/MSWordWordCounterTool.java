package br.edu.ifpr.mcp_server.mcp.tools.moffice.word;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.edu.ifpr.mcp_server.mcp.utils.McpConsts;
import br.edu.ifpr.mcp_server.mcp.utils.McpToolDefinition;
import br.edu.ifpr.mcp_server.services.files.AuthPathService;
import br.edu.ifpr.mcp_server.services.moffice.word.MSWordCounterService;
import br.edu.ifpr.mcp_server.utils.json.schema.SchemaLoader;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;

public class MSWordWordCounterTool implements McpToolDefinition {
   private static final Logger LOG =
                            LoggerFactory.getLogger(MSWordWordCounterTool.class);

   private static final String JSON_SCHEMA =
                                "/json-schemas/msword-word-counter.schema.json";

   @Override
   public void register(SyncSpecification<SingleSessionSyncSpecification> mcp)
               throws Exception {
      Map<String, Object> inputSchema = SchemaLoader.readFromFile(JSON_SCHEMA);

      Tool tool = Tool.builder(McpConsts.WWC_TOOL_NAME,inputSchema)
                      .title("Contador de Palavras do MS Word")
                      .description("Realiza a contagem de palavras em um " +
                                   "arquivo do MS Word")
                      .build();

      SyncToolSpecification toolSpec = SyncToolSpecification.builder()
                                                            .tool(tool)
                                                         .callHandler(this::run)
                                                            .build();

      mcp.tools(toolSpec);
   }

   @Override
   public CallToolResult run(McpSyncServerExchange exchange,
                             CallToolRequest request) {
      try {
         Map<String,Object> args = request.arguments();
         String             path = (String) args.get(
                                                 McpConsts.WWC_ATTR_INPUT_FILE);
         String             msg  = "";

         if (path == null || path.trim().isEmpty())
            msg = "Arquivo MS Word é inválido.";
         else {
            path = AuthPathService.authPath() + path;
   
            Path filePath = Path.of(path);
            if (!Files.exists(filePath,LinkOption.NOFOLLOW_LINKS))
               msg = "Arquivo '" + path + "' não encontrado";
            else
               if (Files.isDirectory(filePath,LinkOption.NOFOLLOW_LINKS))
                  msg = "Arquivo '" + path + "' não pode ser diretório";
               else
                  if (!Files.isRegularFile(filePath,LinkOption.NOFOLLOW_LINKS))
                     msg = "Arquivo '" + path + "' não é um arquivo regular";
         }
         if (!msg.isEmpty())
            return createResult(msg,true);

         MSWordCounterService service = new MSWordCounterService(path);
         long                 words   = service.countWords();
         StringBuilder        sb      = new StringBuilder();

         sb.append("Arquivo: ").append(path).append(System.lineSeparator())
           .append("Palavras: ").append(words);

         return createResult(sb.toString(),false);
      }
      catch (Exception ex) {
         return createResult(ex.getMessage(),true);
      }
   }

   private CallToolResult createResult(String message, boolean error) {
      String      msg         = message == null || message.trim().isEmpty()
                              ? "Erro desconhecido durante a leitra do MS Word."
                              : message;
      TextContent textContent = TextContent.builder(msg).build();

      return CallToolResult.builder()
                           .content(List.of(textContent))
                           .isError(error)
                           .build();
   }
}
