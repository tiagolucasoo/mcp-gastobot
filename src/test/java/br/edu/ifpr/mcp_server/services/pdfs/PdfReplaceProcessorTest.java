package br.edu.ifpr.mcp_server.services.pdfs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import br.edu.ifpr.mcp_server.services.pdfs.model.ReplacementResult;
import br.edu.ifpr.mcp_server.services.pdfs.model.SearchOptions;
import br.edu.ifpr.mcp_server.services.pdfs.processors.PdfPage;
import br.edu.ifpr.mcp_server.services.pdfs.processors.PdfReplaceProcessor;

class PdfReplaceProcessorTest {
   @ParameterizedTest(name="[{index}] {0}")
   @MethodSource("replacementCases")
   @DisplayName("Substituir corretamente os textos informados")
   void testReplaceText(String description, String input,
                        SearchOptions options, String replacement,
                        String expected) {
      PdfReplaceProcessor processor = new PdfReplaceProcessor(options,
                                                              replacement);

      processor.process(new PdfPage(1,input));

      assertEquals(expected,processor.getProcessedPages().get(0));
   }

   @Test
   @DisplayName("Contabilizar corretamente as ocorrências por página")
   void testCountOccurrencesPerPage() {
      PdfReplaceProcessor processor = new PdfReplaceProcessor(
                                 new SearchOptions("Java",false,true),"Python");

      processor.process(new PdfPage(1,"Java Java"));
      processor.process(new PdfPage(2,"Python"));
      processor.process(new PdfPage(3,"Java Java Java"));

      ReplacementResult result = processor.getReplacementResult();

      assertEquals(5,result.totalOccurrences());
      assertEquals(Map.of(1,2,3,3),result.occurrencesPerPage());
   }

   @Test
   @DisplayName("Não deve incluir páginas sem ocorrências no resultado")
   void testExcludePagesWithoutOccurrences() {
      PdfReplaceProcessor processor = new PdfReplaceProcessor(
                                 new SearchOptions("Java",false,true),"Python");

      processor.process(new PdfPage(1,"Java"));
      processor.process(new PdfPage(2,"Texto sem ocorrência"));
      processor.process(new PdfPage(3,"Java"));

      ReplacementResult result = processor.getReplacementResult();

      assertEquals(2,result.totalOccurrences());
      assertEquals(Map.of(1,1,3,1),result.occurrencesPerPage());
   }

   @Test
   @DisplayName("Preservar a ordem das páginas processadas")
   void testPreservePageOrder() {
      PdfReplaceProcessor processor = new PdfReplaceProcessor(
                                 new SearchOptions("Java",false,true),"Python");

      processor.process(new PdfPage(1,"Primeira página: Java"));
      processor.process(new PdfPage(2,"Segunda página: Java"));
      processor.process(new PdfPage(3,"Terceira página: Java"));

      assertEquals(List.of("Primeira página: Python",
                           "Segunda página: Python",
                           "Terceira página: Python"
                          ),
                   processor.getProcessedPages()
                  );
   }

   @Test
   @DisplayName("Retornar uma lista imutável de páginas processadas")
   void testReturnImmutableProcessedPages() {
      PdfReplaceProcessor processor = new PdfReplaceProcessor(
                                 new SearchOptions("Java",false,true),"Python");

      processor.process(new PdfPage(1,"Java"));

      assertThrows(UnsupportedOperationException.class,
                   () -> processor.getProcessedPages().add("Outra página")
                  );
   }

   static Stream<Arguments> replacementCases() {
      return Stream.of(
            Arguments.of(
                  "Substituição literal",
                  "O Java é uma linguagem.",
                  new SearchOptions("Java",false,true),
                  "Python",
                  "O Python é uma linguagem."
            ),
            Arguments.of(
                  "Substituição de múltiplas ocorrências",
                  "Java e Java são tecnologias diferentes.",
                  new SearchOptions("Java",false,true),
                  "Python",
                  "Python e Python são tecnologias diferentes."
            ),
            Arguments.of(
                  "Substituição utilizando expressão regular",
                  "item123 item456",
                  new SearchOptions("item\\d+",true,true),
                  "produto",
                  "produto produto"
            ),
            Arguments.of(
                  "Substituição com grupo de captura",
                  "Nome: Romualdo",
                  new SearchOptions("Nome: (\\w+)",true,true),
                  "Autor: $1",
                  "Autor: Romualdo"
            ),
            Arguments.of(
                  "Busca sem diferenciar maiúsculas e minúsculas",
                  "Java JAVA java",
                  new SearchOptions("java",false,false),
                  "Python",
                  "Python Python Python"
            ),
            Arguments.of(
                  "Busca sem ocorrências",
                  "O texto não contém o termo pesquisado.",
                  new SearchOptions("Python",false,true),
                  "Java",
                  "O texto não contém o termo pesquisado."
            )
      );
   }
}
