package br.edu.ifpr.mcp_server.services.pdfs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.edu.ifpr.mcp_server.services.pdfs.processors.PdfPage;

class PdfPageReaderServiceTest {
   @TempDir
   private Path tempDir;

   private final PdfPageReaderService service = new PdfPageReaderService();

   @Test
   @DisplayName("Processar corretamente uma única página")
   void testProcessSinglePage() throws IOException {
      Path pdf = createPdf("documento.pdf","Texto da primeira página");

      List<PdfPage> pages = new ArrayList<>();

      service.processPages(pdf.toFile(),pages::add);

      assertEquals(1,pages.size());
      assertEquals(1,pages.get(0).pageNumber());
      assertTrue(pages.get(0).text().contains("Texto da primeira página"));
   }

   @Test
   @DisplayName("Processar múltiplas páginas na ordem correta")
   void testProcessMultiplePages() throws IOException {
      Path pdf = createPdf("documento.pdf",
                           "Texto da primeira página",
                           "Texto da segunda página",
                           "Texto da terceira página"
                          );

      List<PdfPage> pages = new ArrayList<>();

      service.processPages(pdf.toFile(),pages::add);

      assertEquals(3,pages.size());

      assertEquals(1,pages.get(0).pageNumber());
      assertTrue(pages.get(0).text().contains("Texto da primeira página"));

      assertEquals(2,pages.get(1).pageNumber());
      assertTrue(pages.get(1).text().contains("Texto da segunda página"));

      assertEquals(3,pages.get(2).pageNumber());
      assertTrue(pages.get(2).text().contains("Texto da terceira página"));
   }

   @Test
   @DisplayName("Processar uma página sem conteúdo textual")
   void testProcessEmptyPage() throws IOException {
      Path pdf = createPdf("vazio.pdf","");

      List<PdfPage> pages = new ArrayList<>();

      service.processPages(pdf.toFile(),pages::add);

      assertEquals(1,pages.size());
      assertEquals(1,pages.get(0).pageNumber());
      assertTrue(pages.get(0).text().isBlank());
   }

   @Test
   @DisplayName("Propagar IOException lançada pelo processador")
   void testPropagateProcessorIOException() throws IOException {
      Path pdf = createPdf("documento.pdf","Texto de teste");

      IOException ex =
         assertThrows(
            IOException.class,
            () -> service.processPages(
                    pdf.toFile(),
                    _ -> { throw new IOException("Falha no processamento"); }
                  )
         );

      assertEquals("Falha no processamento",ex.getMessage());
   }

   @Test
   @DisplayName("Rejeitar arquivo inexistente")
   void testRejectMissingFile() {
      Path pdf = tempDir.resolve("inexistente.pdf");

      assertThrows(IOException.class,
                   () -> service.processPages(pdf.toFile(), _ -> {})
                  );
   }

   @Test
   @DisplayName("Rejeitar arquivo PDF inválido")
   void testRejectInvalidPdf() throws IOException {
      Path pdf = tempDir.resolve("invalido.pdf");

      Files.writeString(pdf,"Este arquivo não é um PDF válido.");

      assertThrows(IOException.class,
                   () -> service.processPages(pdf.toFile(), _ -> {})
                  );
   }

   /**
    * Cria um PDF temporário com uma página para cada texto informado.
    *
    * @param fileName nome do arquivo PDF
    * @param pageTexts textos das páginas
    * @return caminho do PDF criado
    */
   private Path createPdf(String fileName,String... pageTexts)
                throws IOException {

      Path pdf = tempDir.resolve(fileName);

      try (PDDocument document = new PDDocument()) {
         for (String text : pageTexts) {
            PDPage page = new PDPage();
            document.addPage(page);

            if (text.isBlank())
               continue;

            try (PDPageContentStream content =
                                       new PDPageContentStream(document,page)) {
               content.beginText();

               content.setFont(
                  new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                  12
               );

               content.newLineAtOffset(50,750);
               content.showText(text);
               content.endText();
            }
         }
         document.save(pdf.toFile());
      }
      return pdf;
   }
}
