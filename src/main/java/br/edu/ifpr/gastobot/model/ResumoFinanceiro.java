package br.edu.ifpr.gastobot.model;

import java.math.BigDecimal;

public record ResumoFinanceiro(BigDecimal totalEntradas, BigDecimal totalSaidas,
        long quantidadeEntradas, long quantidadeSaidas) {
    public long quantidade() {
        return Math.addExact(quantidadeEntradas, quantidadeSaidas);
    }
}
