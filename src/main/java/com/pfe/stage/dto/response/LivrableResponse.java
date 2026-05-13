package com.pfe.stage.dto.response;

import com.pfe.stage.enums.TypeLivrable;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
@Data
@AllArgsConstructor
public class LivrableResponse {

    private Long id;
    private String titre;
    private String description;
    private String nomFichier;
    private String typeMime;
    private Long tailleFichier;
    private TypeLivrable typeLivrable;

    private Long etudiantId;
    private String etudiantFullName;   // ✅ AJOUT

    private Long demandeId;

    private LocalDateTime createdAt;
}