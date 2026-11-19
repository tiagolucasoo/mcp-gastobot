package br.edu.ifpr.mcp_server.mcp.tools.files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.edu.ifpr.mcp_server.mcp.utils.McpConsts;
import br.edu.ifpr.mcp_server.services.files.FileSearchService;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

@ExtendWith(MockitoExtension.class)
class FileSearchToolIT {
   private static final Logger LOG =
                                LoggerFactory.getLogger(FileSearchToolIT.class);
//   // nome e caminho para o servidor MCP
//   private static final String SERVER_PATH    = "server/mcp_server01_server.jar";
//   // este é o diretório que o servidor recebe para a ferramenta
//   // "FileSearchTool": USE UM CAMINHO QUE EXISTA NO COMPUTADOR EM QUE O
//   //                   SERVIDOR É EXECUTADO!!!
//   private static final String MCP_FILES_ROOT = "D:\\desenvolvimento";
//
//   private static McpSyncClient client;

   // o serviço utilizado pela ferramenta será "simulado" ('mocking') ao invés
   // de um objeto real
   @Mock
   private FileSearchService service;
   // o atributo "exchange" é desnecessário, visto que o método run() da
   // ferramenta não o utiliza, mas está aqui para garantir que o método recebe
   // "algo"
   @Mock
   private McpSyncServerExchange exchange;
   // ferramenta a ser testada
   private FileSearchTool tool;

   @BeforeAll
   static void setUpBeforeClass() throws Exception {
//      // antes que a classe de testes seja carregada, este método providencia
//      // alguma inicialização ou alocação de recursos, neste caso, o servidor
//      // MCP é executado e o cliente se conecta a ele
//      File serverJar = new File(SERVER_PATH);
//      assertTrue(serverJar.isFile(),"JAR do servidor não encontrado: " +
//                                    serverJar.getAbsolutePath());
//      
//      String           jarPath = serverJar.getAbsolutePath();
//      ServerParameters params  = ServerParameters.builder("java")
//                                                 .args("-jar",jarPath)
//                                                 .addEnvVar("MCP_FILES_ROOT",
//                                                            MCP_FILES_ROOT)
//                                                 .build();
//      JacksonMcpJsonMapper jsonMapper = new JacksonMcpJsonMapper(
//                                                              new JsonMapper());
//      StdioClientTransport transport  = new StdioClientTransport(params,
//                                                                 jsonMapper);
//      
//      transport.setStdErrorHandler(line -> LOG.info("[SERVER STDERR] " + line));
//      
//      client = McpClient.sync(transport).build();
//      client.initialize();
   }

   @AfterAll
   static void tearDownAfterClass() throws Exception {
//      // ao terminar todos os testes, todos os métodos nesta classe foram
//      // executados, o cliente se desconecta do servidor
//      if (client != null)
//         client.closeGracefully();
   }

   @BeforeEach
   void setUp() throws Exception {
      tool = new FileSearchTool(service);
   }

   @AfterEach
   void tearDown() throws Exception {}

   @Test
   @DisplayName("Executar busca com os argumentos informados")
   final void testSearchFilesWithProvidedArguments() throws Exception {
      Map<String,Object> args = Map.of(McpConsts.FST_ATTR_PATTERN,"*.pdf",
                                       McpConsts.FST_ATTR_RECURSIVE,true,
                                       McpConsts.FST_ATTR_MAX_RES,10
                                      );

      CallToolRequest request = CallToolRequest.builder(McpConsts.FST_TOOL_NAME)
                                               .arguments(args)
                                               .build();

      when(service.search("*.pdf",true,10))
         .thenReturn("arquivo1.pdf\narquivo2.pdf");

      CallToolResult result = tool.run(exchange,request);

      verify(service).search("*.pdf",true,10);

      assertFalse(result.isError());
      assertEquals("arquivo1.pdf\narquivo2.pdf",
                   ((TextContent) result.content().get(0)).text());
   }

   @Test
   @DisplayName("Utilizar os valores padrão dos argumentos opcionais")
   void testUseDefaultArguments() throws Exception {
      Map<String,Object> args = Map.of(McpConsts.FST_ATTR_PATTERN,"*.pdf");

      CallToolRequest request = CallToolRequest.builder(
                                                      McpConsts.FST_TOOL_NAME)
                                               .arguments(args)
                                               .build();

      when(service.search("*.pdf",false,20))
         .thenReturn("arquivo.pdf");

      CallToolResult result = tool.run(exchange,request);

      verify(service).search("*.pdf",false,20);
      assertFalse(result.isError());
   }

   @Test
   @DisplayName("Retornar erro quando o serviço falhar")
   void testReturnErrorWhenServiceFails() throws Exception {
      Map<String,Object> args = Map.of(McpConsts.FST_ATTR_PATTERN,"*.pdf");

      CallToolRequest request = CallToolRequest.builder(McpConsts.FST_TOOL_NAME)
                                               .arguments(args)
                                               .build();

      when(service.search("*.pdf",false,20))
         .thenThrow(new IllegalArgumentException("Padrão inválido"));

      CallToolResult result = tool.run(exchange,request);

      assertTrue(result.isError());
      assertTrue(((TextContent) result.content()
                                      .get(0))
                                      .text()
                                      .contains("Padrão inválido"));
   }
}
