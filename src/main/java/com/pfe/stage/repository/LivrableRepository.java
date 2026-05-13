package com.pfe.stage.repository;

import com.pfe.stage.entity.Livrable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LivrableRepository extends JpaRepository<Livrable, Long> {

    List<Livrable> findByEtudiantId(Long etudiantId);

    List<Livrable> findByDemandeId(Long demandeId);

    @Query("""
        SELECT l
        FROM Livrable l
        WHERE l.demande.encadrantId = :encadrantId
    """)
    List<Livrable> findByEncadrantId(@Param("encadrantId") Long encadrantId);

    @Query("""
        SELECT DISTINCT l.etudiantId
        FROM Livrable l
        WHERE l.demande.encadrantId = :encadrantId
    """)
    List<Long> findEtudiantsAyantDepose(@Param("encadrantId") Long encadrantId);
}