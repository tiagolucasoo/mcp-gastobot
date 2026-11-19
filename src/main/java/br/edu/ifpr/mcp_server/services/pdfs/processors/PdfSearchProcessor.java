package br.edu.ifpr.mcp_server.services.pdfs.processors;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import br.edu.ifpr.mcp_server.services.pdfs.model.SearchOptions;
import br.edu.ifpr.mcp_server.services.pdfs.model.SearchResult;

/**
 * Processa páginas PDF contabilizando as ocorrências de um texto.
 */
public class PdfSearchProcessor implements PdfPageProcessor {
   private final Pattern              pattern;
   private final Map<Integer,Integer> occurrencesPerPage = new HashMap<>();

   private int totalOccurrences;

   public PdfSearchProcessor(SearchOptions options) {
      this.pattern = options.toPattern();
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
   }

   public SearchResult getSearchResult() {
      return new SearchResult(totalOccurrences,Map.copyOf(occurrencesPerPage));
   }
}
