package br.edu.ifpr.mcp_server.services.pdfs;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import br.edu.ifpr.mcp_server.services.pdfs.processors.PdfPage;
import br.edu.ifpr.mcp_server.services.pdfs.processors.PdfPageProcessor;

public class PdfTextExtractorService {
   public void extractPagesText(String pdfFile, PdfPageProcessor processor)
               throws IOException {
      extractPagesText(new File(pdfFile),processor);
   }

   public void extractPagesText(File pdfFile, PdfPageProcessor processor)
               throws IOException {
      try (PDDocument doc = Loader.loadPDF(pdfFile)) {
         PDFTextStripper stripper   = new PDFTextStripper();
         int             totalPages = doc.getNumberOfPages();

         for (int i = 1; i <= totalPages; i++) {
            stripper.setStartPage(i);
            stripper.setEndPage(i);

            String text = stripper.getText(doc);
            processor.process(new PdfPage(i,text));
         }
      }
   }
}
