package br.edu.ifpr.mcp_server.mcp.tools.files;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.edu.ifpr.mcp_server.mcp.utils.McpConsts;
import br.edu.ifpr.mcp_server.mcp.utils.McpToolDefinition;
import br.edu.ifpr.mcp_server.services.files.FileSearchService;
import br.edu.ifpr.mcp_server.utils.json.schema.SchemaLoader;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;

public class FileSearchTool implements McpToolDefinition {
   private static final Logger LOG =
                                  LoggerFactory.getLogger(FileSearchTool.class);

   private static final String JSON_SCHEMA =
			                               "/json-schemas/file-search.schema.json";
   // número de resultados do serviço de busca
   private static final int DEFAULT_MAX_RESULTS = 20;
   // instância do serviço de busca
   private final FileSearchService service;

   public FileSearchTool(FileSearchService service) {
      this.service = service;
   }

   @Override
   public void register(SyncSpecification<SingleSessionSyncSpecification> mcp)
               throws Exception {
      // define o esquema para troca de dados - JSON-RPC
      Map<String, Object> inputSchema = SchemaLoader.readFromFile(JSON_SCHEMA);

      Tool tool = Tool.builder(McpConsts.FST_TOOL_NAME,inputSchema)
                      .title("Busca de Arquivos")
                      .description("Pesquisa arquivos pelo nome dentro do " +
                                   "diretório autorizado no servidor. Aceita " +
                                   "padrões glob, como *.pdf ou contrato*.")
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
//      LOG.info("LANG: {}",System.getenv("LANG"));
//      LOG.info("LC_ALL: {}",System.getenv("LC_ALL"));
//      LOG.info("file.encoding: {}",
//               System.getProperty("file.encoding"));
//      LOG.info("native.encoding: {}",
//               System.getProperty("native.encoding"));
//      LOG.info("native.encoding: {}",System.getProperty("native.encoding"));
//      LOG.info("sun.jnu.encoding: {}",System.getProperty("sun.jnu.encoding"));
//      LOG.info("Charset.defaultCharset: {}",Charset.defaultCharset());

      // recupea todos os argumentos recebidos
      Map<String, Object> args = request.arguments();
      // recupera separamente os argumentos recebidos
      String  pattern     = (String) args.get(McpConsts.FST_ATTR_PATTERN);
      boolean recursive   = Boolean.TRUE.equals(args.get(
                                                 McpConsts.FST_ATTR_RECURSIVE));
      Object  maxResults  = args.get(McpConsts.FST_ATTR_MAX_RES);
      int     max_results = maxResults instanceof Number number
                                                          ? number.intValue()
                                                          : DEFAULT_MAX_RESULTS;

      if (max_results <= 0) max_results = 1;
      try {
         LOG.info("Iniciando busca de arquivos: pattern=[{}], recursive=[{}], "+
                  "max_results=[{}]",pattern,recursive,max_results);
         // executa o serviço de busca de arquivos
         String report = service.search(pattern,recursive,max_results);
         // não houve quaisquer erros na busca
         return createResult(report,false);
      }
      catch (Exception ex) {
         LOG.warn("Falha na ferramenta '" + McpConsts.FST_TOOL_NAME + "'",ex);
         // houve erro na busca
         return createResult("Não foi possível concluir a busca: " +
                             ex.getMessage(),true);
      }
   }

   private CallToolResult createResult(String message, boolean isError) {
      // prepara o retorno do servicor MCP ao cliente que o executou
      String      msg         = message == null || message.trim().isEmpty()
                                ? "Erro desconhecido na busca por arquivoś."
                                : message;
      TextContent textContent = TextContent.builder(msg).build();

      return CallToolResult.builder()
                           .content(List.of(textContent))
                           .isError(isError)
                           .build();
   }
}
