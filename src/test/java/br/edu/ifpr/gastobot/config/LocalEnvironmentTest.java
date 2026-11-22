package br.edu.ifpr.gastobot.config;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalEnvironmentTest {
    @TempDir Path directory;

    @Test
    void carregaTokenDoArquivoComComentariosBomEAspas() throws Exception {
        Path file = directory.resolve(".env");
        Files.writeString(file, "\uFEFF# configuração local\n\nTELEGRAM_BOT_TOKEN=\"123:test-only\"\n"
                + "GASTOBOT_TIME_ZONE='America/Sao_Paulo'\nGASTOBOT_DB_PATH=data/gastos.db\nVAZIO=\n");
        var values = LocalEnvironment.carregar(file, Map.of());
        assertEquals("123:test-only", TelegramConfig.carregar(values).token());
        assertEquals("", values.get("VAZIO"));
        assertEquals(Path.of("data/gastos.db").toAbsolutePath().normalize(),
                DatabaseConfig.caminho(new java.util.Properties(), values));
    }

    @Test
    void ambienteTemPrecedenciaEArquivoAusenteEhOpcional() throws Exception {
        Path file = directory.resolve(".env");
        assertEquals(Map.of("CHAVE", "ambiente"), LocalEnvironment.carregar(file, Map.of("CHAVE", "ambiente")));
        Files.writeString(file, "CHAVE=arquivo\n");
        assertEquals("ambiente", LocalEnvironment.carregar(file, Map.of("CHAVE", "ambiente")).get("CHAVE"));
    }

    @Test
    void valoresSaoLiteraisESemExpansao() throws Exception {
        Path file = directory.resolve(".env");
        Files.writeString(file, "VALOR=a=b#c\nCOMANDO=$(nao_executar)\n");
        var values = LocalEnvironment.carregar(file, Map.of());
        assertEquals("a=b#c", values.get("VALOR"));
        assertEquals("$(nao_executar)", values.get("COMANDO"));
        assertThrows(UnsupportedOperationException.class, () -> values.put("OUTRO", "x"));
    }

    @Test
    void erroDeFormatoNaoExpoeSegredo() throws Exception {
        Path file = directory.resolve(".env");
        Files.writeString(file, "123:test-only\n");
        var error = assertThrows(IllegalArgumentException.class, () -> LocalEnvironment.carregar(file, Map.of()));
        assertTrue(error.getMessage().contains("linha 1"));
        assertFalse(error.getMessage().contains("test-only"));
        Files.writeString(file, "TELEGRAM_BOT_TOKEN='123:test-only\n");
        assertThrows(IllegalArgumentException.class, () -> LocalEnvironment.carregar(file, Map.of()));
    }

    @Test
    void erroDeLeituraNaoExpoeConteudo() {
        var error = assertThrows(IllegalStateException.class, () -> LocalEnvironment.carregar(directory, Map.of()));
        assertNull(error.getCause());
    }
}
