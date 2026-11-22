package br.edu.ifpr.gastobot.client.telegram;

/** Mensagem pode ser nula para atualizações que não devem executar comandos. */
public record TelegramUpdate(long id, TelegramMessage mensagem) {
}
