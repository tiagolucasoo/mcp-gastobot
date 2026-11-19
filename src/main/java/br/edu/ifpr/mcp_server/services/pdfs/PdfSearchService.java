package br.edu.ifpr.mcp_server.services.pdfs;

import java.io.File;
import java.io.IOException;

import br.edu.ifpr.mcp_server.services.pdfs.model.SearchOptions;
import br.edu.ifpr.mcp_server.services.pdfs.model.SearchResult;
import br.edu.ifpr.mcp_server.services.pdfs.processors.PdfSearchProcessor;

/**
 * Serviço responsável pela busca de textos em documentos PDF.
 */
public class PdfSearchService {
   private final PdfPageReaderService readerService;

   public PdfSearchService() {
      this.readerService = new PdfPageReaderService();
   }

   /**
    * Pesquisa um texto em todas as páginas de um documento PDF.
    *
    * @param sourceFile documento PDF de origem
    * @param options opções de busca
    * @return resultado da pesquisa
    * @throws IOException caso ocorra um erro na leitura do documento
    */
   public SearchResult search(File sourceFile,SearchOptions options)
                       throws IOException {

      PdfSearchProcessor processor = new PdfSearchProcessor(options);

      readerService.processPages(sourceFile,processor);

      return processor.getSearchResult();
   }
}
