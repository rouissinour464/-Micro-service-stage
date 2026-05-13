package com.pfe.stage.repository;

import com.pfe.stage.entity.Soutenance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SoutenanceRepository extends JpaRepository<Soutenance, Long> {

    List<Soutenance> findByEtudiantId(Long etudiantId);

    List<Soutenance> findByEncadrantId(Long encadrantId);
}