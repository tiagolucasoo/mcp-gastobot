
package br.edu.ifpr.mcp_server.mcp.tools.pdfs;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.edu.ifpr.mcp_server.mcp.utils.McpConsts;
import br.edu.ifpr.mcp_server.mcp.utils.McpToolDefinition;
import br.edu.ifpr.mcp_server.services.files.AuthPathService;
import br.edu.ifpr.mcp_server.services.pdfs.PdfPageReaderService;
import br.edu.ifpr.mcp_server.services.pdfs.model.SearchOptions;
import br.edu.ifpr.mcp_server.services.pdfs.model.SearchResult;
import br.edu.ifpr.mcp_server.services.pdfs.processors.PdfSearchProcessor;
import br.edu.ifpr.mcp_server.utils.json.schema.SchemaLoader;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Ferramenta MCP para pesquisar textos em arquivos PDF,
 * contabilizando as ocorrências por página.
 */
public class PdfSearchTool implements McpToolDefinition {
   private static final Logger LOG =
                                   LoggerFactory.getLogger(PdfSearchTool.class);

   private static final String JSON_SCHEMA =
                                         "/json-schemas/pdf-search.schema.json";

   private final PdfPageReaderService service;

   public PdfSearchTool() {
      this.service = new PdfPageReaderService();
   }

   @Override
   public void register(SyncSpecification<SingleSessionSyncSpecification> mcp)
               throws Exception {
/*
      Map<String,Object> inputSchema = Map.of(
            "type","object",
            "properties",Map.of(
                  "pdfPath",Map.of(
                        "type","string",
                        "description","Caminho do arquivo PDF."),
                  "targetText",Map.of(
                        "type","string",
                        "description","Texto que será pesquisado."),
                  "useRegex",Map.of(
                        "type","boolean",
                        "description","Utilizar expressão regular."),
                  "caseSensitive",Map.of(
                        "type","boolean",
                        "description","Diferenciar maiúsculas e minúsculas.")
            ),
            "required",List.of("pdfPath","targetText")
      );
*/
      // define o esquema para troca de dados - JSON-RPC
      Map<String, Object> inputSchema = SchemaLoader.readFromFile(JSON_SCHEMA);

      Tool tool = Tool.builder(McpConsts.PDFST_TOOL_NAME,inputSchema)
                      .title("Buscar texto em arquivos PDF")
                      .description("Pesquisa um texto em um arquivo PDF e " +
                                   "informa a quantidade de ocorrências por " +
                                   "página.")
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
         Map<String,Object> args = request.arguments();

         String  input_file     = (String) args.get(
                                               McpConsts.PDFST_ATTR_INPUT_FILE);
         String  search         = (String) args.get(
                                                   McpConsts.PDFST_ATTR_SEARCH);
         boolean use_regex      = Boolean.TRUE.equals(args.get(
                                               McpConsts.PDFST_ATTR_USE_REGEX));
         boolean case_sensitive = !Boolean.FALSE.equals(
                                 args.get(McpConsts.PDFST_ATTR_CASE_SENSITIVE));

         String  msg = "";

         if (input_file == null || input_file.isBlank())
            msg = "Arquivo PDF é inválido.";
         else {
            input_file = AuthPathService.authPath() + input_file;
   
            Path filePath = Path.of(input_file);
            // de acordo com o esquema JSON para esta ferramenta, este 'if', em
            // tese, é desnecessário
            if (!Files.exists(filePath,LinkOption.NOFOLLOW_LINKS))
               msg = "Arquivo '" + input_file + "' não encontrado";
            else
               if (Files.isDirectory(filePath,LinkOption.NOFOLLOW_LINKS))
                  msg = "Arquivo '" + input_file + "' não pode ser diretório";
               else
                  if (!Files.isRegularFile(filePath,LinkOption.NOFOLLOW_LINKS))
                     msg = "Arquivo '" + input_file + "' não é um arquivo regular";
         }
         // se algum dos 'ifs' acima for verdadeiro, a ferramenta não pode
         // continuar sua execução e retornará com erro ao cliente
         if (!msg.isEmpty())
            return createErrorResult(msg);

         if (search == null || search.isEmpty())
           return createErrorResult("O texto de pesquisa não foi informado.");

         File               pdfFile   = new File(input_file);
         SearchOptions      options   = new SearchOptions(search,use_regex,
                                                          case_sensitive);
         PdfSearchProcessor processor = new PdfSearchProcessor(options);

         service.processPages(pdfFile,processor);

         SearchResult result = processor.getSearchResult();

         return createSuccessResult(pdfFile,search,result);

      }
      catch (Exception e) {
         return createErrorResult(e.getMessage());
      }
   }

   private CallToolResult createSuccessResult(File pdfFile, String targetText,
                                              SearchResult result) {
      StringBuilder response = new StringBuilder();

      response.append("Arquivo: ").append(pdfFile.getName()).append("\n")
              .append("Texto pesquisado: ").append(targetText).append("\n")
              .append("Total de ocorrências: ")
              .append(result.totalOccurrences()).append("\n\n")
              .append("Ocorrências por página:");

      Map<Integer,Integer> occurrences =
                                     new TreeMap<>(result.occurrencesPerPage());

      if (occurrences.isEmpty())
         response.append("\nNenhuma ocorrência encontrada.");
      else
         occurrences.forEach((page,count) ->
            response.append("\nPágina ")
                    .append(String.format("[%5d]: ",page))
                    .append(String.format("[%5d]",count)).append("\n"));

      TextContent textContent = TextContent.builder(response.toString())
                                           .build();

      return CallToolResult.builder()
                           .content(List.of(textContent))
                           .build();
   }

   private CallToolResult createErrorResult(String message) {
      String      errorMessage = message == null
                                ? "Erro desconhecido durante a pesquisa no PDF."
                                : message;
      TextContent textContent  = TextContent.builder(errorMessage).build();

      return CallToolResult.builder()
                           .content(List.of(textContent))
                           .isError(true)
                           .build();
   }
}
