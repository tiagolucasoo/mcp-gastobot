package br.edu.ifpr.mcp_server.services.files;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import br.edu.ifpr.mcp_server.main.AppConsts;

class FileSearchServiceTest {
   // nome de arquivo usado em diferentes testes 
   private static final String DUMMY_FILE_NAME = "teste.txt";
   // simula a existência da propriedade "mcp.files.root"
   // (OPCIONALMENTE, é possível usar a anotação @SetSystemProperty)
   private String previousRootProperty;
   // usada para criar um diretório de testes (o diretório é apagado ao final
   // dos testes)
   @TempDir Path tempDir;

   @BeforeAll
   static void setUpBeforeClass() throws Exception {}

   @AfterAll
   static void tearDownAfterClass() throws Exception {}

   @BeforeEach
   void setUp() throws Exception {}

   @AfterEach
   void tearDown() throws Exception {
      // simula a (não) existência da propriedade "mcp.files.root"
      if (previousRootProperty == null)
         System.clearProperty("mcp.files.root");
      else
         System.setProperty("mcp.files.root",previousRootProperty);
   }

   @ParameterizedTest(name="Referência ao arquivo de busca é inválida")
   @NullSource // referência inválida: null
   final void testConstructorNull(String path) {
      assertThrows(IllegalArgumentException.class,
	    			() -> new FileSearchService(null));
   }

   @ParameterizedTest(name="Diretório {0} inválido ou inexistente")
   @ValueSource(strings={ "desenvolvimento" })
   final void testConstructorIOExcpeption(String path) {
      Path root = tempDir.resolve(path);

      assertThrows(IllegalArgumentException.class,
    			                             () -> new FileSearchService(root));
   }

   @ParameterizedTest(name="Arquivo {0} é válido")
   @ValueSource(strings={ "D:\\arquivo.txt" })
   final void testConstructorIllegalArgumentException(String path) {
      // usar um nome de arquivo válido no computador em que este método de
      // teste for executado
      Path root = tempDir.resolve(path);

      assertThrows(IllegalArgumentException.class,
    			   () -> new FileSearchService(root));
   }

   @ParameterizedTest(name="Diretório {0} é válido")
   @ValueSource(strings={ "D:\\desenvolvimento" })
   final void testConstructorValid(String path) throws Exception {
      FileSearchService fss = new FileSearchService(Path.of(path));

      assertNotNull(fss);
   }

   @Test
   @DisplayName("Propriedade '" + AppConsts.AUTH_DIR_PROP + "' e variável " +
                "de ambiente '" + AppConsts.AUTH_DIR_VAR + "' inexistentes")
   final void testFromConfigurationNoPropertyNoVariable() throws IOException {
      // propriedade "mcp.files.root" e variável de ambiente "MCP_FILES_ROOT"
      // inexistentes
      assertThrows(IllegalStateException.class,
                   () -> FileSearchService.fromConfiguration());
   }

   @Test
   @DisplayName("Propriedade '" + AppConsts.AUTH_DIR_PROP + "' encontrada")
   void testFromConfigurationSystemProperty() throws Exception {
      previousRootProperty = System.getProperty("mcp.files.root");

      Files.createFile(tempDir.resolve(DUMMY_FILE_NAME));

      System.setProperty("mcp.files.root",tempDir.toString());

      FileSearchService fss = FileSearchService.fromConfiguration();

      String result = fss.search(DUMMY_FILE_NAME,false,20);

      assertTrue(result.contains(DUMMY_FILE_NAME));
   }

   @Test
   @DisplayName("Variável de ambiente '" + AppConsts.AUTH_DIR_VAR +
                "' encontrada")
   void testFromConfigurationEnvironmentVariable() throws Exception {
      Files.createFile(tempDir.resolve(DUMMY_FILE_NAME));

      FileSearchService fss = new FileSearchService(tempDir);

      String result = fss.search(DUMMY_FILE_NAME,false,20);

      assertTrue(result.contains(DUMMY_FILE_NAME));
   }
}
