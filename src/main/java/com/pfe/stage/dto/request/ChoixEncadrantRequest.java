package com.pfe.stage.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChoixEncadrantRequest {

    @NotNull(message = "encadrantId est obligatoire")
    private Long encadrantId;
}