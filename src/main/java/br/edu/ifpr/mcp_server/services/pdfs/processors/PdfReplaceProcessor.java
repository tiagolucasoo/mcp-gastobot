package br.edu.ifpr.mcp_server.services.pdfs.processors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import br.edu.ifpr.mcp_server.services.pdfs.model.ReplacementResult;
import br.edu.ifpr.mcp_server.services.pdfs.model.SearchOptions;

/**
 * Processa páginas PDF realizando busca e substituição de textos.
 */
public class PdfReplaceProcessor implements PdfPageProcessor {
   private final Pattern              pattern;
   private final String               replacementText;
   private final List<String>         processedPages = new ArrayList<>();
   private final Map<Integer,Integer> occurrencesPerPage = new HashMap<>();

   private int totalOccurrences;

   public PdfReplaceProcessor(SearchOptions options,String replacementText) {
      this.pattern         = options.toPattern();
      this.replacementText = replacementText;
   }

   @Override
   public void process(PdfPage page) {
      Matcher matcher = pattern.matcher(page.text());
      int     count   = 0;

      while (matcher.find())
         count++;

      if (count > 0) {
         occurrencesPerPage.put(page.pageNumber(),count);
         totalOccurrences += count;
      }
      String modifiedText = matcher.replaceAll(replacementText);

      processedPages.add(modifiedText);
   }

   public List<String> getProcessedPages() {
      return List.copyOf(processedPages);
   }

   public ReplacementResult getReplacementResult() {
      return new ReplacementResult(totalOccurrences,occurrencesPerPage);
   }
}
