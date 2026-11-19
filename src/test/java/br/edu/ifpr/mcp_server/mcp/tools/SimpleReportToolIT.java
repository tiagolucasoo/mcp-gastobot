package br.edu.ifpr.mcp_server.mcp.tools;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import tools.jackson.databind.json.JsonMapper;

class SimpleReportToolIT {
   private static final Logger LOG =
                              LoggerFactory.getLogger(SimpleReportToolIT.class);
   // nome e caminho para o servidor MCP
   private static final String SERVER_PATH = "server/mcp_server01_server.jar";
   // este é o diretório que o servidor recebe para a ferramenta
   // "FileSearchTool": USE UM CAMINHO QUE EXISTA NO COMPUTADOR EM QUE O
   //                   SERVIDOR É EXECUTADO!!!
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

   // ==========================================================================
   // Os métodos de teste à seguir simulam a aplicação cliente:
   // - há ferramentas (tools) disponíveis???
   // - a ferramenta 'obter_documento' existe???
   // - a ferramenta 'obter_documento' executou com sucesso???
   // ==========================================================================

   @Test
   @DisplayName("Verifica se o servidor disponibiliza ferramentas")
   void testDisponibilizarFerramentas() {
      ListToolsResult result = client.listTools();

      assertNotNull(result,"O servidor não retornou o resultado.");
      assertNotNull(result.tools(),"A lista de ferramentas deve existir.");
      assertFalse(result.tools().isEmpty(),"O servidor deveria disponibilizar" +
                                           " pelo menos uma ferramenta." );
   }

   @Test
   @DisplayName("Verifica se a ferramenta '" + McpConsts.SRT_TOOL_NAME +
                "' existe")
   void testDisponibilizarObterDocumento() {
      ListToolsResult result = client.listTools();
                                     // retorna a lista de ferramentas
      Tool            tool   = result.tools()
                                      // cria um fluxo para percorrer a lista
    			                         .stream()
                                     // filtra a lista para um elemento
    			                         .filter(item ->
    			                        McpConsts.SRT_TOOL_NAME.equals(item.name()))
                                     // verifica se há alguma elemento...
                                     .findFirst()
                                     // ...caso contrário, retorna "null"
                                     .orElse(null);

      assertNotNull(tool,"A ferramenta '" + McpConsts.SRT_TOOL_NAME +
                         "' não foi encontrada.");
      assertNotNull(tool.description(),"A ferramenta deveria possuir " +
                                       "descrição.");
      assertNotNull(tool.inputSchema(),"A ferramenta deveria possuir " +
                                       "esquema JSON.");
   }

   @Test
   @DisplayName("Executa a ferramenta '" + McpConsts.SRT_TOOL_NAME + "'")
   void testExecutarObterDocumento() {
      Map<String, Object> args = Map.of(McpConsts.SRT_ATTR_ID,123);

      CallToolRequest request = CallToolRequest.builder(McpConsts.SRT_TOOL_NAME)
                                               .arguments(args)
                                               .build();
      CallToolResult  result  = client.callTool(request);

      assertNotNull(result,"O servidor não retornou resultado.");
      assertFalse(result.isError(),"A execução da ferramenta não deveria " +
                                   "resultar em erro.");
      assertNotNull(result.content(),"O resultado deveria possuir conteúdo.");
      assertFalse(result.content().isEmpty(),"O resultado deveria possuir " +
                                             "pelo menos um conteúdo." );
   }
}
