package br.edu.ifpr.mcp_server.services.pdfs;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PdfMetadataServiceTest {
   private static final Logger LOG =
                           LoggerFactory.getLogger(PdfMetadataServiceTest.class);

//   @ParameterizedTest(name="O arquivo {0} deve existir")
//   @ValueSource(strings={ "D:\\arquivo - SEM SENHA.pdf" })
//   final void testValidPdf(String fileName) throws Exception, IOException {
//      PdfMetadataService pcs = new PdfMetadataService(fileName);
//
//      assertNotNull(pcs);
//   }

//   @ParameterizedTest(name="O arquivo {0} deve existir")
//   @ValueSource(strings={ "D:\\arquivo - SEM SENHA.pdf" })
//   final void testValidPdfWithContent(String fileName)
//              throws Exception, IOException {
//      PdfMetadataService pcs = new PdfMetadataService(fileName);
//
//      String text = pcs.pdfContent();
//      assertTrue(text.length() > 0);
//      LOG.info(text);
//   }

   @ParameterizedTest(name="O arquivo {0} deve existir")
   @ValueSource(strings={ "D:\\arquivo - SEM SENHA.pdf" })
   final void testValidPdfWithMetadata(String fileName)
              throws Exception, IOException {
      PdfMetadataService pcs = new PdfMetadataService(fileName);

      String metadata = pcs.getMetadata();
      assertTrue(metadata.length() > 0);
      LOG.info(metadata);
   }

   @ParameterizedTest(name="Arquivo {0} inválido")
   @ValueSource(strings={ "xyz.pdf", "D:\\" })
   final void testInvalidPdf(String fileName) {
      assertThrows(IllegalArgumentException.class,
                                        () -> new PdfMetadataService(fileName));
   }

//   @ParameterizedTest
//   @ValueSource(strings={ "D:\\arquivo.pdf" })
//   final void testIllegalState(String fileName) {
//      assertThrows(IllegalStateException.class,
//                                        () -> new PdfMetadataService(fileName));
//   }
}
