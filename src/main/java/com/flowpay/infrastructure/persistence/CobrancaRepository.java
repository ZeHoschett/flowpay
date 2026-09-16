package com.flowpay.infrastructure.persistence;

import com.flowpay.domain.Cobranca;
import com.flowpay.domain.StatusCobranca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CobrancaRepository extends JpaRepository<Cobranca, UUID> {

    List<Cobranca> findByClienteId(String clienteId);

    List<Cobranca> findByClienteIdAndStatus(String clienteId, StatusCobranca status);
}
