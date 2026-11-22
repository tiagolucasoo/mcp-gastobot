package br.edu.ifpr.gastobot.dto;

import java.time.LocalDate;
import java.util.List;

public record RelatorioFinanceiroResponse(LocalDate dataReferencia, List<ResumoPeriodoResponse> periodos) {
    public RelatorioFinanceiroResponse {
        periodos = List.copyOf(periodos);
    }
}
