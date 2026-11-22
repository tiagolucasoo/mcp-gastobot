package br.edu.ifpr.gastobot.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Lê configuração literal; não executa comandos nem expande variáveis. */
public final class LocalEnvironment {
    private LocalEnvironment() {
    }

    public static Map<String, String> carregar() {
        return carregar(Path.of(".env"), System.getenv());
    }

    public static Map<String, String> carregar(Path arquivo, Map<String, String> ambiente) {
        var valores = new HashMap<String, String>();
        if (Files.exists(arquivo)) {
            try {
                int numero = 0;
                for (String linha : Files.readAllLines(arquivo, StandardCharsets.UTF_8)) {
                    numero++;
                    linha = linha.replace("\uFEFF", "").strip();
                    if (linha.isEmpty() || linha.startsWith("#")) {
                        continue;
                    }
                    int separador = linha.indexOf('=');
                    if (separador <= 0 || !linha.substring(0, separador).strip().matches("[A-Za-z_][A-Za-z0-9_]*")) {
                        throw formato(numero);
                    }
                    String chave = linha.substring(0, separador).strip();
                    String valor = linha.substring(separador + 1).strip();
                    if (valor.startsWith("\"") || valor.startsWith("'")) {
                        char aspas = valor.charAt(0);
                        if (valor.length() < 2 || valor.charAt(valor.length() - 1) != aspas) {
                            throw formato(numero);
                        }
                        valor = valor.substring(1, valor.length() - 1);
                    }
                    valores.put(chave, valor);
                }
            } catch (IOException exception) {
                // Não inclui o conteúdo ou a causa, que podem conter segredos.
                throw new IllegalStateException("Não foi possível ler o arquivo .env");
            }
        }
        valores.putAll(ambiente);
        return Map.copyOf(valores);
    }

    private static IllegalArgumentException formato(int linha) {
        return new IllegalArgumentException("Formato inválido no .env, linha " + linha + ". Use CHAVE=valor");
    }
}
