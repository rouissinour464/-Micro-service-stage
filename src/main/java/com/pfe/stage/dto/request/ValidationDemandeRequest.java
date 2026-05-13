package com.pfe.stage.dto.request;

import com.pfe.stage.enums.DemandeStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ValidationDemandeRequest {

    @NotNull(message = "Le statut est obligatoire")
    private DemandeStatus status;

    private String commentaire;
}