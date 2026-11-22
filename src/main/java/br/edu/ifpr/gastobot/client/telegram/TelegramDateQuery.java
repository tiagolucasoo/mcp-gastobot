package br.edu.ifpr.gastobot.client.telegram;

import java.time.LocalDate;
import java.time.DateTimeException;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public record TelegramDateQuery(LocalDate inicio, LocalDate fim, int pagina) {
    private static final DateTimeFormatter BRASIL = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    public static TelegramDateQuery interpretar(String texto) {
        try {
            String normalizado = texto.strip().replaceAll("(?i)\\s+(a|ate|até)\\s+", " ");
            var partes = new ArrayList<>(Arrays.asList(normalizado.split("\\s+")));
            int pagina = 1;
            if (partes.getLast().startsWith("pagina=")) {
                pagina = Integer.parseInt(partes.removeLast().substring(7));
            }
            if (partes.isEmpty() || partes.size() > 2 || pagina < 1) {
                throw new IllegalArgumentException("Informe uma data ou duas datas, com página maior que zero");
            }
            LocalDate inicio = data(partes.getFirst());
            LocalDate fim = partes.size() == 1 ? inicio : data(partes.get(1));
            if (fim.isBefore(inicio)) {
                throw new IllegalArgumentException("A data final deve ser igual ou posterior à data inicial");
            }
            return new TelegramDateQuery(inicio, fim, pagina);
        } catch (DateTimeException | NumberFormatException exception) {
            throw new IllegalArgumentException("Data ou página inválida. Use DD/MM/AAAA ou AAAA-MM-DD; página deve ser inteira e positiva");
        }
    }

    private static LocalDate data(String texto) {
        return texto.contains("/") ? LocalDate.parse(texto, BRASIL) : LocalDate.parse(texto);
    }

    public Map<String, String> opcoes() {
        return Map.of("dataInicio", inicio.toString(), "dataFim", fim.toString(), "pagina", Integer.toString(pagina));
    }
}
