package br.edu.ifpr.mcp_server.services.pdfs;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import br.edu.ifpr.mcp_server.services.pdfs.processors.PdfPage;
import br.edu.ifpr.mcp_server.services.pdfs.processors.PdfPageProcessor;

/**
 * Serviço responsável pela leitura sequencial das páginas de um PDF.
 */
public class PdfPageReaderService {
   /**
    * Lê as páginas de um PDF e encaminha cada uma para processamento.
    *
    * @param pdfFile arquivo PDF de origem
    * @param processor responsável pelo processamento de cada página
    * @throws IOException caso ocorra um erro na leitura ou no processamento
    */
   public void processPages(File pdfFile, PdfPageProcessor processor)
               throws IOException {

      try (PDDocument doc = Loader.loadPDF(pdfFile)) {
         PDFTextStripper stripper = new PDFTextStripper();

         int totalPages = doc.getNumberOfPages();

         for (int i = 1; i <= totalPages; i++) {
            stripper.setStartPage(i);
            stripper.setEndPage(i);

            String text = stripper.getText(doc);

            processor.process(new PdfPage(i,text));
         }
      }
   }
}
