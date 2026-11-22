package br.edu.ifpr.gastobot.dto;

import java.time.LocalDate;

public record ConsultarLancamentosRequest(String usuarioId, LocalDate dataInicio, LocalDate dataFim, int pagina) {
}
