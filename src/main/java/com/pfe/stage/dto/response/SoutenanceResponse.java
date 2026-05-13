package com.pfe.stage.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class SoutenanceResponse {

    private Long id;

    private Long etudiantId;
    private String etudiantNom;

    private Long encadrantId;
    private String encadrantNom;

    private Long rapporteurId;
    private String rapporteurNom;

    private Long presidentId;
    private String presidentNom;

    private LocalDateTime dateHeure;
    private String salle;

    private LocalDateTime createdAt;
}