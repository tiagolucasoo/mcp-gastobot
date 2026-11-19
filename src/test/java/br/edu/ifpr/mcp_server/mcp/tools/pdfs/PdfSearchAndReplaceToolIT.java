package br.edu.ifpr.mcp_server.mcp.tools.pdfs;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.ifpr.mcp_server.main.AppConsts;
import br.edu.ifpr.mcp_server.mcp.utils.McpConsts;
import br.edu.ifpr.mcp_server.services.pdfs.PdfProcessorFacade;
import br.edu.ifpr.mcp_server.services.pdfs.model.ReplacementResult;
import br.edu.ifpr.mcp_server.services.pdfs.model.SearchOptions;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

@ExtendWith(MockitoExtension.class)
class PdfSearchAndReplaceToolIT {
   @TempDir
   private Path tempDir;

   @Mock
   private PdfProcessorFacade processorFacade;

   private PdfSearchAndReplaceTool tool;

   private String previousRoot;

   @BeforeEach
   void setUp() {
      previousRoot = System.getProperty(AppConsts.AUTH_DIR_PROP);

      System.setProperty(AppConsts.AUTH_DIR_PROP,tempDir.toString());

      tool = new PdfSearchAndReplaceTool(processorFacade);
   }

   @AfterEach
   void tearDown() {
      if (previousRoot == null)
         System.clearProperty(AppConsts.AUTH_DIR_PROP);
      else
         System.setProperty(AppConsts.AUTH_DIR_PROP,previousRoot);
   }

   @Test
   @DisplayName("Encaminhar corretamente os argumentos ao facade")
   void testProcessAndReplaceWithProvidedArguments() throws Exception {
      Map<String,Object> args = Map.of(McpConsts.PDFSRT_ATTR_INPUT_FILE,
                                       "documento.pdf",
                                       McpConsts.PDFSRT_ATTR_OUTPUT_FILE,
                                       "resultado.pdf",
                                       McpConsts.PDFSRT_ATTR_SEARCH,"Java",
                                       McpConsts.PDFSRT_ATTR_REPLACE,"Python",
                                       McpConsts.PDFSRT_ATTR_USE_REGEX,true,
                                       McpConsts.PDFSRT_ATTR_CASE_SENSITIVE,true
                                      );

      when(processorFacade.processAndReplace(any(File.class),
                                             any(File.class),
                                             any(SearchOptions.class),
                                             eq("Python")
                                            )
          ).thenReturn(new ReplacementResult(2,Map.of(1,2)));

      CallToolResult result = execute(args);

      assertFalse(result.isError());

      Path input = createInputFile("documento.pdf");
      verify(processorFacade).processAndReplace(
                                             eq(input.toFile()),
                                             eq(tempDir.resolve("resultado.pdf")
                                                       .toFile()),
                                             eq(new SearchOptions("Java",true,
                                                                  true)),
                                             eq("Python")
                                               );
   }

   @ParameterizedTest(name = "[{index}] {0}")
   @MethodSource("defaultOutputCases")
   @DisplayName("Gerar nome padrão para o arquivo de saída")
   void testGenerateDefaultOutputFile(String description, String inputName,
                                      String outputName)
        throws Exception {

      Map<String,Object> args = Map.of(McpConsts.PDFSRT_ATTR_INPUT_FILE,
                                       inputName,
                                       McpConsts.PDFSRT_ATTR_SEARCH,"Java",
                                       McpConsts.PDFSRT_ATTR_REPLACE,"Python"
                                      );

      when(processorFacade.processAndReplace(any(File.class),
                                             any(File.class),
                                             any(SearchOptions.class),
                                             eq("Python")
                                             )
          ).thenReturn(new ReplacementResult(0,Map.of()));

      CallToolResult result = execute(args);

      assertFalse(result.isError());

      Path input = createInputFile(inputName);
      verify(processorFacade).processAndReplace(eq(input.toFile()),
                                                eq(tempDir.resolve(outputName)
                                                          .toFile()),
                                                any(SearchOptions.class),
                                                eq("Python")
                                               );
   }

   @Test
   @DisplayName("Retornar erro quando o arquivo de entrada não existir")
   void testRejectNonexistentInputFile() {
      Map<String,Object> args = Map.of(McpConsts.PDFSRT_ATTR_INPUT_FILE,
                                       "inexistente.pdf",
                                       McpConsts.PDFSRT_ATTR_SEARCH,"Java",
                                       McpConsts.PDFSRT_ATTR_REPLACE,"Python"
                                      );

      CallToolResult result = execute(args);

      assertTrue(result.isError());
      assertResultContains(result,"não encontrado");

      verifyNoInteractions(processorFacade);
   }

   @Test
   @DisplayName("Retornar erro quando o caminho de entrada for diretório")
   void testRejectDirectoryAsInputFile() throws IOException {
      Files.createDirectory(tempDir.resolve("diretorio"));

      Map<String,Object> args = Map.of(McpConsts.PDFSRT_ATTR_INPUT_FILE,
                                       "diretorio",
                                       McpConsts.PDFSRT_ATTR_SEARCH,"Java",
                                       McpConsts.PDFSRT_ATTR_REPLACE,"Python"
                                      );

      CallToolResult result = execute(args);

      assertTrue(result.isError());
      assertResultContains(result,"não pode ser diretório");

      verifyNoInteractions(processorFacade);
   }

   @Test
   @DisplayName("Retornar erro quando o caminho não for arquivo regular")
   void testRejectNonRegularInputFile() throws IOException {
      // o Windows não permite criar links simbólicos??!!??!!
//      Files.createSymbolicLink(tempDir.resolve("atalho.pdf"),
//                               tempDir.resolve("alvo.pdf")
//                              );
	   Path link   = tempDir.resolve("atalho.pdf");
	   Path target = tempDir.resolve("alvo.pdf");

      try { Files.createSymbolicLink(link,target); }
      catch (IOException | UnsupportedOperationException |
             SecurityException e) {
         Assumptions.assumeTrue(false,"Não foi possível criar link " +
                                "simbólico: " + e.getMessage());
      }
      Map<String,Object> args = Map.of(McpConsts.PDFSRT_ATTR_INPUT_FILE,
                                       "atalho.pdf",
                                       McpConsts.PDFSRT_ATTR_SEARCH,"Java",
                                       McpConsts.PDFSRT_ATTR_REPLACE,"Python"
                                      );

      CallToolResult result = execute(args);

      assertTrue(result.isError());
      assertResultContains(result,"não é um arquivo regular");

      verifyNoInteractions(processorFacade);
   }

   @Test
   @DisplayName("Retornar relatório de substituição")
   void testReturnReplacementReport() throws Exception {
      createInputFile("documento.pdf");

      Map<String,Object> args = Map.of(McpConsts.PDFSRT_ATTR_INPUT_FILE,
                                       "documento.pdf",
                                       McpConsts.PDFSRT_ATTR_SEARCH,"Java",
                                       McpConsts.PDFSRT_ATTR_REPLACE,"Python"
                                      );

      when(processorFacade.processAndReplace(any(File.class),
                                             any(File.class),
                                             any(SearchOptions.class),
                                             eq("Python")
                                            )
          ).thenReturn(new ReplacementResult(3,Map.of(1,2,2,1)));

      CallToolResult result = execute(args);

      assertFalse(result.isError());
      assertResultContains(result,"Total de ocorrências encontradas e " +
                                  "substituídas: 3");
      assertResultContains(result,"Página [    1]: [    2]");
      assertResultContains(result,"Página [    2]: [    1]");
   }

   @Test
   @DisplayName("Retornar relatório quando nenhuma ocorrência for encontrada")
   void testReturnReportWithoutOccurrences() throws Exception {
      createInputFile("documento.pdf");

      Map<String,Object> args = Map.of(McpConsts.PDFSRT_ATTR_INPUT_FILE,
                                       "documento.pdf",
                                       McpConsts.PDFSRT_ATTR_SEARCH,"Java",
                                       McpConsts.PDFSRT_ATTR_REPLACE,"Python"
                                      );

      when(processorFacade.processAndReplace(any(File.class),
                                             any(File.class),
                                             any(SearchOptions.class),
                                             eq("Python")
                                            )
          ).thenReturn(new ReplacementResult(0,Map.of()));

      CallToolResult result = execute(args);

      assertFalse(result.isError());
      assertResultContains(result,"Total de ocorrências encontradas e " +
                                  "substituídas: 0");
   }

   @Test
   @DisplayName("Retornar erro quando o processamento lançar IOException")
   void testReturnErrorWhenProcessingFails() throws Exception {
      createInputFile("documento.pdf");

      Map<String,Object> args = Map.of(McpConsts.PDFSRT_ATTR_INPUT_FILE,
                                       "documento.pdf",
                                       McpConsts.PDFSRT_ATTR_SEARCH,"Java",
                                       McpConsts.PDFSRT_ATTR_REPLACE,"Python"
                                      );

      when(processorFacade.processAndReplace(any(File.class),
                                             any(File.class),
                                             any(SearchOptions.class),
                                             eq("Python")
                                            )
          ).thenThrow(new IOException("Falha na leitura do PDF"));

      CallToolResult result = execute(args);

      assertTrue(result.isError());
      assertResultContains(result,"Falha na leitura do PDF");
   }

   static Stream<Arguments> defaultOutputCases() {
      return Stream.of(Arguments.of("Arquivo com extensão PDF",
                                    "documento.pdf",
                                    "documento_saida.pdf"
                                   ),
                       Arguments.of("Arquivo com extensão diferente",
                                    "documento.txt",
                                    "documento_saida.txt"
                                   ),
                       Arguments.of("Arquivo sem extensão",
                                    "documento",
                                    "documento_saida"
                                   )
                      );
   }

   private Path createInputFile(String fileName) throws IOException {
      Path input = tempDir.resolve(fileName);

      return Files.createFile(input);
   }

   private CallToolResult execute(Map<String,Object> args) {
      CallToolRequest request = CallToolRequest.builder(
                                                     McpConsts.PDFSRT_TOOL_NAME)
                                               .arguments(args)
                                               .build();

      return tool.run(null,request);
   }

   private void assertResultContains(CallToolResult result,
                                     String expectedText) {
      String text = ((TextContent) result.content().get(0)).text();

      assertTrue(text.contains(expectedText),
                 () -> "Texto esperado não encontrado: " + expectedText +
                       "\nConteúdo recebido: " + text
                );
   }
}
