package com.pfe.stage.repository;

import com.pfe.stage.entity.DemandeStage;
import com.pfe.stage.enums.DemandeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DemandeStageRepository extends JpaRepository<DemandeStage, Long> {

    List<DemandeStage> findByEtudiantId(Long etudiantId);

    List<DemandeStage> findByStatus(DemandeStatus status);

    List<DemandeStage> findByEncadrantId(Long encadrantId);

    long countByEncadrantId(Long encadrantId);
}
