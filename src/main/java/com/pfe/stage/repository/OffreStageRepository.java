package com.pfe.stage.repository;

import com.pfe.stage.entity.OffreStage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OffreStageRepository extends JpaRepository<OffreStage, Long> {

    List<OffreStage> findByActiveTrue();

    List<OffreStage> findByDomaine(String domaine);
}