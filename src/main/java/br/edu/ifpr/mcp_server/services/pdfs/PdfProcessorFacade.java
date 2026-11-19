package br.edu.ifpr.mcp_server.services.pdfs;

import java.io.File;
import java.io.IOException;

import br.edu.ifpr.mcp_server.services.pdfs.model.ReplacementResult;
import br.edu.ifpr.mcp_server.services.pdfs.model.SearchOptions;
import br.edu.ifpr.mcp_server.services.pdfs.processors.PdfReplaceProcessor;

/**
 * Fachada responsável por coordenar a pesquisa e substituição
 * de conteúdo textual em documentos PDF.
 */
public class PdfProcessorFacade {

   private final PdfPageReaderService readerService;
   private final PdfDocumentGeneratorService generatorService;

   /**
    * Inicializa os serviços necessários ao processamento.
    */
   public PdfProcessorFacade() {
      this.readerService = new PdfPageReaderService();
      this.generatorService = new PdfDocumentGeneratorService();
   }

   /**
    * Pesquisa e substitui o texto em todas as páginas do PDF,
    * gerando um novo documento com o conteúdo processado.
    *
    * @param sourceFile arquivo PDF de origem
    * @param targetFile arquivo PDF de destino
    * @param options opções de pesquisa
    * @param replacementText texto de substituição
    * @return resultado da pesquisa e substituição
    * @throws IOException caso ocorra erro na leitura ou gravação
    */
   public ReplacementResult processAndReplace(File sourceFile, File targetFile,
                                              SearchOptions options,
                                              String replacementText)
                            throws IOException {

      PdfReplaceProcessor processor = new PdfReplaceProcessor(options,
                                                              replacementText);

      readerService.processPages(sourceFile,processor);

      generatorService.createPdfFromTexts(processor.getProcessedPages(),
                                          targetFile);

      return processor.getReplacementResult();
   }
}
