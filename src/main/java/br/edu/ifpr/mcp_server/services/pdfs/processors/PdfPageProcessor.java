package br.edu.ifpr.mcp_server.services.pdfs.processors;

import java.io.IOException;

/**
 * Define uma operação de processamento de uma página PDF.
 */
@FunctionalInterface
public interface PdfPageProcessor {
   /**
    * Processa uma página do documento.
    *
    * @param page página a ser processada
    * @throws IOException caso ocorra um erro durante o processamento
    */
   void process(PdfPage page) throws IOException;
}
