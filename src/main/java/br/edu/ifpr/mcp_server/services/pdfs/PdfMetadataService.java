package br.edu.ifpr.mcp_server.services.pdfs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PdfMetadataService {
   private static final Logger LOG =
                              LoggerFactory.getLogger(PdfMetadataService.class);

   private String     fileName;
   private PDDocument doc;

   public PdfMetadataService(String fileName) {
      if (!Files.exists(Path.of(fileName),LinkOption.NOFOLLOW_LINKS))
         throw new IllegalArgumentException("Arquivo '" + fileName +
                                            "' não encontrado");

      if (!Files.isRegularFile(Path.of(fileName),LinkOption.NOFOLLOW_LINKS))
         throw new IllegalArgumentException("Arquivo '" + fileName +
                                            "' não é um arquivo regular");

      this.fileName = fileName;
      this.doc      = null;
   }

   public String getMetadata() throws Exception, IOException {
      if (doc == null) getDocument();

      PDDocumentInformation di = doc.getDocumentInformation();
      DateTimeFormatter     fmt =
                             DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
      String title    = di.getTitle() != null ? di.getTitle() : "";
      String subject  = di.getSubject() != null ? di.getSubject() : "";
      String author   = di.getAuthor() != null ? di.getAuthor() : "";
      String creator  = di.getCreator() != null ? di.getCreator() : "";
      String producer = di.getProducer() != null ? di.getProducer() : "";
      String keywords = di.getKeywords() != null ? di.getKeywords() : "";

      StringBuilder sb = new StringBuilder();
      sb.append("Título [").append(title).append("]\n")
        .append("Assunto [").append(subject).append("]\n")
        .append("Autor [").append(author).append("]\n")
        .append("Criador [").append(creator).append("]\n")
        .append("Produtor [").append(producer).append("]\n")
        .append("Palavras-chave [").append(keywords).append("]\n");

      Calendar      cal = di.getCreationDate();
      LocalDateTime ldt = LocalDateTime.ofInstant(cal.toInstant(),
                                                  cal.getTimeZone().toZoneId());
      sb.append("Criado em [").append(fmt.format(ldt)).append("]\n");

      cal = di.getCreationDate();
      sb.append("Modificado em [").append(fmt.format(ldt)).append("]\n");

      return sb.toString();
   }

//   public String pdfContent() throws Exception {
//      try {
//         if (doc == null) getDocument();
//
//         if (doc.isEncrypted())
//             throw new Exception("Arquivo '" + fileName +
//                                 "' protegido por senha");
//
//         return (new PDFTextStripper()).getText(doc);
//      }
//      catch (IOException ex) {
//         throw new Exception("Erro de acesso ao arquivo '" + fileName + "'");
//      }
//   }

   private void getDocument() throws Exception, IOException {
      try {
         doc = Loader.loadPDF(new RandomAccessReadBufferedFile(fileName));
         if (doc.isEncrypted())
            throw new Exception("Arquivo '" + fileName + 
                                "' protegido por senha");
       }
       catch (IOException ex) {
           throw new Exception("Erro de acesso ao arquivo '" + fileName + "'");
       }
    }
}
