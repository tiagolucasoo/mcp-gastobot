package br.edu.ifpr.gastobot.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import br.edu.ifpr.gastobot.controller.LancamentoController;
import br.edu.ifpr.gastobot.dto.RelatorioFinanceiroRequest;
import br.edu.ifpr.gastobot.model.ResumoFinanceiro;
import br.edu.ifpr.gastobot.repository.LancamentoRepository;

class RelatorioFinanceiroTest {
    private final LancamentoRepository repository = mock(LancamentoRepository.class);
    private final LancamentoService service = new LancamentoService(repository);

    static Stream<Arguments> periodos() {
        return Stream.of(
                Arguments.of("2026-11-22", "2026-11-16", "2026-11-22", "2026-11-01", "2026-11-30"),
                Arguments.of("2026-11-01", "2026-10-26", "2026-11-01", "2026-11-01", "2026-11-30"),
                Arguments.of("2024-02-29", "2024-02-26", "2024-03-03", "2024-02-01", "2024-02-29"),
                Arguments.of("2026-12-31", "2026-12-28", "2027-01-03", "2026-12-01", "2026-12-31"));
    }

    @ParameterizedTest
    @MethodSource("periodos")
    void calendarioIncluiSemanaCompletaMesEAnoBissexto(String referencia, String inicioSemana,
            String fimSemana, String inicioMes, String fimMes) {
        when(repository.resumirPorPeriodo(anyString(), any(), any())).thenReturn(
                new ResumoFinanceiro(new BigDecimal("10.00"), new BigDecimal("2.00"), 2, 1));
        var data = LocalDate.parse(referencia);
        var result = new LancamentoController(service).relatorio(new RelatorioFinanceiroRequest(" u ", data));
        assertEquals(java.util.List.of("DIA", "SEMANA", "MES"), result.periodos().stream().map(p -> p.periodo()).toList());
        assertEquals(data, result.periodos().getFirst().dataInicio());
        assertEquals(data, result.periodos().getFirst().dataFim());
        assertEquals(LocalDate.parse(inicioSemana), result.periodos().get(1).dataInicio());
        assertEquals(LocalDate.parse(fimSemana), result.periodos().get(1).dataFim());
        assertEquals(LocalDate.parse(inicioMes), result.periodos().get(2).dataInicio());
        assertEquals(LocalDate.parse(fimMes), result.periodos().get(2).dataFim());
        assertEquals(3, result.periodos().getFirst().quantidade());
        assertEquals(2, result.periodos().getFirst().quantidadeEntradas());
        assertEquals(1, result.periodos().getFirst().quantidadeSaidas());
        assertEquals(new BigDecimal("10.00"), result.periodos().getFirst().totalEntradas());
        verify(repository).resumirPorPeriodo("u", data, data);
        verify(repository).resumirPorPeriodo("u", LocalDate.parse(inicioSemana), LocalDate.parse(fimSemana));
        verify(repository).resumirPorPeriodo("u", LocalDate.parse(inicioMes), LocalDate.parse(fimMes));
        assertThrows(UnsupportedOperationException.class, () -> result.periodos().clear());
    }

    @Test
    void parametrosInvalidosNaoConsultamBanco() {
        assertThrows(NullPointerException.class, () -> service.relatorio(null));
        assertThrows(IllegalArgumentException.class, () -> service.relatorio(new RelatorioFinanceiroRequest(null, LocalDate.now())));
        assertThrows(IllegalArgumentException.class, () -> service.relatorio(new RelatorioFinanceiroRequest(" ", LocalDate.now())));
        assertThrows(IllegalArgumentException.class, () -> service.relatorio(new RelatorioFinanceiroRequest("u", null)));
        verifyNoInteractions(repository);
    }
}
