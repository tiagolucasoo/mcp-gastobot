package br.edu.ifpr.mcp_server.mcp.prompts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Map;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.edu.ifpr.mcp_server.main.AppConsts;
import br.edu.ifpr.mcp_server.mcp.utils.McpConsts;
import br.edu.ifpr.mcp_server.utils.AuthDirTests;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.ListPromptsResult;
import io.modelcontextprotocol.spec.McpSchema.Prompt;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import tools.jackson.databind.json.JsonMapper;

class AnalyzePdfPromptIT {
   private static final Logger LOG =
                              LoggerFactory.getLogger(AnalyzePdfPromptIT.class);

   // nome e caminho para o servidor MCP
   private static final String SERVER_PATH = "server/mcp_server01_server.jar";
   // este é o diretório que o servidor recebe para a ferramenta
   // "FileSearchTool": USE UM CAMINHO QUE EXISTA NO COMPUTADOR EM QUE O
   //                   TESTE É EXECUTADO!!!
   private static final String MCP_FILES_ROOT = AuthDirTests.AUTH_DIR_TESTS;

   private static McpSyncClient client;

   @BeforeAll
   static void setUpBeforeClass() throws Exception {
      // antes que a classe de testes seja carregada, este método providencia
      // alguma inicialização ou alocação de recursos, neste caso, o servidor
      // MCP é executado e o cliente se conecta a ele
      File serverJar = new File(SERVER_PATH);
      assertTrue(serverJar.isFile(),"JAR do servidor não encontrado: " +
                                    serverJar.getAbsolutePath());
      
      String           jarPath = serverJar.getAbsolutePath();
      ServerParameters params  = ServerParameters.builder("java")
                                                 .args("-jar",jarPath)
                                              .addEnvVar(AppConsts.AUTH_DIR_VAR,
                                                         MCP_FILES_ROOT)
                                                 .build();
      JacksonMcpJsonMapper jsonMapper = new JacksonMcpJsonMapper(
                                                              new JsonMapper());
      StdioClientTransport transport  = new StdioClientTransport(params,
                                                                 jsonMapper);
      
      transport.setStdErrorHandler(line -> LOG.info("[SERVER STDERR] " + line));
      
      client = McpClient.sync(transport).build();
      client.initialize();
   }

   @AfterAll
   static void tearDownAfterClass() throws Exception {
      // ao terminar todos os testes, todos os métodos nesta classe foram
      // executados, o cliente se desconecta do servidor
      if (client != null)
         client.closeGracefully();
   }

   @BeforeEach
   void setUp() throws Exception {}

   @AfterEach
   void tearDown() throws Exception {}

   @Test
   @DisplayName("O prompt '" + McpConsts.ANALYZE_PDF_PROMPT + "' existe?")
   void testListAnalyzedPdfPrompt() {
      ListPromptsResult result = client.listPrompts();
      // existe algum prompt???
      assertNotNull(result);
      // há algum prompt???
      assertNotNull(result.prompts());
      // o prompt desejado foi encontrado???
      assertTrue(result.prompts().stream()
                                 .map(Prompt::name)
                                .anyMatch(McpConsts.ANALYZE_PDF_PROMPT::equals),
                 "O prompt '" + McpConsts.ANALYZE_PDF_PROMPT +
                 "' não foi encontrado.");
   }

   @ParameterizedTest
   @DisplayName("Retornar as instruções para analisar um PDF")
   @MethodSource("provideData")
   void testReturnAnalyzePdfInstructions(String arquivo, String termo) {
      String termoDeBusca = "Verificar a existência de '%s'".formatted(termo);

      GetPromptRequest request =
         GetPromptRequest.builder(McpConsts.ANALYZE_PDF_PROMPT)
                         .arguments(Map.of(McpConsts.ANALYZE_PDF_ATTR_ARQUIVO,
                                           arquivo,
                                           McpConsts.ANALYZE_PDF_ATTR_TERMO,
                                           termo,
                                           McpConsts.ANALYZE_PDF_ATTR_CONTEXTO,
                                           termoDeBusca
                                          )
                                   )
                         .build();

      GetPromptResult result = client.getPrompt(request);
      // temos um resultado da busca pelo prompt???
      assertNotNull(result);
      // o prompt possui mensagens???
      assertEquals(1,result.messages().size());

      PromptMessage message = result.messages().get(0);
      // se há mensagens, elas são válidas???
      assertNotNull(message);
      // o objeto retornado pela mensagem é uma referência esperada???
      TextContent content = assertInstanceOf(TextContent.class,
                                             message.content());
      String      text    = content.text();
      // existe um texto válido???
      assertNotNull(text);
      // há um texto retornado???
      assertFalse(text.isBlank());

      assertTrue(text.contains(arquivo));
      assertTrue(text.contains(termo));
      assertTrue(text.contains(termoDeBusca));
      assertTrue(text.contains(McpConsts.PDFMT_TOOL_NAME));
      assertTrue(text.contains(McpConsts.PDFST_TOOL_NAME));
      assertTrue(text.contains("Não invente metadados"));
   }

   @ParameterizedTest
   @DisplayName("Aceitar a ausência do argumento '" +
                McpConsts.ANALYZE_PDF_ATTR_CONTEXTO + "'")
   @MethodSource("provideData")
   void testAcceptMissingContext(String arquivo, String termo) {
      GetPromptRequest request = 
         GetPromptRequest.builder(McpConsts.ANALYZE_PDF_PROMPT)
                         .arguments(Map.of(McpConsts.ANALYZE_PDF_ATTR_ARQUIVO,
                                           arquivo,
                                           McpConsts.ANALYZE_PDF_ATTR_TERMO,
                                           termo))
                         .build();

      GetPromptResult result = client.getPrompt(request);

      assertNotNull(result);
      assertEquals(1,result.messages().size());

      PromptMessage message = result.messages().get(0);

      TextContent content = assertInstanceOf(TextContent.class,
                                             message.content());

      assertTrue(content.text().contains("Não informado."));
   }

   @Test
   @DisplayName("Os argumentos esperados são publicados?")
   void testExposeExpectedArguments() {
      ListPromptsResult result = client.listPrompts();

      Prompt prompt = result.prompts()
                            .stream()
                            .filter(item -> McpConsts.ANALYZE_PDF_PROMPT
                                                     .equals(item.name()))
                            .findFirst()
                            .orElseThrow();
      // o prompt deve aceitar três argumentos
      assertEquals(3,prompt.arguments().size());

      assertTrue(prompt.arguments()
                       .stream()
                       .anyMatch(argument ->
                                 McpConsts.ANALYZE_PDF_ATTR_ARQUIVO
                                          .equals(argument.name()) &&
                                 Boolean.TRUE.equals(argument.required())));

      assertTrue(prompt.arguments()
                       .stream()
                       .anyMatch(argument ->
                                 McpConsts.ANALYZE_PDF_ATTR_TERMO
                                          .equals(argument.name()) &&
                                 Boolean.TRUE.equals(argument.required())));

      assertTrue(prompt.arguments()
                       .stream()
                       .anyMatch(argument ->
                                 McpConsts.ANALYZE_PDF_ATTR_CONTEXTO
                                          .equals(argument.name()) &&
                                 !Boolean.TRUE.equals(argument.required())));
   }

   // método estático que fornece a massa de dados
   private static Stream<Arguments> provideData() {
      return Stream.of(Arguments.of("arquivo.pdf", // arquivo para busca
                                    "computação"   // termo a buscar
                                   )
                      );
   }
}
