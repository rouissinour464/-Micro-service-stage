package com.pfe.stage.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SoutenanceRequest {

    /* Étudiant */
    @NotNull
    private Long etudiantId;

    /* Encadrant pédagogique */
    @NotNull
    private Long encadrantId;

    /* Jury */
    @NotNull
    private Long rapporteurId;

    @NotNull
    private Long presidentId;

    /* Logistique */
    @NotNull
    @Future
    private LocalDateTime dateHeure;

    @NotBlank
    private String salle;
}