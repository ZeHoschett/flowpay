package com.flowpay.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estados possíveis de uma cobrança e as transições permitidas entre eles.
 *
 * Regra de negócio central do sistema:
 *   PENDENTE -> PAGA
 *   PENDENTE -> VENCIDA
 *   PENDENTE -> CANCELADA
 *   (PAGA, VENCIDA e CANCELADA são estados finais - nenhuma transição sai deles)
 */
public enum StatusCobranca {

    PENDENTE {
        @Override
        public Set<StatusCobranca> transicoesPermitidas() {
            return EnumSet.of(PAGA, VENCIDA, CANCELADA);
        }
    },
    PAGA {
        @Override
        public Set<StatusCobranca> transicoesPermitidas() {
            return EnumSet.noneOf(StatusCobranca.class);
        }
    },
    VENCIDA {
        @Override
        public Set<StatusCobranca> transicoesPermitidas() {
            return EnumSet.noneOf(StatusCobranca.class);
        }
    },
    CANCELADA {
        @Override
        public Set<StatusCobranca> transicoesPermitidas() {
            return EnumSet.noneOf(StatusCobranca.class);
        }
    };

    public abstract Set<StatusCobranca> transicoesPermitidas();

    public boolean podeTransicionarPara(StatusCobranca novoStatus) {
        return transicoesPermitidas().contains(novoStatus);
    }
}
