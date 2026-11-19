package br.edu.ifpr.mcp_server.mcp.tools.pdfs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.edu.ifpr.mcp_server.main.AppConsts;
import br.edu.ifpr.mcp_server.mcp.utils.McpConsts;
import br.edu.ifpr.mcp_server.services.pdfs.PdfMetadataService;
import br.edu.ifpr.mcp_server.utils.AuthDirTests;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import tools.jackson.databind.json.JsonMapper;

class PdfMetadataToolIT {
   private static final Logger LOG =
                               LoggerFactory.getLogger(PdfMetadataToolIT.class);

   private static final String SERVER_PATH = "server/mcp_server01_server.jar";

   private static final String MCP_FILES_ROOT = AuthDirTests.AUTH_DIR_TESTS;

   private static McpSyncClient client;

   @BeforeAll
   static void setUpBeforeClass() throws Exception {
      Path jarPath = Path.of(SERVER_PATH).toAbsolutePath();

      assertTrue(Files.isRegularFile(jarPath),
                 "JAR do servidor não encontrado: " + jarPath);

      ServerParameters params = ServerParameters.builder("java")
                                                .args("-jar",jarPath.toString())
                                              .addEnvVar(AppConsts.AUTH_DIR_VAR,
                                                           MCP_FILES_ROOT)
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
   @DisplayName("A ferramenta '" + McpConsts.PDFMT_TOOL_NAME + "' deve existir")
   void testToolShouldExist() {
      List<String> toolNames = client.listTools().tools()
                                                 .stream()
                                                 .map(tool -> tool.name())
                                                 .toList();

      assertTrue(toolNames.contains(McpConsts.PDFMT_TOOL_NAME),
                 "Ferramenta '" + McpConsts.PDFMT_TOOL_NAME +
                 "' não encontrada.");
   }

   @ParameterizedTest(name="O arquivo {0} deve existir")
   @ValueSource(strings={ "arquivo - SEM SENHA.pdf" })
   void testValidPdfWithMetadata(String fileName)
              throws Exception, IOException {
      PdfMetadataService pcs = new PdfMetadataService(fileName);

      String metadata = pcs.getMetadata();

      assertTrue(metadata.length() > 0);
   }

   @ParameterizedTest(name="O título {1} deve coincidir com os metadados no " +
                           "arquivo {0}")
   @MethodSource("provideData")
   void testMetadataTitle(String fileName, String title)
              throws Exception {
      PdfMetadataService pcs = new PdfMetadataService(fileName);

      String metadata = pcs.getMetadata();

      assertThat(metadata,containsString(title));
   }

   // método estático que fornece a massa de dados
   private static Stream<Arguments> provideData() {
      return Stream.of(Arguments.of("arquivo - SEM SENHA.pdf",
                                    "Microsoft Word - CR99.01.rtf"
                                   )
                      );
   }
}
