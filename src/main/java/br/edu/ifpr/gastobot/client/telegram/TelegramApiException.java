package br.edu.ifpr.gastobot.client.telegram;

/** Erro sanitizado: não inclui URL, token, corpo da resposta ou causa HTTP. */
public final class TelegramApiException extends RuntimeException {
    private final int codigo;

    public TelegramApiException(int codigo) {
        super(switch (codigo) {
            case 401 -> "Token do Telegram inválido; confira TELEGRAM_BOT_TOKEN";
            case 409 -> "Conflito no Telegram: encerre outra instância do bot e confira se existe webhook configurado";
            case 429 -> "Limite de requisições do Telegram; aguarde antes de tentar novamente";
            default -> "Não foi possível comunicar com a API do Telegram";
        });
        this.codigo = codigo;
    }

    public int codigo() {
        return codigo;
    }
}
