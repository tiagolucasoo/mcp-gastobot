package br.edu.ifpr.gastobot.client.telegram;

import java.util.Map;

public record TelegramCommand(String nome, Map<String, String> opcoes) {
    public TelegramCommand {
        opcoes = Map.copyOf(opcoes);
    }
}
