package br.edu.ifpr.mcp_server.services.pdfs.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SearchOptionsTest {
   @Test
   @DisplayName("Encontrar um de dois textos")
   void testBuscarTextoLiteral() {
      SearchOptions options = new SearchOptions("a+b",false,true);

      Pattern pattern = options.toPattern();

      assertTrue(pattern.matcher("Texto a+b encontrado").find());
      assertFalse(pattern.matcher("Texto aaab encontrado").find());
   }

   @Test
   @DisplayName("Encontrar dois textos")
   void testInterpretarExpressaoRegular() {
      SearchOptions options = new SearchOptions("a+b",true,true);

      Pattern pattern = options.toPattern();

      assertTrue(pattern.matcher("Texto aaab encontrado").find());
      assertFalse(pattern.matcher("Texto a+b encontrado").find());
   }

   @Test
   @DisplayName("Encontrar três textos")
   void testIgnorarDiferencaEntreMaiusculasEMinusculas() {
      SearchOptions options = new SearchOptions("java",false,false);

      Pattern pattern = options.toPattern();

      assertTrue(pattern.matcher("Java").find());
      assertTrue(pattern.matcher("JAVA").find());
      assertTrue(pattern.matcher("java").find());
   }

   @Test
   @DisplayName("Encontrar um de três textos")
   void testDiferenciarMaiusculasEMinusculas() {
      SearchOptions options = new SearchOptions("Java",false,true);

      Pattern pattern = options.toPattern();

      assertTrue(pattern.matcher("Java").find());
      assertFalse(pattern.matcher("JAVA").find());
      assertFalse(pattern.matcher("java").find());
   }

   @Test
   @DisplayName("Encontrar dois textos")
   void testReconhecerCaracteresUnicode() {
      SearchOptions options = new SearchOptions("ação",false,false);

      Pattern pattern = options.toPattern();

      assertTrue(pattern.matcher("AÇÃO").find());
      assertTrue(pattern.matcher("Ação").find());
   }

   @Test
   @DisplayName("Expressão regular inválida")
   void testLancarExcecaoParaExpressaoRegularInvalida() {
      SearchOptions options = new SearchOptions("[abc",true,true);

      assertThrows(PatternSyntaxException.class,options::toPattern);
   }
}
