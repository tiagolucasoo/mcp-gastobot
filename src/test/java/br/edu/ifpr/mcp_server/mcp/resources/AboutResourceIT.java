package br.edu.ifpr.mcp_server.mcp.resources;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
import io.modelcontextprotocol.spec.McpSchema.ListResourcesResult;
import tools.jackson.databind.json.JsonMapper;

class AboutResourceIT {
   private static final Logger LOG =
                                 LoggerFactory.getLogger(AboutResourceIT.class);
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
   @DisplayName("Verificar se o servidor disponibiliza recursos")
   final void deveDisponibilizarRecursos() {
	   ListResourcesResult result = client.listResources();

      assertNotNull(result,"O servidor não retornou o resultado.");
      assertNotNull(result.resources(),"A lista de recursos deve existir.");
      assertFalse(result.resources().isEmpty(),"O servidor deveria " +
                                               "disponibilizar pelo menos um " +
                                               "recurso.");
      String msg    = "O recurso '%s' não foi " +
                      "encontrado.".formatted(McpConsts.RESOURCE_ABOUT_URI);
      boolean found = result.resources()
                            .stream()
                            .anyMatch(res ->
                                McpConsts.RESOURCE_ABOUT_URI.equals(res.uri()));
      assertTrue(found,msg);
   }
}
