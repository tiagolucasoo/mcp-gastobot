package br.edu.ifpr.mcp_server.mcp.resources;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import br.edu.ifpr.mcp_server.mcp.utils.McpConsts;
import br.edu.ifpr.mcp_server.mcp.utils.McpResourceDefinition;
import br.edu.ifpr.mcp_server.services.files.AuthPathService;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpServerFeatures
      .SyncResourceSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.Resource;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;

public final class RootInfoResource implements McpResourceDefinition {
//   private final FileSearchService service;

//   public RootInfoResource(FileSearchService service) {
//      this.service = service;
//   }

   @Override
   public void register(SyncSpecification<SingleSessionSyncSpecification> mcp)
               throws Exception {
      Resource resource = Resource.builder(McpConsts.RESOURCE_ROOT_INFO_URI,
                                           McpConsts.RESOURCE_ROOT_INFO_NAME)
                                  .title("Informações da raiz de arquivos")
                                  .description("Informa o estado do " +
                                               "diretório autorizado.")
                                  .mimeType("text/plain")
                                  .build();

      SyncResourceSpecification spec = new SyncResourceSpecification(resource,
                                                                    this::read);

      mcp.resources(spec);
   }

   @Override
   public ReadResourceResult read(McpSyncServerExchange exchange,
                                  ReadResourceRequest request) {
//      Path   root       = service.getRoot();
      Path   root       = Path.of(AuthPathService.authPath());
      String isDir      = Files.isDirectory(root) ? "Sim" : "Não";
      String isReadable = Files.isReadable(root) ? "Sim" : "Não";
      String content    = """
                          Diretório autorizado '%s'\n
                          É diretório %s\n
                          Habilitado para leitura %s\n
                          """.formatted(root.toString(),isDir,isReadable);

      TextResourceContents text =
                    TextResourceContents.builder(
                                               McpConsts.RESOURCE_ROOT_INFO_URI,
                                                 content)
                                        .mimeType("text/plain")
                                        .build();

      return ReadResourceResult.builder(List.of(text)).build();
   }
}
