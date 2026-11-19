package br.edu.ifpr.mcp_server.utils.json.schema;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PathParser {

   public record PathComponents(
      String root,
      String parentDirectory,
      String fileName,
      String baseName,
      String extension
   ) {}

   public static PathComponents parse(String pathString) {
      Path path = Paths.get(pathString);

      // unidade / Drive / Raiz (ex: "C:\" no Windows ou "/" no Linux)
      Path   rootPath = path.getRoot();
      String root     = (rootPath != null) ? rootPath.toString() : "";

      // diretório pai / caminho dos diretórios
      Path   parentPath      = path.getParent();
      String parentDirectory = (parentPath != null) ? parentPath.toString()
                                                    : "";

      // nome completo do arquivo (ex: "documento.pdf")
      Path fileNamePath = path.getFileName();
      String fileName   = (fileNamePath != null) ? fileNamePath.toString() : "";

      // separação do Nome Base e da Extensão
      String baseName  = fileName;
      String extension = "";

      if (fileName != null && !fileName.isEmpty()) {
         int lastDotIndex = fileName.lastIndexOf('.');
         // garante que o ponto não seja o primeiro caractere (arquivos ocultos
         // como .gitignore)
         if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            baseName  = fileName.substring(0, lastDotIndex);
            extension = fileName.substring(lastDotIndex + 1);
         }
      }
      return new PathComponents(root,parentDirectory,fileName,baseName,
                                extension);
   }

   public static void main(String[] args) {
      // exemplo Windows
      String         winPath       = "C:\\projetos\\mcp-server\\schemas\\" +
                                     "relatorio.schema.json";
      PathComponents winComponents = parse(winPath);

      System.out.println("=== Exemplo Windows ===");
      System.out.println("Drive/Root: " + winComponents.root());
      System.out.println("Diretórios: " + winComponents.parentDirectory());
      System.out.println("Arquivo:    " + winComponents.fileName());
      System.out.println("Nome Base:  " + winComponents.baseName());
      System.out.println("Extensão:   " + winComponents.extension());

      System.out.println();

      // exemplo Linux / POSIX
      String linuxPath = "/var/log/app/servidor.log";
      PathComponents linuxComponents = parse(linuxPath);

      System.out.println("=== Exemplo Linux ===");
      System.out.println("Root:       " + linuxComponents.root());
      System.out.println("Diretórios: " + linuxComponents.parentDirectory());
      System.out.println("Arquivo:    " + linuxComponents.fileName());
      System.out.println("Nome Base:  " + linuxComponents.baseName());
      System.out.println("Extensão:   " + linuxComponents.extension());
   }
}
