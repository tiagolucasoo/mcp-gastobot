package br.edu.ifpr.mcp_server.services.moffice.word;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MSWordCounterService {
   private static final Logger LOG =
                            LoggerFactory.getLogger(MSWordCounterService.class);

   private String fileName;

   public MSWordCounterService(String fileName) {
      this.fileName = fileName;
   }

   public long countWords() throws IOException {
      long wordCount = 0L;

      try (XWPFDocument doc = new XWPFDocument(
                                   Files.newInputStream(Paths.get(fileName)))) {
         XWPFWordExtractor extractor = new XWPFWordExtractor(doc);
         String            text      = extractor.getText();

         extractor.close();
         wordCount = Arrays.stream(text.split("\\s+")).count();
      }
      return wordCount;
   }

   public long countParagraphs() throws IOException {
      long paragraphsCount = 0L;

      try (XWPFDocument doc = new XWPFDocument(
                                   Files.newInputStream(Paths.get(fileName)))) {
         List<XWPFParagraph> paragraphs = doc.getParagraphs();

         paragraphsCount = paragraphs.size();
      }
      return paragraphsCount;
   }
}
