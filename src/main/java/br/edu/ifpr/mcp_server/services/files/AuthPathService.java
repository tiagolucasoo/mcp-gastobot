package br.edu.ifpr.mcp_server.services.files;

import java.io.File;

public class AuthPathService {
   /**
    * Verifica se a propriedade 'mcp.files.root' ou a variável de ambiente
    * MCP_FILES_ROOT está disponível para ajustar o caminho autorizado para
    * operações pelo servidor MCP, caso contrário, lança exceção
    * @return
    */
   public static String authPath() {
      String propertyRoot    = System.getProperty("mcp.files.root");
      String environmentRoot = System.getenv("MCP_FILES_ROOT");
      String configuredRoot  = propertyRoot;

      if (configuredRoot == null || configuredRoot.trim().isEmpty())
         configuredRoot = environmentRoot;

      if (configuredRoot == null || configuredRoot.trim().isEmpty())
         throw new IllegalStateException("Configure a variável de ambiente " +
                                         "'MCP_FILES_ROOT' ou a propriedade " +
                                         "'mcp.files.root'.");

      if (configuredRoot.charAt(configuredRoot.length() - 1) !=
    			                                                 File.separatorChar)
         configuredRoot = configuredRoot + File.separator;

      return configuredRoot;
   }
}
