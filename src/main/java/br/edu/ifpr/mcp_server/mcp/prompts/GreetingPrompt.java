package br.edu.ifpr.mcp_server.mcp.prompts;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.edu.ifpr.mcp_server.mcp.utils.McpConsts;
import br.edu.ifpr.mcp_server.mcp.utils.McpPromptDefinition;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncPromptSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.Prompt;
import io.modelcontextprotocol.spec.McpSchema.PromptArgument;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

public class GreetingPrompt implements McpPromptDefinition {
   private static final Logger LOG =
                                  LoggerFactory.getLogger(GreetingPrompt.class);

   private static final String DESCRIPTION = "Gera uma saudação " +
                                             "personalizada para uma pessoa.";

   @Override
   public void register(SyncSpecification<SingleSessionSyncSpecification> mcp)
               throws Exception {
      List<PromptArgument> list = List.of(
                                   PromptArgument.builder(
                                                   McpConsts.GREETING_ATTR_NOME)
                                                 .description("Nome da pessoa "+
                                                            "que será saudada.")
                                                 .required(true)
                                                 .build()
                                         );
      Prompt prompt = Prompt.builder(McpConsts.GREETING_PROMPT)
                            .description(DESCRIPTION)
                            .arguments(list)
                            .build();

      SyncPromptSpecification spec =
                                  new SyncPromptSpecification(prompt,this::run);

      mcp.prompts(spec);
   }

   @Override
   public GetPromptResult run(McpSyncServerExchange exchange,
                              GetPromptRequest request) {
      Map<String, Object> args = request.arguments();

      if (args == null || args.isEmpty() || 
          !(args.get(McpConsts.GREETING_ATTR_NOME) instanceof String nome) ||
          nome.trim().isEmpty())
         throw new IllegalArgumentException("O argumento '" +
                                            McpConsts.GREETING_ATTR_NOME +
                                            "' é obrigatório.");

      String        message       = String.format("Olá, %s! É um prazer " +
                                                  "recebê-lo. Como posso " +
                                                  "ajudar você hoje?",nome);
      TextContent   content       = TextContent.builder(message).build();
      PromptMessage promptMessage = PromptMessage.builder(Role.USER,content)
                                                 .build();

      return GetPromptResult.builder(List.of(promptMessage))
                            .description(DESCRIPTION)
                            .build();
   }
}
