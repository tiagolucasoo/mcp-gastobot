package br.edu.ifpr.mcp_server.services.pdfs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.edu.ifpr.mcp_server.services.pdfs.model.ReplacementResult;
import br.edu.ifpr.mcp_server.services.pdfs.model.SearchOptions;

class PdfProcessorFacadeTest {
   @TempDir
   private Path tempDir;

   private final PdfProcessorFacade facade = new PdfProcessorFacade();

   @Test
   @DisplayName("Substituir texto em PDF e retornar o resultado")
   void testProcessAndReplaceText() throws IOException {
      Path source = tempDir.resolve("origem.pdf");
      Path target = tempDir.resolve("resultado.pdf");

      createPdf(source,List.of("Java é uma linguagem.",
                               "Java é multiplataforma."
                              )
               );

      SearchOptions options = new SearchOptions("Java",false,true);

      ReplacementResult result = facade.processAndReplace(source.toFile(),
                                                          target.toFile(),
                                                          options,
                                                          "Kotlin"
                                                         );

      assertTrue(Files.exists(target));
      assertEquals(2,result.totalOccurrences());
      assertEquals(Map.of(1,1,2,1),result.occurrencesPerPage());

      try (PDDocument document = Loader.loadPDF(target.toFile())) {
         PDFTextStripper stripper = new PDFTextStripper();
         String extractedText = stripper.getText(document);

         assertTrue(extractedText.contains("Kotlin"));
         assertTrue(!extractedText.contains("Java"));
      }
   }

   @Test
   @DisplayName("Retornar resultado vazio quando não houver ocorrências")
   void testProcessWithoutOccurrences() throws IOException {
      Path source = tempDir.resolve("origem.pdf");
      Path target = tempDir.resolve("resultado.pdf");

      createPdf(source,List.of("Conteúdo da primeira página.",
                               "Conteúdo da segunda página."
                              )
               );

      SearchOptions options = new SearchOptions("Python",false,true);

      ReplacementResult result = facade.processAndReplace(source.toFile(),
                                                          target.toFile(),
                                                          options,
                                                          "Java"
                                                         );

      assertTrue(Files.exists(target));
      assertEquals(0,result.totalOccurrences());
      assertTrue(result.occurrencesPerPage().isEmpty());
   }

   @Test
   @DisplayName("Preservar a quantidade e a ordem das páginas")
   void testPreservePageCountAndOrder() throws IOException {
      Path source = tempDir.resolve("origem.pdf");
      Path target = tempDir.resolve("resultado.pdf");

      createPdf(source,List.of("Primeira página.","Segunda página.",
                               "Terceira página."
                              )
               );

      SearchOptions options = new SearchOptions("inexistente",false,true);

      facade.processAndReplace(source.toFile(),target.toFile(),options,
                               "substituído");

      try (PDDocument document = Loader.loadPDF(target.toFile())) {
         assertEquals(3,document.getNumberOfPages());

         PDFTextStripper stripper = new PDFTextStripper();

         for (int i = 1; i <= 3; i++) {
            stripper.setStartPage(i);
            stripper.setEndPage(i);

            String extractedText = stripper.getText(document);

            assertTrue(
               extractedText.contains(
                  i == 1 ? "Primeira"
                         : i == 2 ? "Segunda" : "Terceira"
               ),
               "Conteúdo incorreto na página " + i
            );
         }
      }
   }

   /**
    * Cria um PDF simples com uma página para cada texto informado.
    *
    * @param path caminho do PDF
    * @param texts textos das páginas
    * @throws IOException se ocorrer erro na criação do PDF
    */
   private void createPdf(Path path,List<String> texts)
         throws IOException {
      try (PDDocument document = new PDDocument()) {
         PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

         for (String text : texts) {
            PDPage page = new PDPage();

            document.addPage(page);

            try (PDPageContentStream content =
                                       new PDPageContentStream(document,page)) {
               content.beginText();
               content.setFont(font,12);
               content.newLineAtOffset(50,750);
               content.showText(text);
               content.endText();
            }
         }
         document.save(path.toFile());
      }
   }
}
