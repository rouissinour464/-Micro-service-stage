package com.pfe.stage.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class CommentaireResponse {
    private Long id;
    private String contenu;
    private Long encadrantId;
    private String auteurNom;      // ← transient, pas en DB
    private Long livrableId;
    private String livrableTitre;
    private LocalDateTime createdAt;
}