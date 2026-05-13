package com.pfe.stage.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class EncadrantInfo {
    private Long id;
    private String fullName;
    private long nbEtudiants;
    private int capaciteMax;
    private int placesRestantes;
    private boolean disponible;
}