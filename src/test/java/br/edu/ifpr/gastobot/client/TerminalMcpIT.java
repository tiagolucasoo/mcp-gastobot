package br.edu.ifpr.gastobot.client;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TerminalMcpIT {
    @Test
    void terminalRegistraEConsultaMesmoSemToken(@TempDir Path pasta) throws Exception {
        var jar = Path.of("server/mcp_server01_server.jar").toAbsolutePath();
        var entrada = pasta.resolve("entrada.txt");
        Files.writeString(entrada, "/saida\n15,50\npix\nTeste terminal\n/relatorio\nsair\n");
        var saida = pasta.resolve("saida.txt");
        var builder = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8",
                "-Dgastobot.server.jar=" + jar, "-Dgastobot.db.path=" + pasta.resolve("teste.db"),
                "-cp", jar.toString(), GastoBotClient.class.getName(), "--telegram")
                .directory(pasta.toFile()).redirectInput(entrada.toFile())
                .redirectErrorStream(true).redirectOutput(saida.toFile());
        builder.environment().remove("TELEGRAM_BOT_TOKEN");
        var processo = builder.start();
        try {
            assertTrue(processo.waitFor(40, TimeUnit.SECONDS));
            String resultado = Files.readString(saida);
            assertEquals(0, processo.exitValue(), resultado);
            assertTrue(resultado.contains("Telegram indisponível"), resultado);
            assertTrue(resultado.contains("Lançamento registrado."), resultado);
            assertTrue(resultado.contains("Saídas: R$ 15,50 | Quantidade: 1"), resultado);
        } finally {
            processo.destroyForcibly();
        }
    }
}
