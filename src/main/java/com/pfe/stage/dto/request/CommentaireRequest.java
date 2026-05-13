package com.pfe.stage.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentaireRequest {

    @NotNull(message = "L'id du livrable est obligatoire")
    private Long livrableId;

    @NotBlank(message = "Le contenu est obligatoire")
    private String contenu;
}