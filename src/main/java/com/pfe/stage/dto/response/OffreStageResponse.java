package com.pfe.stage.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class OffreStageResponse {

    private Long id;
    private String titre;
    private String description;
    private String entreprise;
    private String lieu;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String domaine;
    private String competencesRequises;
    private boolean active;
    private Long createurId;
    private LocalDateTime createdAt;
}