package br.edu.ifpr.mcp_server.mcp.tools.pdfs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.edu.ifpr.mcp_server.main.AppConsts;
import br.edu.ifpr.mcp_server.mcp.utils.McpConsts;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import tools.jackson.databind.json.JsonMapper;

class PdfSearchToolIT {
   private static final Logger LOG =
                                 LoggerFactory.getLogger(PdfSearchToolIT.class);

   private static final String SERVER_PATH = "server/mcp_server01_server.jar";

   private static McpSyncClient client;

   @TempDir
   static Path tempDir;

   @BeforeAll
   static void setUpBeforeClass() throws Exception {
      Path jarPath = Path.of(SERVER_PATH).toAbsolutePath();

      assertTrue(Files.isRegularFile(jarPath),
                 "JAR do servidor não encontrado: " + jarPath);

      String           prop   = "-D%s=%s".formatted(AppConsts.AUTH_DIR_PROP,
                                                 // ??????????????????????
                                                    tempDir.toAbsolutePath());
      ServerParameters params = ServerParameters.builder("java")
                                                .args(prop,
                                                      "-jar",
                                                      jarPath.toString())
                                                .build();

      JacksonMcpJsonMapper mapper = new JacksonMcpJsonMapper(new JsonMapper());

      StdioClientTransport transport = new StdioClientTransport(params,mapper);

      transport.setStdErrorHandler(line -> LOG.info("[SERVER STDERR] {}",line));

      client = McpClient.sync(transport).build();

      client.initialize();
   }

   @AfterAll
   static void tearDownAfterClass() throws Exception {
      if (client != null)
         client.closeGracefully();
   }

   @BeforeEach
   void setUp() throws Exception {}

   @AfterEach
   void tearDown() throws Exception {}

   @Test
   @DisplayName("A ferramenta '" + McpConsts.PDFST_TOOL_NAME +
                "' deve ser encontrada")
   void testRegisterPdfSearchTool() {
      List<String> toolNames = client.listTools().tools()
                                                 .stream()
                                                 .map(tool -> tool.name())
                                                 .toList();

      assertTrue(toolNames.contains(McpConsts.PDFST_TOOL_NAME),
                 "Ferramenta '" + McpConsts.PDFST_TOOL_NAME +
                 "' não encontrada.");
   }

   @Test
   @DisplayName("Contagem de várias ocorrências em uma página")
   void testCountOccurrencesOnSinglePage() throws IOException {
      Path           pdf    = createPdf("single-page.pdf","Java Java java");
      CallToolResult result = search(pdf,"Java",Map.of());

      assertFalse(result.isError(),getResponseText(result));

      String response = getResponseText(result);

      assertTrue(response.contains("Total de ocorrências: 2"));
      assertTrue(response.contains("Página 1: 2"));
   }

   @Test
   @DisplayName("Contagem de ocorrências distribuídas em várias páginas")
   void testCountOccurrencesAcrossMultiplePages() throws IOException {

      Path           pdf    = createPdf("multiple-pages.pdf","Java Java","Java",
                                        "Python");
      CallToolResult result = search(pdf,"Java",Map.of());

      assertFalse(result.isError(),getResponseText(result));

      String response = getResponseText(result);

      assertTrue(response.contains("Total de ocorrências: 3"));
      assertTrue(response.contains("Página 1: 2"));
      assertTrue(response.contains("Página 2: 1"));
      assertFalse(response.contains("Página 3:"));
   }

   @Test
   @DisplayName("Verifica a pesquisa sem diferenciação entre maiúsculas " +
                "e minúsculas")
   void testSearchIgnoringCase() throws IOException {
      Path               pdf     = createPdf("case-insensitive.pdf",
                                             "Java java JAVA");
      Map<String,Object> options = Map.of(McpConsts.PDFST_ATTR_USE_REGEX,
                                          false,
                                          McpConsts.PDFST_ATTR_CASE_SENSITIVE,
                                          false);

      CallToolResult result = search(pdf,"java",options);

      assertFalse(result.isError(),getResponseText(result));

      String response = getResponseText(result);

      assertTrue(response.contains("Total de ocorrências: 3"));
      assertTrue(response.contains("Página 1: 3"));
   }

   @Test
   @DisplayName("Verifica a pesquisa utilizando expressão regular")
   void testSearchUsingRegularExpression() throws IOException {
      Path               pdf     = createPdf("regex.pdf","Java java JAVA");
      Map<String,Object> options = Map.of(McpConsts.PDFST_ATTR_USE_REGEX,
                                          true,
                                          McpConsts.PDFST_ATTR_CASE_SENSITIVE,
                                          false);

      CallToolResult result = search(pdf,"J.va",options);

      assertFalse(result.isError(),getResponseText(result));

      String response = getResponseText(result);

      assertTrue(response.contains("Total de ocorrências: 3"));
      assertTrue(response.contains("Página 1: 3"));
   }

   @Test
   @DisplayName("Verifica o resultado quando nenhuma ocorrência é encontrada")
   void testReturnZeroWhenTextIsNotFound() throws IOException {

      Path pdf = createPdf("not-found.pdf","Java Jakarta Maven");

      CallToolResult result = search(pdf,"Python",Map.of());

      assertFalse(result.isError(),getResponseText(result));

      String response = getResponseText(result);

      assertTrue(response.contains("Total de ocorrências: 0"));
      assertTrue(response.contains("Nenhuma ocorrência encontrada."));
   }

   @Test
   @DisplayName("Verifica o tratamento de uma expressão regular inválida")
   void testReturnErrorForInvalidRegularExpression()
         throws IOException {

      Path pdf = createPdf("invalid-regex.pdf","Java Jakarta Maven");

      Map<String,Object> options = Map.of(McpConsts.PDFST_ATTR_USE_REGEX,
                                          true,
                                          McpConsts.PDFST_ATTR_CASE_SENSITIVE,
                                          true);

      CallToolResult result = search(pdf,"[",options);

      assertTrue(result.isError());
      assertFalse(getResponseText(result).isBlank());
   }

   @Test
   @DisplayName("Verifica o tratamento de um arquivo PDF inexistente")
   void testReturnErrorWhenPdfDoesNotExist() {
      Path           pdf    = tempDir.resolve("missing.pdf");
      CallToolResult result = search(pdf,"Java",Map.of());

      assertTrue(result.isError());
      assertFalse(getResponseText(result).isBlank());
   }

   // cria um PDF temporário contendo o texto informado em cada página.
   private Path createPdf(String fileName, String... pagesText)
                throws IOException {
      Path pdf = tempDir.resolve(fileName);

      try (PDDocument document = new PDDocument()) {
         PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

         for (String pageText : pagesText) {
            PDPage page = new PDPage(PDRectangle.A4);

            document.addPage(page);

            try (PDPageContentStream content =
                                       new PDPageContentStream(document,page)) {
               content.beginText();
               content.setFont(font,11);
               content.newLineAtOffset(50,750);
               content.showText(pageText);
               content.endText();
            }
         }
         document.save(pdf.toFile());
      }
      return pdf;
   }

   // executa a ferramenta de pesquisa através do cliente MCP.
   private CallToolResult search(Path pdf, String targetText,
                                 Map<String,Object> options) {
      Map<String,Object> args = new HashMap<>();

      args.put(McpConsts.PDFST_ATTR_INPUT_FILE,pdf.toAbsolutePath().toString());
      args.put(McpConsts.PDFST_ATTR_SEARCH,targetText);
      args.putAll(options);

      CallToolRequest request = CallToolRequest.builder(
                                                      McpConsts.PDFST_TOOL_NAME)
                                               .arguments(args)
                                               .build();

      return client.callTool(request);
   }

   // extrai o texto da resposta MCP.
   private String getResponseText(CallToolResult result) {
      return result.content()
                   .stream()
                   .filter(TextContent.class::isInstance)
                   .map(TextContent.class::cast)
                   .map(TextContent::text)
                   .findFirst()
                   .orElse("");
   }
}
