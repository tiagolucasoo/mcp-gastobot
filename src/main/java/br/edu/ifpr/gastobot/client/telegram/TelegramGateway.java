package br.edu.ifpr.gastobot.client.telegram;

import java.util.List;

public interface TelegramGateway {
    List<TelegramUpdate> receber(long offset) throws InterruptedException;
    void enviar(long chatId, String texto) throws InterruptedException;
}
