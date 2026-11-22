package br.edu.ifpr.gastobot.client.telegram;

public record TelegramMessage(long chatId, long usuarioId, boolean privado, boolean bot, String texto) {
}
