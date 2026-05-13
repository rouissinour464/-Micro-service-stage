package com.pfe.stage.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "soutenances")
@Data
@NoArgsConstructor
public class Soutenance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ───── Étudiant ───── */
    @Column(nullable = false)
    private Long etudiantId;

    /* ───── Encadrant pédagogique ───── */
    @Column(nullable = false)
    private Long encadrantId;

    /* ───── Jury ───── */
    @Column(nullable = false)
    private Long rapporteurId;

    @Column(nullable = false)
    private Long presidentId;

    /* ───── Logistique ───── */
    @Column(nullable = false)
    private LocalDateTime dateHeure;

    @Column(nullable = false)
    private String salle;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}