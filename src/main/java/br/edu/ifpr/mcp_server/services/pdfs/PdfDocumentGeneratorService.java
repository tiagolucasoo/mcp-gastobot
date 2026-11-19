package br.edu.ifpr.mcp_server.services.pdfs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

public class PdfDocumentGeneratorService {
   private static final float MARGIN    = 50;
   private static final float FONT_SIZE = 11;
   // Espaçamento entre linhas
   private static final float LEADING   = 1.4f * FONT_SIZE;

   public void createPdfFromTexts(List<String> pagesText,
                                  File targetFile) throws IOException {
      try (PDDocument doc = new PDDocument()) {
         PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

         for (String pageText : pagesText)
            writePageContent(doc,font,pageText);

         doc.save(targetFile);
      }
   }

   private void writePageContent(PDDocument doc, PDFont font,
                                 String text) throws IOException {
      PDPage page = new PDPage(PDRectangle.A4);
      doc.addPage(page);

      float width    = page.getMediaBox().getWidth() - (2 * MARGIN);
      float startY   = page.getMediaBox().getHeight() - MARGIN;
      float currentY = startY;

      PDPageContentStream contentStream = new PDPageContentStream(doc,page);
      contentStream.beginText();
      contentStream.setFont(font,FONT_SIZE);
      contentStream.newLineAtOffset(MARGIN,startY);

      String[] lines = text.split("\r?\n");

      for (String line : lines) {
         List<String> wrappedLines = wrapLine(line,font,FONT_SIZE,width);

         for (String subLine : wrappedLines) {
            if (currentY - LEADING < MARGIN) {
               // fecha o fluxo atual e abre uma nova página se estourar a
               // margem inferior
               contentStream.endText();
               contentStream.close();

               page = new PDPage(PDRectangle.A4);
               doc.addPage(page);
               currentY = startY;

               contentStream = new PDPageContentStream(doc,page);
               contentStream.beginText();
               contentStream.setFont(font, FONT_SIZE);
               contentStream.newLineAtOffset(MARGIN, startY);
            }
            contentStream.showText(subLine);
            contentStream.newLineAtOffset(0,-LEADING);
            currentY -= LEADING;
         }
      }
      contentStream.endText();
      contentStream.close();
   }

   // garante que linhas compridas sejam quebradas de acordo com a largura
   // útil da página A4
   private List<String> wrapLine(String text, PDFont font, float fontSize,
                                 float width) throws IOException {
      List<String>  result      = new ArrayList<>();
      String[]      words       = text.split(" ");
      StringBuilder currentLine = new StringBuilder();

      for (String word : words) {
         String candidate = currentLine.isEmpty() ? word
                                                  : currentLine + " " + word;
         float  textWidth = font.getStringWidth(candidate) / 1000 * fontSize;

         if (textWidth > width) {
            result.add(currentLine.toString());
            currentLine = new StringBuilder(word);
         }
         else
            currentLine = new StringBuilder(candidate);
      }
      if (!currentLine.isEmpty())
         result.add(currentLine.toString());

      return result;
   }
}
