package br.edu.ifpr.gastobot.dto;

import java.time.LocalDate;

public record RelatorioFinanceiroRequest(String usuarioId, LocalDate dataReferencia) {
}
