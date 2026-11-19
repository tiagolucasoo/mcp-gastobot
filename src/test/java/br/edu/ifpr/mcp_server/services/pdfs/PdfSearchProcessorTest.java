package br.edu.ifpr.mcp_server.services.pdfs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import br.edu.ifpr.mcp_server.services.pdfs.model.SearchOptions;
import br.edu.ifpr.mcp_server.services.pdfs.model.SearchResult;
import br.edu.ifpr.mcp_server.services.pdfs.processors.PdfPage;
import br.edu.ifpr.mcp_server.services.pdfs.processors.PdfSearchProcessor;

class PdfSearchProcessorTest {
   @ParameterizedTest(name="search {0}, use_regex={1}, case_sensitive={2}")
   @CsvSource({
      "Java, false, true",
      "java, false, true",
      "JAVA, false, true"
   })
   final void testCountOccurrencesAccordingToSearchOptionsCsv(String search,
                                                              boolean use_regex,
                                                       boolean case_sensitive) {
	   SearchOptions      options   = new SearchOptions(search,use_regex,
                                                        case_sensitive);
	   PdfSearchProcessor processor = new PdfSearchProcessor(options);
	   PdfPage            page      = new PdfPage(1,"java Java JAVA");

	   processor.process(page);

	   SearchResult result = processor.getSearchResult();

       assertEquals(1,result.totalOccurrences());
       assertEquals(Map.of(1,1),result.occurrencesPerPage());
   }

   @ParameterizedTest(name="search {0}, use_regex={1}, case_sensitive={2}")
   @MethodSource("provideData")
   final void testCountOccurrencesAccordingToSearchOptions(String search,
                                                           boolean use_regex,
                                                       boolean case_sensitive) {
	   SearchOptions      options   = new SearchOptions(search,use_regex,
	                                                    case_sensitive);
	   PdfSearchProcessor processor = new PdfSearchProcessor(options);
	   PdfPage            page      = new PdfPage(1,"java Java JAVA");

	   processor.process(page);

	   SearchResult result = processor.getSearchResult();

       assertEquals(1,result.totalOccurrences());
       assertEquals(Map.of(1,1),result.occurrencesPerPage());
   }

   @ParameterizedTest(name="search {0}, use_regex={1}, case_sensitive={2}")
   @CsvSource({
      "JAVA, false, false"
   })
   final void testCountMultipleOccurrencesOnSamePage(String search,
                                                     boolean use_regex,
                                                     boolean case_sensitive) {
      SearchOptions      options   = new SearchOptions(search,use_regex,
                                                       case_sensitive);
      PdfSearchProcessor processor = new PdfSearchProcessor(options);
      PdfPage            page      = new PdfPage(1,"java Java JAVA");

      processor.process(page);

      SearchResult result = processor.getSearchResult();

      assertEquals(3,result.totalOccurrences());
      assertEquals(Map.of(1,3),result.occurrencesPerPage());
   }

   // método estático que fornece a massa de dados
   private static Stream<Arguments> provideData() {
      return Stream.of(Arguments.of("JAVA", false, true),
                       Arguments.of("java", false, true),
                       Arguments.of("Java", false, true)
                      );
   }
}
