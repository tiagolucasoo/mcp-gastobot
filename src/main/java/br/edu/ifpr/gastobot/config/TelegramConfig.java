package br.edu.ifpr.gastobot.config;

import java.time.ZoneId;
import java.util.Map;

public record TelegramConfig(String token, ZoneId timeZone) {
    public TelegramConfig {
        if (token == null || !token.trim().matches("[0-9]+:[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("Defina TELEGRAM_BOT_TOKEN com o token fornecido pelo BotFather");
        }
        token = token.trim();
        java.util.Objects.requireNonNull(timeZone, "Fuso horário obrigatório");
    }

    public static TelegramConfig carregar() {
        return carregar(LocalEnvironment.carregar());
    }

    public static TelegramConfig carregar(Map<String, String> environment) {
        String zone = environment.get("GASTOBOT_TIME_ZONE");
        return new TelegramConfig(environment.get("TELEGRAM_BOT_TOKEN"),
                ZoneId.of(zone == null || zone.isBlank() ? "America/Sao_Paulo" : zone));
    }

    @Override
    public String toString() {
        return "TelegramConfig[token=<oculto>, timeZone=" + timeZone + "]";
    }
}
