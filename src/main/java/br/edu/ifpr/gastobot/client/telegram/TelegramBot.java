package br.edu.ifpr.gastobot.client.telegram;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;

import br.edu.ifpr.gastobot.client.mcp.StdioMcpConnection;
import br.edu.ifpr.gastobot.client.mcp.McpGateway;
import br.edu.ifpr.gastobot.config.TelegramConfig;

public final class TelegramBot {
    private TelegramBot() {
    }

    public static void main(String[] args) {
        var config = TelegramConfig.carregar();
        Thread pollingThread = Thread.currentThread();
        Thread shutdown = new Thread(() -> {
            pollingThread.interrupt();
            try {
                pollingThread.join(5_000);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        });
        Runtime.getRuntime().addShutdownHook(shutdown);
        try (var mcp = StdioMcpConnection.conectar()) {
            executar(mcp, config);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } finally {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdown);
            } catch (IllegalStateException exception) {
                // A JVM já está executando o hook de encerramento.
            }
        }
    }

    public static Thread iniciarEmSegundoPlano(McpGateway mcp) {
        return Thread.ofPlatform().name("telegram-polling").daemon().start(() -> {
            try {
                executar(mcp, TelegramConfig.carregar());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } catch (TelegramApiException | IllegalArgumentException exception) {
                System.err.println("Telegram indisponível. " + exception.getMessage() + ". O terminal continua ativo.");
            } catch (RuntimeException exception) {
                System.err.println("Telegram indisponível. Confira a configuração do bot. O terminal continua ativo.");
            }
        });
    }

    private static void executar(McpGateway mcp, TelegramConfig config) throws InterruptedException {
        try (var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()) {
            var service = new TelegramCommandService(mcp, Clock.system(config.timeZone()));
            var handler = new TelegramUpdateHandler(new TelegramCommandParser(), service);
            System.err.println("GastoBot Telegram iniciado. Abra o chat privado do bot e envie /start.");
            new TelegramPollingBot(new TelegramApiClient(http, config.token()), handler).executar();
        }
    }
}
