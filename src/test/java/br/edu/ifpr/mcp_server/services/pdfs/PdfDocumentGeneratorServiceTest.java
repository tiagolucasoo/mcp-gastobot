package br.edu.ifpr.mcp_server.services.pdfs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PdfDocumentGeneratorServiceTest {
   @TempDir
   private Path tempDir;

   private final PdfDocumentGeneratorService service =
                                              new PdfDocumentGeneratorService();

   @Test
   @DisplayName("Gerar PDF com uma única página")
   void testGeneratePdfWithSinglePage() throws IOException {
      Path output = tempDir.resolve("documento.pdf");

      service.createPdfFromTexts(List.of("Texto da primeira página"),
                                         output.toFile());

      assertTrue(Files.exists(output));

      try (PDDocument document = Loader.loadPDF(output.toFile())) {
         assertEquals(1,document.getNumberOfPages());
      }
   }

   @Test
   @DisplayName("Gerar PDF com múltiplas páginas")
   void testGeneratePdfWithMultiplePages() throws IOException {
      Path         output = tempDir.resolve("documento.pdf");
      List<String> pages  = List.of("Texto da primeira página",
                                    "Texto da segunda página",
                                    "Texto da terceira página"
                                   );

      service.createPdfFromTexts(pages,output.toFile());

      try (PDDocument document = Loader.loadPDF(output.toFile())) {
         assertEquals(3,document.getNumberOfPages());

         PDFTextStripper stripper = new PDFTextStripper();

         for (int i = 0; i < pages.size(); i++) {
            stripper.setStartPage(i + 1);
            stripper.setEndPage(i + 1);

            String extractedText = stripper.getText(document);

            assertTrue(extractedText.contains(pages.get(i)),
                       "Conteúdo não encontrado na página " + (i + 1));
         }
      }
   }

   @Test
   @DisplayName("Gerar PDF com texto vazio")
   void testGeneratePdfWithEmptyText() throws IOException {
      Path output = tempDir.resolve("vazio.pdf");

      service.createPdfFromTexts(List.of(""),output.toFile());

      try (PDDocument document = Loader.loadPDF(output.toFile())) {
         assertEquals(1,document.getNumberOfPages());

         String extractedText = new PDFTextStripper().getText(document);

         assertTrue(extractedText.isBlank());
      }
   }

   @Test
   @DisplayName("Preservar o conteúdo textual fornecido")
   void testGeneratePdfPreservingText() throws IOException {
      Path   output       = tempDir.resolve("conteudo.pdf");
      String expectedText = "Java MCP Server - PDF Document Generator";

      service.createPdfFromTexts(List.of(expectedText),output.toFile());

      try (PDDocument document = Loader.loadPDF(output.toFile())) {
         String extractedText = new PDFTextStripper().getText(document);

         assertTrue(extractedText.contains(expectedText));
      }
   }

   @Test
   @DisplayName("Quebrar linhas para textos extensos")
   void testGeneratePdfWithLongText() throws IOException {
      int    copies = 100;
      Path   output = tempDir.resolve("texto-longo.pdf");
      String text = String.join(" ",Collections.nCopies(copies,"Java"));

      service.createPdfFromTexts(List.of(text),output.toFile());

      try (PDDocument document = Loader.loadPDF(output.toFile())) {
         String extractedText = new PDFTextStripper().getText(document);

         assertTrue(extractedText.contains("Java"));

         long lineCount = extractedText.lines()
                                       .filter(line -> !line.isBlank())
                                       .count();

         assertTrue(lineCount > 1,"O texto deveria ocupar mais de uma linha");

         assertEquals(copies,extractedText.split("\\s+").length);
      }
   }

   @Test
   @DisplayName("Rejeitar diretório de saída inexistente")
   void testRejectInvalidOutputPath() {
      Path output = tempDir.resolve("diretorio-inexistente")
                           .resolve("documento.pdf");

      assertThrows(IOException.class,
                   () -> service.createPdfFromTexts(List.of("Texto de teste"),
                                                    output.toFile()
                                                   )
                  );
      assertFalse(Files.exists(output));
   }
}
