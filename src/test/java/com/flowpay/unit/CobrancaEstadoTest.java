package com.flowpay.unit;

import com.flowpay.domain.Cobranca;
import com.flowpay.domain.StatusCobranca;
import com.flowpay.domain.TransicaoInvalidaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CobrancaEstadoTest {

    private Cobranca novaCobranca() {
        return new Cobranca("cliente-1", new BigDecimal("100.00"), LocalDate.now().plusDays(5), "agenda-123");
    }

    @Test
    void novaCobrancaNasceComoPendente() {
        assertThat(novaCobranca().getStatus()).isEqualTo(StatusCobranca.PENDENTE);
    }

    @Test
    void pendentePodeIrParaPaga() {
        Cobranca cobranca = novaCobranca();
        cobranca.transicionarPara(StatusCobranca.PAGA);
        assertThat(cobranca.getStatus()).isEqualTo(StatusCobranca.PAGA);
    }

    @Test
    void pendentePodeIrParaVencida() {
        Cobranca cobranca = novaCobranca();
        cobranca.transicionarPara(StatusCobranca.VENCIDA);
        assertThat(cobranca.getStatus()).isEqualTo(StatusCobranca.VENCIDA);
    }

    @Test
    void pendentePodeIrParaCancelada() {
        Cobranca cobranca = novaCobranca();
        cobranca.transicionarPara(StatusCobranca.CANCELADA);
        assertThat(cobranca.getStatus()).isEqualTo(StatusCobranca.CANCELADA);
    }

    @ParameterizedTest
    @EnumSource(value = StatusCobranca.class, names = {"PAGA", "VENCIDA", "CANCELADA"})
    void estadosFinaisNaoPermitemNenhumaTransicao(StatusCobranca estadoFinal) {
        for (StatusCobranca destino : StatusCobranca.values()) {
            assertThat(estadoFinal.podeTransicionarPara(destino)).isFalse();
        }
    }

    @Test
    void naoPodeVoltarDePagaParaPendente() {
        Cobranca cobranca = novaCobranca();
        cobranca.transicionarPara(StatusCobranca.PAGA);

        assertThatThrownBy(() -> cobranca.transicionarPara(StatusCobranca.PENDENTE))
                .isInstanceOf(TransicaoInvalidaException.class);
    }

    @Test
    void naoPodePagarUmaCobrancaJaCancelada() {
        Cobranca cobranca = novaCobranca();
        cobranca.transicionarPara(StatusCobranca.CANCELADA);

        assertThatThrownBy(() -> cobranca.transicionarPara(StatusCobranca.PAGA))
                .isInstanceOf(TransicaoInvalidaException.class);
    }
}
