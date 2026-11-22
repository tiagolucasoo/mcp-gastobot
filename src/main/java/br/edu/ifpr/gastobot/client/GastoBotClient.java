package br.edu.ifpr.gastobot.client;

import java.util.Map;
import java.util.Scanner;
import java.time.Clock;
import java.time.ZoneId;
import java.nio.charset.Charset;

import br.edu.ifpr.gastobot.client.telegram.TelegramBot;
import br.edu.ifpr.gastobot.client.telegram.TelegramCommandParser;
import br.edu.ifpr.gastobot.client.telegram.TelegramCommandService;
import br.edu.ifpr.gastobot.client.telegram.TelegramConversation;
import br.edu.ifpr.gastobot.client.mcp.StdioMcpConnection;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import tools.jackson.databind.json.JsonMapper;

/** Cliente genérico: descobre ferramentas e aceita argumentos JSON de qualquer ferramenta. */
public final class GastoBotClient {
    private GastoBotClient() {
    }

    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("--somente-telegram")) {
            TelegramBot.main(args);
            return;
        }
        boolean comTelegram = args.length == 1 && args[0].equals("--telegram");
        if (args.length != 0 && !comTelegram) {
            throw new IllegalArgumentException("Use sem argumentos, --telegram ou --somente-telegram");
        }
        var console = System.console();
        var charset = console == null ? Charset.defaultCharset() : console.charset();
        try (var client = StdioMcpConnection.conectar(); var scanner = new Scanner(System.in, charset)) {
            Thread bot = comTelegram ? TelegramBot.iniciarEmSegundoPlano(client) : null;
            try {
                executarMenu(client, scanner, new JsonMapper());
            } finally {
                if (bot != null) {
                    bot.interrupt();
                    try {
                        bot.join();
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    private static void executarMenu(StdioMcpConnection client, Scanner scanner, JsonMapper json) {
        var conversa = new TelegramConversation(new TelegramCommandParser());
        var service = new TelegramCommandService(client, Clock.system(ZoneId.of("America/Sao_Paulo")));
        var tools = client.ferramentas();
        System.out.println("GastoBot — /entrada, /saida, /consultar, /relatorio, /cancelar");
        System.out.println("Digite /ferramentas para ver as ferramentas MCP ou sair para encerrar.");
        while (true) {
            System.out.print("> ");
            if (!scanner.hasNextLine()) {
                return;
            }
            String name = scanner.nextLine().trim();
            if (name.equalsIgnoreCase("sair")) {
                return;
            }
            if (name.equals("/ferramentas")) {
                tools = client.ferramentas();
                for (var tool : tools) {
                    System.out.println(tool.name() + " — " + tool.description());
                    System.out.println("Parâmetros: " + json.writeValueAsString(tool.inputSchema()));
                }
                continue;
            }
            if (tools.stream().noneMatch(tool -> tool.name().equals(name))) {
                System.out.println(conversa.responder(name, 0, "terminal:local", service));
                continue;
            }
            System.out.print("Argumentos JSON em uma linha: ");
            if (!scanner.hasNextLine()) {
                return;
            }
            try {
                var arguments = argumentos(json, scanner.nextLine());
                var result = client.executar(name, arguments);
                System.out.println(Boolean.TRUE.equals(result.isError()) ? "Erro:" : "Resultado:");
                for (var content : result.content()) {
                    if (content instanceof TextContent text) {
                        System.out.println(text.text());
                    }
                }
            } catch (RuntimeException exception) {
                System.out.println("Não foi possível executar: " + exception.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> argumentos(JsonMapper json, String texto) {
        Map<String, Object> arguments = json.readValue(texto, Map.class);
        if (arguments == null) {
            throw new IllegalArgumentException("Informe um objeto JSON");
        }
        return arguments;
    }
}
