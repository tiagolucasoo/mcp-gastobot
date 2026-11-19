package br.edu.ifpr.mcp_server.mcp.tools.pdfs;

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
import br.edu.ifpr.mcp_server.services.pdfs.PdfMetadataService;
import br.edu.ifpr.mcp_server.utils.json.schema.SchemaLoader;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;

public class PdfMetadataTool implements McpToolDefinition {
   private static final Logger LOG =
                                LoggerFactory.getLogger(PdfMetadataTool.class);

   private static final String JSON_SCHEMA =
                                      "/json-schemas/pdf-metadata.schema.json";

//   private String configuredRoot;

   public PdfMetadataTool() {
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
   }

   @Override
   public void register(SyncSpecification<SingleSessionSyncSpecification> mcp)
               throws Exception {
      Map<String, Object> inputSchema = SchemaLoader.readFromFile(JSON_SCHEMA);

      Tool tool = Tool.builder(McpConsts.PDFMT_TOOL_NAME,inputSchema)
                      .title("Recuperar metadados PDF")
                      .description("Recuperar metadados de um arquivo PDF.")
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
         Map<String,Object> arguments = request.arguments();

         String  pdfPath  = (String) arguments.get(
                                              McpConsts.PDFMT_ATTR_INPUT_FILE);
         String  msg      = "";
         // de acordo com o esquema JSON para esta ferramenta, este 'if', em
         // tese, é desnecessário
         if (pdfPath == null || pdfPath.trim().isEmpty())
            msg = "Arquivo PDF '" + pdfPath + "' é inválido.";
         else {
            pdfPath  = AuthPathService.authPath() + pdfPath;

            Path filePath = Path.of(pdfPath);
            if (!Files.exists(filePath,LinkOption.NOFOLLOW_LINKS))
               msg = "Arquivo '" + pdfPath + "' não encontrado";
            else
               if (Files.isDirectory(filePath,LinkOption.NOFOLLOW_LINKS))
                  msg = "Arquivo '" + pdfPath + "' não pode ser diretório";
               else
                  if (!Files.isRegularFile(filePath,LinkOption.NOFOLLOW_LINKS))
                     msg = "Arquivo '" + pdfPath + "' não é um arquivo regular";
         }
         // se algum dos 'ifs' acima for verdadeiro, a ferramenta não pode
         // continuar sua execução e retornará com erro ao cliente
         if (!msg.isEmpty())
            return createResult(msg,true);

         String metadata = (new PdfMetadataService(pdfPath)).getMetadata();

         return createResult(metadata,false);
      }
      catch (Exception e) {
         return createResult(e.getMessage(),true);
      }
   }

   private CallToolResult createResult(String message, boolean error) {
      String      msg         = message == null || message.trim().isEmpty()
                                ? "Erro desconhecido durante a leitra do PDF."
                                : message;
      TextContent textContent = TextContent.builder(msg)
                                           .build();

      return CallToolResult.builder()
                           .content(List.of(textContent))
                           .isError(error)
                           .build();
   }
}
