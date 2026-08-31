package com.VanControl.VanControl.pagamento.repository;

import com.VanControl.VanControl.pagamento.domain.entity.Pagamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PagamentoRepository extends JpaRepository<Pagamento, UUID> {

   Page<Pagamento> findByPassageiroId(UUID passageiroId, Pageable pageable);

   List<Pagamento> id(UUID id);

   boolean existsByPassageiroIdAndCompetencia(UUID passageiroId, String competencia);

   Page<Pagamento> findByCompetencia(String competencia, Pageable pageable);

   Optional<Pagamento> findByCodigoPagamento(String codigoPagamento);
}
