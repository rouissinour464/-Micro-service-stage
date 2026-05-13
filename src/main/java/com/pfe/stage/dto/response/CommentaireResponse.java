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
    private Long livrableId;
    private String livrableTitre;
    private LocalDateTime createdAt;
}
