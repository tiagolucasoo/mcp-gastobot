package br.edu.ifpr.gastobot.client.telegram;

public sealed interface TelegramStep {
    record Pergunta(String texto) implements TelegramStep {
    }

    record Executar(TelegramCommand comando) implements TelegramStep {
    }
}
