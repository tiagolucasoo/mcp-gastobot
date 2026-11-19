package br.edu.ifpr.mcp_server.main;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.ListPromptsResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import io.modelcontextprotocol.spec.McpSchema.LoggingLevel;
import io.modelcontextprotocol.spec.McpSchema.LoggingMessageNotification;
import io.modelcontextprotocol.spec.McpSchema.Prompt;
import io.modelcontextprotocol.spec.McpSchema.PromptArgument;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.Resource;
import io.modelcontextprotocol.spec.McpSchema.ResourceContents;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import tools.jackson.databind.json.JsonMapper;

public class McpClient01SyncStdio {
   private static final Logger LOG =
                            LoggerFactory.getLogger(McpClient01SyncStdio.class);
   // nome e caminho para o servidor MCP
   private static final String SERVER_PATH =
                                         "../../server/mcp_server01_server.jar";
   // objeto cliente para comunicação com o servidor
   private final McpSyncClient client;

   // será invocado (chamado) pelo servidor (é a forma que o servidor tem para
   // "conversar" com o cliente)
   private void loggingMessageNotification(
                                      LoggingMessageNotification notification) {
      LOG.info("[NOTIFICAÇÃO MCP] Origem: {} | Nível: {} | Mensagem: {}", 
               notification.logger(), 
               notification.level(), 
               notification.data());
   }

   private McpSyncClient createClient(String root, boolean isProperty) {
      // identifica o servidor a ser chamado/executado
      File serverJar = new File(SERVER_PATH);
      // se o objeto File não é um arquivo: erro!!!
      if (!serverJar.isFile())
         throw new IllegalStateException("JAR do servidor não encontrado: " +
		                                 serverJar.getAbsolutePath());
      // caminho absoluto do programa executável do servidor
      String jarPath = serverJar.getAbsolutePath();
      LOG.info("Servidor MCP: {}",jarPath);
      // comando para executar a aplicação servidor
      ServerParameters.Builder serverParams  = ServerParameters.builder("java");
      // se o diretório autorizado foi passado como propriedade da JVM, então
      // a execução do servidor seguirá o mesmo formato....
      if (isProperty)
         serverParams = serverParams.args("-D%s=%s".formatted(
                                                             AppConsts.AUTH_DIR_PROP,
                                                              root),
                                          "-jar",jarPath);
      // ....caso contrário, o servidor MCP Java será executado com o valor do
      // diretório autorizado como variável de ambiente
      else
         serverParams = serverParams.args("-jar",jarPath)
                                    .addEnvVar(AppConsts.AUTH_DIR_VAR,root);

      ServerParameters params = serverParams.build();

      // cria o objeto para comunicação entre cliente e servidor
      JacksonMcpJsonMapper jsonMapper = new JacksonMcpJsonMapper(
                                                              new JsonMapper());
      // estabelece que a comunicação usará o transporte STDIO
      StdioClientTransport   transport  = new StdioClientTransport(params,
                                                                   jsonMapper);
      // captura a comunicação entre o cliente e o servidor
      transport.setStdErrorHandler(line -> LOG.info("[SERVER STDERR] {}",line));
      // constrói o objeto cliente
      return McpClient.sync(transport)
                      // CONFIGURAÇÃO DO LISTENER DE LOGGING:
                      // Registra o handler (tratador) para capturar as
                      // notificações enviadas pelo servidor
                      .loggingConsumer(this::loggingMessageNotification)
                      .build();
   }

   private void close() {
      LOG.info("Fechando cliente MCP...");
      // encerra o cliente
      client.closeGracefully();
      LOG.info("Cliente MCP fechado.");
   }

   private void execute() {
      // inicializa o cliente com o transporte STDIO para o servidor
      client.initialize();
      // define o nível mínimo de "logging" que o cliente quer receber do
      // servidor
      client.setLoggingLevel(LoggingLevel.INFO);

      // executa o 'looping' para:
      // - listar ferramentas
      // - selecionar ferramenta
      // - realizar entrada de dados da ferramenta, se houver
      // - executar ferramenta
//      listSelectRunToolLooping();

      // os parâmetros para a(s) ferramenta(s) serão lidos da entrada padrão
      Scanner scanner = new Scanner(System.in);
      showMenu(scanner);
      scanner.close();
   }

   private ListToolsResult loadTools() {
      ListToolsResult result = client.listTools();

      LOG.info("Servidor disponibilizou {} ferramenta(s).",
               result.tools().size());

      return result;
   }

   private ListPromptsResult loadPrompts() {
      ListPromptsResult result = client.listPrompts();

      LOG.info("Servidor disponibilizou {} prompt(s).",result.prompts().size());

      return result;
   }

   private void showMenu(Scanner scanner) {
      while (true) {
         // exibe as ferramentas e opções ao usuário
         System.out.println("\n");
         System.out.println("========================================");
         System.out.println(" CLIENTE MCP");
         System.out.println("========================================");
         System.out.println("1 - Ferramentas (Tools)");
         System.out.println("2 - Recursos (Resources)");
         System.out.println("3 - Prompts");
         System.out.println("\n0 - Sair");
         System.out.print("\nOpção: ");

         if (!scanner.hasNextLine())
             continue;

         String option = scanner.nextLine().trim().toLowerCase();
         if (option.isBlank())
             continue;

         switch (option) {
            case "1" -> executeToolMenu(scanner);
            case "2" -> showResourceMenu(scanner);
            case "3" -> showPromptMenu(scanner);
            case "0",
                 "sair" -> { return; }
            default -> System.out.println("==> Opção inválida <==");
         }
      }
   }

   private void executeToolMenu(Scanner scanner) {
      ListToolsResult tools = loadTools();

      if (tools.tools().isEmpty()) {
         System.out.println("Nenhuma ferramenta disponibilizada.");
         return;
      }
      while (true) {
         System.out.println();
         System.out.println("\n=== Ferramentas disponíveis ===");
         for (int i = 0; i < tools.tools().size(); i++) {
            Tool tool = tools.tools().get(i);
   
            System.out.printf(" %d - %s%n",i + 1,tool.name());
            if (tool.description() != null && !tool.description().isBlank())
               System.out.printf(" %s%n", tool.description());
         }
         System.out.println("\n0 - Voltar");
         System.out.print("Selecione uma ferramenta: ");

         if (!scanner.hasNextLine())
             continue;

         String command = scanner.nextLine().trim();

         if ("0".equals(command) || "voltar".equalsIgnoreCase(command))
            return;

         Tool selectedTool = selectTool(tools.tools(),command);
         // se a ferramenta não foi encontrada, reinicia o 'looping'
         if (selectedTool == null) {
            LOG.warn("Ferramenta não encontrada: {}",command);
            continue;
         }
         executeTool(selectedTool,scanner);
      }
   }

   private Tool selectTool(List<Tool> tools, String command) {
      try {
         int index = Integer.parseInt(command) - 1;

         if (index >= 0 && index < tools.size())
            return tools.get(index);
      }
      catch (NumberFormatException ignored) {
         // o comando pode ser o nome da ferramenta
      }
      // tenta encontrar a ferramenta solicitada pelo usuário
      for (Tool tool : tools)
         if (tool.name().equalsIgnoreCase(command))
            return tool;
      // se não encontra a ferramenta
      return null;
   }

   private void executeTool(Tool tool, Scanner scanner) {
      System.out.println();
      System.out.println("----------------------------------------");
      System.out.println("Ferramenta: " + tool.name());

      if (tool.description() != null)
         System.out.println("Descrição: " + tool.description());

      Map<String, Object> arguments = readArguments(tool,scanner);
      CallToolRequest     request   = CallToolRequest.builder(tool.name())
    			                                     .arguments(arguments)
    			                                     .build();
      LOG.info("Executando ferramenta '{}'",tool.name());
      try {
         CallToolResult result = client.callTool(request);

         showResult(result);
      }
      catch (Exception ex) {
         LOG.error("Erro executando ferramenta '{}'",tool.name(),ex);
      }
   }

   private Map<String, Object> readArguments(Tool tool, Scanner scanner) {
      Map<String, Object> arguments = new LinkedHashMap<>();
      // recupera os esquema JSON, que define os parâmetros da ferramenta, para
      // solicitar os valores ao usuário
      Map<String, Object> schema    = tool.inputSchema();
      // se o esquema não existe ou está vazio, retorna uma lista vazia
      if (schema == null || schema.isEmpty())
         return arguments;
      // recupera as propriedades do esquema da ferramenta
      // ver método register() da classe SimpleReportTool
      Object propertiesObject = schema.get("properties");

      if (!(propertiesObject instanceof Map<?, ?>))
         return arguments;

      Map<?, ?> properties = (Map<?, ?>) propertiesObject;
      List<?>   required   = getRequiredProperties(schema);

      if (properties.isEmpty())
         return arguments;

      System.out.println();
      System.out.println("Informe os parâmetros:");
      for (Map.Entry<?, ?> entry : properties.entrySet()) {
         String name = String.valueOf(entry.getKey());

         if (!(entry.getValue() instanceof Map<?, ?>))
            continue;
         // recupera o esquema (a descrição dos parâmetros) da ferramenta
         Map<?, ?> property    = (Map<?, ?>) entry.getValue();
         String    type        = getStringValue(property,"type","string");
         String    description = getStringValue(property,"description","");
         boolean   isRequired  = required.contains(name);
         Object    value       = readArgument(scanner,name,type,description,
        			                               isRequired,property);

         if (value != null)
            arguments.put(name,value);
      }
      return arguments;
   }

   private String getStringValue(Map<? ,?> map, String key, 
                                 String defaultValue) {
      Object value = map.get(key);

      return value != null ? String.valueOf(value) : defaultValue;
   }

   private List<?> getRequiredProperties(Map<String, Object> schema) {
      Object required = schema.get("required");
      // verifica se há uma ou mais propriedades no esquema JSON que são
      // obrigatórias
      if (required instanceof List<?>)
         return (List<?>) required;
      // retorna uma lista vazia
      return List.of();
   }

   private Object readArgument(Scanner scanner, String name, String type,
			                      String description, boolean required,
			                      Map<?, ?> schema) {
      while (true) {
         System.out.println();
         System.out.println("'" + name + "' [" + type + "] " +
                            (required ? "(obrigatório)" : "(opcional)"));
         System.out.println("Descrição: " + description);
         System.out.print("Valor: ");

         if (!scanner.hasNextLine())
            return null;

         String value = scanner.nextLine().trim();
         if (value.isBlank()) {
            if (!required) return null;

            System.out.println("Este parâmetro é obrigatório.");
            continue;
         }
         try { return convertValue(value,type,schema); }
         catch (IllegalArgumentException ex) {
            System.out.println("Valor inválido: " + ex.getMessage());
         }
      }
   }

   private Object convertValue(String value, String type, Map<?, ?> schema) {
      return switch (type) {
    	        case "integer" -> Integer.valueOf(value);
    	        case "number" -> Double.valueOf(value);
    	        case "boolean" -> parseBoolean(value);
    	        case "string" -> value;
    	        default -> parseComplexValue(value,type,schema);
             };
   }

   private Boolean parseBoolean(String value) {
      if ("true".equalsIgnoreCase(value) ||
          "yes".equalsIgnoreCase(value) ||
          "sim".equalsIgnoreCase(value) ||
          "on".equalsIgnoreCase(value)
         )
         return true;

      if ("false".equalsIgnoreCase(value) ||
          "no".equalsIgnoreCase(value) ||
          "não".equalsIgnoreCase(value) ||
          "nao".equalsIgnoreCase(value) ||
          "off".equalsIgnoreCase(value)
         )
         return false;

      throw new IllegalArgumentException("Informe true/false ou sim/não");
   }

   private Object parseComplexValue(String value, String type,
			                           Map<?, ?> schema) {
      if ("null".equals(type))
         return null;

      throw new IllegalArgumentException("Tipo não suportado pelo modo " +
                                         "interativo: " + type);
   }

   private void showResult(CallToolResult result) {
      System.out.println();
      System.out.println("=============== RESULTADO ===============");
      System.out.println("   Erro: " + (result.isError() ? "Sim" : "Não"));
      System.out.println("Content:" );
         result.content().forEach(content -> System.out.println(" " + content));

      if (result.structuredContent() != null) {
         System.out.println();
         System.out.println("Structured Content:");
         System.out.println(result.structuredContent());
      }
      System.out.println( "==========================================" );
   }

   private void showPromptResult(GetPromptResult result) {
      System.out.println();
      System.out.println("=============== PROMPT ===============");

      if (result.description() != null)
         System.out.println("Descrição: " + result.description());

      for (PromptMessage message : result.messages()) {
         System.out.println();
         System.out.println("Papel: " + message.role());

         if (message.content() instanceof TextContent content)
            System.out.println(content.text());
         else
            System.out.println(message.content());
      }
      System.out.println("======================================");
   }

   private void showResourceMenu(Scanner scanner) {
      List<Resource> resources = client.listResources().resources();

      if (resources.isEmpty()) {
         System.out.println("Nenhum recurso disponível.");
         return;
      }
      while (true) {
         System.out.println();
         System.out.println("\n=== Recursos disponíveis ===");
         for (int i = 0; i < resources.size(); i++) {
            Resource resource = resources.get(i);

            System.out.printf("%d - %s [%s]%n",
                              i + 1,resource.name(),resource.uri());
         }
         System.out.println("\n0 - Voltar");
         System.out.print("Selecione um recurso: ");
         if (!scanner.hasNextLine())
             continue;

         String option = scanner.nextLine().trim();
         if (option.isBlank())
            continue;

         if ("0".equals(option) || "voltar".equalsIgnoreCase(option))
            return;

         int index;
         try { index = Integer.parseInt(option) - 1; }
         catch (NumberFormatException e) {
            System.out.println("Informe um número válido.");
            continue;
         }
         if (index < 0 || index >= resources.size()) {
            System.out.println("Opção inválida.");
            continue;
         }
         readResource(resources.get(index));
      }
   }

   private void showPromptMenu(Scanner scanner) {
      ListPromptsResult result  = loadPrompts();
      List<Prompt>      prompts = result.prompts();
   
      if (prompts.isEmpty()) {
         System.out.println("Nenhum prompt disponibilizado.");
         return;
      }
      while (true) {
         System.out.println("\n=== Prompts disponíveis ===");
   
         for (int i = 0; i < prompts.size(); i++) {
            Prompt prompt = prompts.get(i);
   
            System.out.printf("%d - %s%n",i + 1,prompt.name());
   
            if (prompt.description() != null && !prompt.description().isBlank())
               System.out.println(" " + prompt.description());
   
            System.out.printf(" Argumentos: %d%n",prompt.arguments() == null
                                                  ? 0
                                                  : prompt.arguments().size());
         }
         System.out.println("\n0 - Voltar");
         System.out.print("Selecione um prompt: ");

         if (!scanner.hasNextLine())
            return;

         String option = scanner.nextLine().trim();

         if ("0".equals(option) || "voltar".equalsIgnoreCase(option))
            return;

         Prompt selectedPrompt = selectPrompt(prompts,option);

         if (selectedPrompt == null) {
            System.out.println("Prompt não encontrado.");
            continue;
         }
         executePrompt(selectedPrompt,scanner);
      }
   }

   private Prompt selectPrompt(List<Prompt> prompts, String command) {
      try {
         int index = Integer.parseInt(command) - 1;

         if (index >= 0 && index < prompts.size())
            return prompts.get(index);
      }
      catch (NumberFormatException ignored) {
         // O comando pode ser o nome do prompt.
      }

      for (Prompt prompt : prompts)
         if (prompt.name().equalsIgnoreCase(command))
            return prompt;

      return null;
   }

   private void executePrompt(Prompt prompt, Scanner scanner) {
      System.out.println();
      System.out.println("----------------------------------------");
      System.out.println("Prompt: " + prompt.name());

      if (prompt.description() != null)
         System.out.println("Descrição: " + prompt.description());

      Map<String,Object> arguments = readPromptArguments(prompt,scanner);

      GetPromptRequest request = GetPromptRequest.builder(prompt.name())
                                                 .arguments(arguments)
                                                 .build();

      LOG.info("Obtendo prompt '{}'",prompt.name());

      try {
         GetPromptResult result = client.getPrompt(request);

         showPromptResult(result);
      }
      catch (Exception ex) {
         LOG.error("Erro obtendo prompt '{}'",prompt.name(),ex);
      }
   }

   private Map<String,Object> readPromptArguments(Prompt prompt,
                                                  Scanner scanner) {
      Map<String,Object> arguments = new LinkedHashMap<>();

      if (prompt.arguments() == null || prompt.arguments().isEmpty())
         return arguments;

      System.out.println("\nInforme os argumentos:");
      for (PromptArgument argument : prompt.arguments()) {
         boolean required = Boolean.TRUE.equals(argument.required());

         while (true) {
            System.out.println();
            System.out.println("'" + argument.name() + "'" +
                               (required ? " (obrigatório)" : " (opcional)"));

            if (argument.description() != null)
               System.out.println("Descrição: " + argument.description());

            System.out.print("Valor: ");
            if (!scanner.hasNextLine())
               return arguments;

            String value = scanner.nextLine().trim();
             if (value.isBlank()) {
                if (!required)
                   break;

	            System.out.println("Este argumento é obrigatório.");
	            continue;
	         }
	         arguments.put(argument.name(),value);
	         break;
         }
      }
      return arguments;
   }

   private void readResource(Resource resource) {
      ReadResourceRequest request = ReadResourceRequest.builder(resource.uri())
                                                       .build();

      ReadResourceResult  result  = client.readResource(request);

      System.out.println("\n=== " + resource.name() + " ===");
      for (ResourceContents content : result.contents()) {
         if (content instanceof TextResourceContents textContent)
            System.out.println(textContent.text());
         else
            System.out.println("Conteúdo binário não suportado neste menu.");
      }
   }

   public McpClient01SyncStdio(String root, boolean isProperty) {
      this.client = createClient(root,isProperty);
   }

   public static void main(String... args) {
      // para um tratamento mais aprimorado/avançado de argumentos de linha
      // comando se pode usar PicoCLI (https://picocli.info/)

      // o servidor deve reconhecer o diretório autorizado, um caminho a partir
      // do qual as operações (tools - ferramentas) com arquivos serão
      // executadas
      // o diretório autorizado pode ser passado ao servidor de três formas:
      // 1) variável de ambiente do sistema operacional (padrão para qualquer
      //    linguagem de programação receber dados externos):
      //    MCP_FILES_ROOT;
      // 2) propriedades (somente para aplicações Java):
      //    -Dmcp.files.root
      // 3) o caminho (nome do diretório autorizado) é passado como parâmetro
      //    à aplicação servidor MCP Java
      // ver os arquivos "client_var.bat", "client_prop.bat" e "client_cli.bat",
      // respectivamente, no diretório raiz deste projeto
      String  mcpFilesRootArg  = args.length != 0 ? args[0] : null;
      String  mcpFilesRootProp = System.getProperty(AppConsts.AUTH_DIR_PROP);
      String  mcpFilesRootVar  = System.getenv(AppConsts.AUTH_DIR_VAR);
      String  mcpFilesRoot     = "";
      // se o diretório autorizado foi recebido como propriedade da JVM,
      // isProperty será "true", caso contrário, será "false" e isto é
      // importante para o método createClient() saber como deve executar o
      // servidor MCP Java
      boolean isProperty       = mcpFilesRootProp != null;
      // o parâmetro à aplicação foi passado na linha de comando???
      if (mcpFilesRootArg != null) mcpFilesRoot = mcpFilesRootArg;
      else
         // a propriedade foi passada à JVM???
         if (mcpFilesRootProp != null) mcpFilesRoot = mcpFilesRootProp;
         else
            // a variável de ambiente foi ajustada???
            if (mcpFilesRootVar != null) mcpFilesRoot = mcpFilesRootVar;
            else
               LOG.error("O caminho para o diretório autorizado não foi " +
                         "fornecido");

      McpClient01SyncStdio clientStdio = new McpClient01SyncStdio(mcpFilesRoot,
                                                                  isProperty);

      try { clientStdio.execute(); }
      finally { clientStdio.close(); }
   }

//   private void listSelectRunToolLooping() {
//      // os parâmetros para a(s) ferramenta(s) serão lidos da entrada padrão
//      Scanner         scanner = new Scanner(System.in);
//      // recupera as ferramentas disponíveis no servidor
//      ListToolsResult tools   = loadTools();
//      // executa "in perpetuum"
//      while (true) {
//         // mostra as ferramentas (menu) ao usuário
//         showMenu(scanner);
//         // exibe o prompt ao usuário
//         System.out.print("> ");
//         // se não houver qualquer entrada, encerra o 'looping'
//         if (!scanner.hasNextLine())
//            break;
//         // obtém uma entrada e remove espaços de ambos os lados, se houverem
//         String command = scanner.nextLine().trim();
//         // se nada foi digitado, reinicia o 'looping'
//         if (command.isBlank())
//            continue;
//         // se o usuário desejar encerrar
//         if ("sair".equalsIgnoreCase(command))
//            break;
//         // se o usuário desejar a lista de ferramentas disponibilizada pelo
//         // servidor MCP
//         if ("listar".equalsIgnoreCase(command)) {
//            tools = loadTools();
//            // reinicia o 'looping'
//            continue;
//         }
//         // tenta encontrar a ferramenta desejada
//         Tool selectedTool = selectTool(tools.tools(),command);
//         // se a ferramenta não foi encontrada, reinicia o 'looping'
//         if (selectedTool == null) {
//            LOG.warn("Ferramenta não encontrada: {}",command);
//            continue;
//         }
//         // ao chegarmos aqui a ferramenta pode ser executada
//         executeTool(selectedTool,scanner);
//      }
//   }
}
