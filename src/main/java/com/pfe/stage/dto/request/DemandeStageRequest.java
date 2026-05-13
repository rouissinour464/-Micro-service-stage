package com.pfe.stage.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DemandeStageRequest {

    @NotBlank
    private String titreProjet;

    @NotBlank
    private String descriptionProjet;

    @NotBlank
    private String domaine;

    @NotBlank
    private String niveau;

    @NotBlank
    private String lieu;

    @NotBlank
    private String entreprise;

    private String imageDemandeUrl;
}