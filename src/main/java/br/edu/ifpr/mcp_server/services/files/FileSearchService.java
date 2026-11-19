package br.edu.ifpr.mcp_server.services.files;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pesquisa arquivos dentro de um diretório autorizado.
 */
public final class FileSearchService {
   private static final Logger LOG =
                               LoggerFactory.getLogger(FileSearchService.class);
   // constantes para limites do serviço:
   // número máximo de resultados da busca
   private static final int MAX_RESULTS_LIMIT   = 100;
   // entradas pesquisadas
   private static final int MAX_ENTRIES_SCANNED = 20_000;
   // profundidade da pesquisa
   private static final int MAX_DEPTH           = 32;
   // indica o caminho raiz a partir do qual as buscas serão realizadas
   private final Path root;

   /**
    * Cria o serviço a partir de uma raiz existente.
    *
    * @param root diretório autorizado
    * @throws IOException se a raiz não puder ser resolvida
    */
   public FileSearchService(Path root) throws Exception {
      if (root == null)
         throw new IllegalArgumentException("Caminho inválido.");

      try { this.root = root.toRealPath(); }
      catch (IOException ex) {
         throw new IllegalArgumentException(ex.getMessage());
      }

      if (!Files.isDirectory(this.root))
         throw new IllegalArgumentException(
                    String.format("Caminho não é um diretório [%s].",
                                  root.getFileName().toString()));
   }

   /**
    * Cria o serviço usando a configuração do processo (método alternativo ao
    * construtor).
    * 
    * @return
    * @throws IOException
    */
   public static FileSearchService fromConfiguration() throws Exception {
//      String mcpFilesRootProp = System.getProperty("mcp.files.root");
//      String mcpFilesRootVar  = System.getenv("MCP_FILES_ROOT");

//      return fromConfiguration(mcpFilesRootProp,mcpFilesRootVar);
      return fromConfiguration("","");
   }

   /**
    * Usado pelo método acima e para testes unitários.
    * 
    * @param propertyRoot
    * @param environmentRoot
    * @return
    * @throws IOException
    */
   static FileSearchService fromConfiguration(String propertyRoot,
                                              String environmentRoot)
                            throws Exception {
/*
      // procura pela propriedade (opção -D na linha de comando)....
      String configuredRoot = System.getProperty("mcp.files.root");
      // ....se não encontrou, procura pela variável de ambiente (ajustada antes
      // da execução da aplicação
      LOG.debug("Propriedade 'mcp.files.root' [{}]",configuredRoot);
      if (configuredRoot == null || configuredRoot.isBlank()) {
         configuredRoot = System.getenv("MCP_FILES_ROOT");
         LOG.debug("Variável de ambiente 'MCP_FILES_ROOT' [{}]",configuredRoot);
      }
      if (configuredRoot == null || configuredRoot.isBlank())
         // se ambas a propriedade e a variável de ambiente não forem
         // encontradas, a aplicação NÃO pode continuar
         throw new IllegalStateException("Configure a variável de ambiente " +
                                         "'MCP_FILES_ROOT' ou a propriedade " +
                                         "'mcp.files.root'.");
*/
      String configuredRoot = AuthPathService.authPath();
//      String configuredRoot = propertyRoot;
//
//      if (configuredRoot == null || configuredRoot.isBlank())
//         configuredRoot = environmentRoot;
//
//      if (configuredRoot == null || configuredRoot.isBlank())
//         throw new IllegalStateException("Configure a variável de ambiente " +
//                                         "'MCP_FILES_ROOT' ou a propriedade " +
//                                         "'mcp.files.root'.");

      return new FileSearchService(Path.of(configuredRoot));
   }

   // Retorna o caminho real do diretório autorizado
//   public Path getRoot() { return root; }

   /**
    * Pesquisa arquivos pelo nome, usando um padrão glob (ou globbing), que é
    * uma técnica de procura usando caracteres curinga (wildcards):
    * Caracteres usados do padrão glob:
    * * (asterisco): exemplo: *.pdf - encontra qualquer arquivo com a extensão
    *                pdf;
    * ? (interrogação): receita?.txt: corresponde a exatamente um caractere,
    *                   receita1.txt ou receita2.txt, mas não receita10.txt;
    * ** (asterisco duplo): busca recursivamente em diretórios e subdiretórios,
    *                       src/(asterisco duplo)/.txt: busca arquivos texto no
    *                       diretório "src" e em seus subdiretórios;
    * [...] (colchetes): cria um intervalo de caracteres, p.ex. receita[0-9].txt
    *                    encontra receita1.txt ou receita0.txt ou receita9.txt;
    * {...} (chaves): lista múltiplas opções ou extensões, p.ex. *.{java,txt}
    *
    * @param pattern padrão do nome do arquivo
    * @param recursive indica se a busca será recursiva
    * @param maxResults limite de resultados
    * @return relatório textual da busca
    * @throws IOException se ocorrer uma falha de leitura
    */
   public String search(String pattern, boolean recursive, int maxResults)
                 throws Exception {
      // valida os parâmetros 'pattern' e 'maxResults'
      validateArguments(pattern,maxResults);
      // serve para converter todo o texto de uma variável (pattern) para letras
      // minúsculas de forma segura e independente do idioma configurado no
      // computador do usuário.
      // Local.ROOT define um "idioma neutro" (baseado no padrão Unicode), isso
      // garante que a conversão funcione exatamente da mesma maneira em
      // qualquer lugar do mundo.
      String normalizedPattern = pattern.toLowerCase(Locale.ROOT);
      // cria o objeto para realizar a combinação entre o padrão (pattern)
      // recebido com o resultado da busca
      PathMatcher matcher = FileSystems.getDefault()
                                       .getPathMatcher("glob:" +
                                                       normalizedPattern);
      // 
      int        maxDepth = recursive ? MAX_DEPTH : 1;
      List<Path> entries;
      // percorre o diretório (propriedade ou variável de ambiente) e recupera
      // MAX_ENTRIES_SCANNED entradas
      try (Stream<Path> paths = Files.walk(root,maxDepth)) {
         entries = paths.limit(MAX_ENTRIES_SCANNED + 1L).toList();
      }
      // verifica se a busca superou a quantidade de entradas
      boolean scanLimitReached = entries.size() > MAX_ENTRIES_SCANNED;
      // se a quantidade de entradas foi superada, "pega" a quantidade definida
      if (scanLimitReached)
         entries = entries.subList(0,MAX_ENTRIES_SCANNED);
      // armazena o resultado da busca
      List<String> results            = new ArrayList<>();
      boolean      resultLimitReached = false;
      // percorre as entradas buscadas
      for (Path path : entries) {
         // se uma entrada é igual ao caminho raiz da inicialização desta classe
         // ou a entrada NÃO é um arquivo regular (arquivo com dados e não um
         // diretório ou arquivo especial), passa à próxima entrada
         if (path.equals(root) ||
             !Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS))
            continue;

         // APENAS PARA VERIFICAÇÃO (1)
         String originalName = path.getFileName().toString();
         LOG.debug("NOME ORIGINAL [{}]",originalName);

         // converte o nome do arquivo para minúsculo
         String fileName = originalName.toLowerCase(Locale.ROOT);
//         String fileName = path.getFileName()
//                               .toString().toLowerCase(Locale.ROOT);

         // APENAS PARA VERIFICAÇÃO (2)
         LOG.debug("NOME NORMALIZADO [{}]",fileName);

         // se o arquivo encontrado NÃO corresponde ao padrão de busca, passa
         // à próxima entrada
         if (!matcher.matches(Path.of(fileName)))
            continue;
         // se o limite de resultados foi alcançado, encerra a procura 
         if (results.size() == maxResults) {
            resultLimitReached = true;
            break;
         }
         // prepara o texto para cada entrada (arquivo) encontrada
         try {
            long    size         = Files.size(path);
            Instant modified     = Files.getLastModifiedTime(path).toInstant();
            String  relativePath = root.relativize(path).toString();

            // APENAS PARA VERIFICAÇÃO (3)
            LOG.debug("CAMINHO RELATIVO [{}]",relativePath);

            results.add(String.format("%s | %d bytes | modificado em %s",
                                      relativePath,size,modified));
         }
         catch (IOException e) {
//            LOG.debug("Não foi possível recuperar os metadados o arquivo [{}]",
//                      path,e);
            // APENAS PARA VERIFICAÇÃO (4)
            LOG.error("Falha ao processar entrada [{}]",path,e);
            throw e;
         }
      }
      StringBuilder report = new StringBuilder();
      // dados do resultado da busca
      report.append("Padrão: ").append(pattern).append('\n')
            .append("Busca recursiva: ")
            .append(recursive ? "sim" : "não").append('\n')
            .append("Resultados encontrados: ")
            .append(results.size()).append('\n');

      if (results.isEmpty())
         report.append("\nNenhum arquivo encontrado.\n");
      else
         // acrescenta cada entrada (arquivo) recuperada ao resultado final
         for (String result : results)
            report.append("- ").append(result).append('\n');
      // verifica se o limite de busca foi atingido
      if (resultLimitReached)
         report.append("\nLimite de resultados atingido; podem existir mais " +
                       "ocorrências.\n");

      if (scanLimitReached)
         report.append("\nLimite de entradas examinadas atingido; a busca " +
                       "pode estar incompleta.\n");

      return report.toString();
   }

   private void validateArguments(String pattern, int maxResults) {
      if (pattern == null || pattern.isBlank())
         throw new IllegalArgumentException(
                                       "O padrão de busca não pode ser vazio.");

      if (pattern.contains("/") || pattern.contains("\\"))
         throw new IllegalArgumentException(
                           "Informe apenas um padrão de nome, sem diretórios.");

      if (maxResults < 1 || maxResults > MAX_RESULTS_LIMIT)
         throw new IllegalArgumentException("'maxResults' deve estar entre 1 e "
                                            + MAX_RESULTS_LIMIT + ".");
   }
}
