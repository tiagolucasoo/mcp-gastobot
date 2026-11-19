package br.edu.ifpr.mcp_server.services.pdfs.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReplacementResultTest {
   @Test
   @DisplayName("Criar resultado com os dados informados")
   void testCreateReplacementResult() {
      Map<Integer,Integer> occurrences = Map.of(1,2,3,4);

      ReplacementResult result = new ReplacementResult(6,occurrences);

      assertEquals(6,result.totalOccurrences());
      assertEquals(occurrences,result.occurrencesPerPage());
   }

   @Test
   @DisplayName("Aceitar mapa de ocorrências vazio")
   void testCreateReplacementResultWithEmptyMap() {
      ReplacementResult result = new ReplacementResult(0,Map.of());

      assertEquals(0,result.totalOccurrences());
      assertTrue(result.occurrencesPerPage().isEmpty());
   }

   @Test
   @DisplayName("Preservar ocorrências de múltiplas páginas")
   void testCreateReplacementResultWithMultiplePages() {
      Map<Integer,Integer> occurrences = Map.of(1,3,
                                                2,5,
                                                4,2,
                                                7,6
                                               );

      ReplacementResult result = new ReplacementResult(16,occurrences);

      assertEquals(16,result.totalOccurrences());
      assertEquals(4,result.occurrencesPerPage().size());
      assertEquals(3,result.occurrencesPerPage().get(1));
      assertEquals(5,result.occurrencesPerPage().get(2));
      assertEquals(2,result.occurrencesPerPage().get(4));
      assertEquals(6,result.occurrencesPerPage().get(7));
   }

   @Test
   @DisplayName("Não deve permitir modificar o mapa de ocorrências")
   void testReturnImmutableOccurrencesPerPage() {
      ReplacementResult result = new ReplacementResult(2,Map.of(1,2));

      assertThrows(UnsupportedOperationException.class,
                   () -> result.occurrencesPerPage().put(2,3)
                  );
   }

   @Test
   @DisplayName("Proteger o resultado contra alterações no mapa original")
   void testDefensiveCopyOfOccurrencesPerPage() {
      Map<Integer,Integer> occurrences = new HashMap<>();

      occurrences.put(1,2);

      ReplacementResult result = new ReplacementResult(2,occurrences);

      occurrences.put(2,3);
      occurrences.remove(1);

      assertEquals(Map.of(1,2),result.occurrencesPerPage());
   }
}
