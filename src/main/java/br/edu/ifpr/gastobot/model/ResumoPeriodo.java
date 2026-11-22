package br.edu.ifpr.gastobot.model;

import java.time.LocalDate;

public record ResumoPeriodo(String periodo, LocalDate inicio, LocalDate fim, ResumoFinanceiro totais) {
}
