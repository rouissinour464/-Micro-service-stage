package com.pfe.stage.entity;

import com.pfe.stage.enums.DemandeStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Table(name = "demandes_stage")
@Data
@NoArgsConstructor
public class DemandeStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ───── Étudiant ───── */
    @Column(nullable = false)
    private Long etudiantId;

    /* ───── Infos stage ───── */
    @Column(nullable = false)
    private String titreProjet;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String descriptionProjet;

    @Column(nullable = false)
    private String domaine;

    @Column(nullable = false)
    private String niveau;

    @Column(nullable = false)
    private String lieu;

    @Column(nullable = false)
    private String entreprise;

    /* ───── Fichier ───── */
    @Column(columnDefinition = "TEXT")
    private String imageDemandeUrl;

    /* ───── Processus ───── */
    @Enumerated(EnumType.STRING)
    private DemandeStatus status = DemandeStatus.EN_ATTENTE;

    @Column(columnDefinition = "TEXT")
    private String commentaireAdmin;

    private Long encadrantId;
    private LocalDateTime dateValidation;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
