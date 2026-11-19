package br.edu.ifpr.mcp_server.services.moffice.word;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MSWordCounterServiceTest {
   @BeforeAll
   static void setUpBeforeClass() throws Exception {}

   @AfterAll
   static void tearDownAfterClass() throws Exception {}

   @BeforeEach
   void setUp() throws Exception {}

   @AfterEach
   void tearDown() throws Exception {}

   @ParameterizedTest(name="Arquivo do MS Word não existe: {0}")
   @ValueSource(strings={ "" })
   final void testMSWordFileDoesNotExist(String fileName) {
	   MSWordCounterService service = new MSWordCounterService(fileName);

       assertThrows(IOException.class, () -> { service.countWords(); });

   }

   @ParameterizedTest(name="Calcula a quantidade de palavras do arquivo {0}")
   // D:\\desenvolvimento\\Textos & Materiais\\Java\\01. Básico\\Java - Básico_v2 - TESTE.docx: 8179
   @ValueSource(strings={ "D:\\desenvolvimento\\Textos & Materiais\\Java\\01. Básico\\Java - Básico_v2 - TESTE.docx" })
   final void testMSWordWordCount(String fileName) throws IOException {
	   MSWordCounterService service = new MSWordCounterService(fileName);
       long                 words   = service.countWords();

       assertEquals(8179L,words);
   }

   @ParameterizedTest(name="Calcula a quantidade de parágrafos do arquivo {0}")
   // D:\\desenvolvimento\\Textos & Materiais\\Java\\01. Básico\\Java - Básico_v2 - TESTE.docx: 493
   @ValueSource(strings={ "D:\\desenvolvimento\\Textos & Materiais\\Java\\01. Básico\\Java - Básico_v2 - TESTE.docx" })
   final void testMSWordParagraphCount(String fileName) throws IOException {
	   MSWordCounterService service = new MSWordCounterService(fileName);
       long                 words   = service.countParagraphs();

       assertEquals(493L,words);
   }
}
