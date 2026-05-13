package com.pfe.stage.dto.response;

import com.pfe.stage.enums.DemandeStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemandeStageResponse {
    private Long id;
    private Long etudiantId;
    private String etudiantNom;
    private String titreProjet;
    private String descriptionProjet;
    private String domaine;
    private String niveau;
    private String lieu;
    private String entreprise;
    private DemandeStatus status;
    private String imageDemandeUrl;
    private String commentaireAdmin;
    private Long encadrantId;
    private String encadrantNom;      
    private LocalDateTime dateValidation;
    private LocalDateTime createdAt;
}