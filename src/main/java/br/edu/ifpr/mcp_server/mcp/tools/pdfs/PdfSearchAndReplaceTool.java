package br.edu.ifpr.mcp_server.mcp.tools.pdfs;

import java.io.IOException;
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
import br.edu.ifpr.mcp_server.services.pdfs.PdfProcessorFacade;
import br.edu.ifpr.mcp_server.services.pdfs.model.ReplacementResult;
import br.edu.ifpr.mcp_server.services.pdfs.model.SearchOptions;
import br.edu.ifpr.mcp_server.utils.json.schema.SchemaLoader;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;

public class PdfSearchAndReplaceTool implements McpToolDefinition {
   private static final Logger LOG =
                         LoggerFactory.getLogger(PdfSearchAndReplaceTool.class);

   private static final String JSON_SCHEMA =
                                 "/json-schemas/pdf-search-replace.schema.json";

   private PdfProcessorFacade processorFacade = new PdfProcessorFacade();

//   private String configuredRoot;
//
//   public PdfSearchAndReplaceTool() {
//      String propertyRoot    = System.getProperty("mcp.files.root");
//      String environmentRoot = System.getenv("MCP_FILES_ROOT");
//
//      configuredRoot = propertyRoot;
//      if (configuredRoot == null || configuredRoot.trim().isEmpty())
//         configuredRoot = environmentRoot;
//
//      if (configuredRoot == null || configuredRoot.trim().isEmpty())
//         throw new IllegalStateException("Configure a variável de ambiente " +
//                                         "'MCP_FILES_ROOT' ou a propriedade " +
//                                         "'mcp.files.root'.");
//
//      if (configuredRoot.charAt(configuredRoot.length() - 1) !=
//    			                                             File.separatorChar)
//         configuredRoot = configuredRoot + File.separator;
//   }
   public PdfSearchAndReplaceTool() {
      this(new PdfProcessorFacade());
   }

   public PdfSearchAndReplaceTool(PdfProcessorFacade processorFacade) {
      this.processorFacade = processorFacade;
   }

   @Override
   public void register(SyncSpecification<SingleSessionSyncSpecification> mcp)
               throws Exception {
      // define o esquema para troca de dados - JSON-RPC
      Map<String, Object> inputSchema = SchemaLoader.readFromFile(JSON_SCHEMA);

      Tool tool = Tool.builder(McpConsts.PDFSRT_TOOL_NAME,inputSchema)
                      .title("Buscar & Subsituir em PDF")
                      .description("Realiza busca e substituição de textos " +
                                   "em arquivos PDF.")
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
      // recupea todos os argumentos recebidos
      Map<String,Object> args = request.arguments();

      String  fileInput     = (String) args.get(
                                              McpConsts.PDFSRT_ATTR_INPUT_FILE);
      String  fileOutput    = (String) args.get(
                                             McpConsts.PDFSRT_ATTR_OUTPUT_FILE);
      String  search        = (String) args.get(
                                                  McpConsts.PDFSRT_ATTR_SEARCH);
      String  replace       = (String) args.get(
                                                 McpConsts.PDFSRT_ATTR_REPLACE);
      boolean useRegex      = Boolean.TRUE.equals(args.get(
                                              McpConsts.PDFSRT_ATTR_USE_REGEX));
      boolean caseSensitive = Boolean.TRUE.equals(args.get(
                                         McpConsts.PDFSRT_ATTR_CASE_SENSITIVE));

      String msg   = "";

      Path root    = Path.of(AuthPathService.authPath())
                         .toAbsolutePath()
                         .normalize();
      Path filePath = root.resolve(fileInput).normalize();

      if (!filePath.startsWith(root))
         msg = "Arquivo '" + filePath + "' fora da raiz autorizada";
      else
         if (!Files.exists(filePath,LinkOption.NOFOLLOW_LINKS))
             msg = "Arquivo '" + fileInput + "' não encontrado";
         else
            if (Files.isDirectory(filePath,LinkOption.NOFOLLOW_LINKS))
                msg = "Arquivo '" + fileInput + "' não pode ser diretório";
            else
               if (!Files.isRegularFile(filePath,LinkOption.NOFOLLOW_LINKS))
                   msg = "Arquivo '" + fileInput + "' não é um arquivo regular";

      if (!msg.isEmpty())
         return createResult(msg,true);

//      if (fileOutput == null || fileOutput.trim().isEmpty()) {
//         filePath = filePath.normalize();
//
//         Path   parent = filePath.getParent();
//         String name   = filePath.getFileName().toString();
//         int    dot    = name.lastIndexOf(".");
//
//         if (dot >= 0) {
//            String ext = name.substring(dot + 1);
//
//            name = name.substring(0,dot);
//            fileOutput = String.format("%s%s%s_saida.%s",parent,File.separator,
//                                                         name,ext);
//         }
//         else fileOutput = String.format("%s%s%s_saida",parent,File.separator,
//                                                        name);
//      }
      Path output = fileOutput == null || fileOutput.isBlank()
                    ? generateDefaultOutputPath(filePath)
                    : root.resolve(fileOutput).normalize();
      if (!output.startsWith(root)) {
         msg = "Arquivo '" + output + "' fora da raiz autorizada";

         return createResult(msg,true);
      }
                                                // texto a busca, pode usar
                                                // expressões regulares (RegEx)
      SearchOptions options = new SearchOptions(search,
                                                // utilizar regex?
                                                useRegex,
                                                // maiúsculas == minúsculas?
                                                caseSensitive
                                               );
      LOG.debug(" fileInput [{}]",fileInput);
      LOG.debug("fileOutput [{}]",fileOutput);
      try {
         ReplacementResult result =
            processorFacade.processAndReplace(filePath.toFile(),
                                              output.toFile(),
                                              options,
                                              replace);
         StringBuilder     sb     = new StringBuilder();

         sb.append("=== Relatório de Substituição ===").append("\n")
           .append("Total de ocorrências encontradas e substituídas: ")
           .append(result.totalOccurrences()).append("\n");

         // o resultado das ocorrências por página é armazenado em um mapa e
         // não é possível saber a ordem na qual seus elementos estão dispostos,
         // por isso, o código, simples, a seguir, é substituído por um código
         // capaz de garantir que o objeto StringBuilder conterá as páginas
         // ordenadas pelo número de página em que o texto de busca/troca foi
         // encontrado
//         for (Map.Entry<Integer, Integer> pair :
//              result.occurrencesPerPage().entrySet())
//            sb.append("Página ").append(String.format("[%5d]: ",pair.getKey()))
//              .append(String.format("[%5d]",pair.getValue())).append("\n");
         result.occurrencesPerPage()
               .entrySet()
               .stream()
               .sorted(Map.Entry.comparingByKey())
               .forEach(pair ->
                   sb.append("Página ")
                     .append(String.format("[%5d]: ",pair.getKey()))
                     .append(String.format("[%5d]",pair.getValue()))
                     .append("\n")
               );

         return createResult(sb.toString(),false);
      }
      catch (IOException ex) {
         msg = "Erro durante o processamento do arquivo PDF: " + 
               ex.getMessage();

         return createResult(msg,true);
      }
   }

   private CallToolResult createResult(String message, boolean isError) {
      // prepara o retorno do servicor MCP ao cliente que o executou
      TextContent textContent = TextContent.builder(message).build();

      return CallToolResult.builder()
                           .content(List.of(textContent))
                           .isError(isError)
                           .build();
   }

   private Path generateDefaultOutputPath(Path input) {
      String fileName       = input.getFileName().toString();
      int    extensionIndex = fileName.lastIndexOf('.');

      String outputName = extensionIndex > 0
                          ? fileName.substring(0,extensionIndex) + "_saida" +
                            fileName.substring(extensionIndex)
                          : fileName + "_saida";

      return input.resolveSibling(outputName);
   }
}
