package com.pfe.stage.repository;

import com.pfe.stage.entity.Commentaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentaireRepository extends JpaRepository<Commentaire, Long> {

    List<Commentaire> findByLivrableId(Long livrableId);

    List<Commentaire> findByEncadrantId(Long encadrantId);

    // ✅ FIX: supprimer commentaires avant suppression livrable
    @Modifying
    @Query("DELETE FROM Commentaire c WHERE c.livrable.id = :livrableId")
    void deleteByLivrableId(@Param("livrableId") Long livrableId);
}