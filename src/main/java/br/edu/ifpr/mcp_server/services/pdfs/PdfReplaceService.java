package br.edu.ifpr.mcp_server.services.pdfs;

import java.io.File;
import java.io.IOException;

import br.edu.ifpr.mcp_server.services.pdfs.model.ReplacementResult;
import br.edu.ifpr.mcp_server.services.pdfs.model.SearchOptions;
import br.edu.ifpr.mcp_server.services.pdfs.processors.PdfReplaceProcessor;

/**
 * Serviço responsável pela busca e substituição em documentos PDF.
 */
public class PdfReplaceService {
   private final PdfPageReaderService        readerService;
   private final PdfDocumentGeneratorService generatorService;

   public PdfReplaceService() {
      this.readerService    = new PdfPageReaderService();
      this.generatorService = new PdfDocumentGeneratorService();
   }

   /**
    * Substitui textos em um documento PDF e gera um novo documento.
    *
    * @param sourceFile documento de origem
    * @param targetFile documento de destino
    * @param options opções de busca
    * @param replacementText texto de substituição
    * @return resultado da substituição
    * @throws IOException caso ocorra um erro no processamento
    */
   public ReplacementResult replace(File sourceFile, File targetFile,
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
