package br.edu.ifpr.gastobot.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ResumoPeriodoResponse(String periodo, LocalDate dataInicio, LocalDate dataFim,
        BigDecimal totalEntradas, BigDecimal totalSaidas,
        long quantidadeEntradas, long quantidadeSaidas, long quantidade) {
}
