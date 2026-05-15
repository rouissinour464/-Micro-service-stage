package com.pfe.stage.controller;

import com.pfe.stage.dto.request.ChoixEncadrantRequest;
import com.pfe.stage.dto.request.DemandeStageRequest;
import com.pfe.stage.dto.request.ValidationDemandeRequest;
import com.pfe.stage.dto.response.DemandeStageResponse;
import com.pfe.stage.service.DemandeStageService;
import com.pfe.stage.service.FileStorageService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

/**
 * REST Controller pour la gestion des demandes de stage.
 *
 * Endpoints répartis par rôle :
 *   - ÉTUDIANT  : soumettre, modifier, supprimer, consulter ses demandes, choisir un encadrant
 *   - ENCADRANT : consulter ses encadrements
 *   - ADMIN     : consulter toutes les demandes, valider/rejeter
 *   - PUBLIC    : consulter une demande par ID
 */
@RestController
@RequestMapping("/api/demandes")
@RequiredArgsConstructor
public class DemandeStageController {

    private final DemandeStageService demandeService;
    private final FileStorageService  fileStorage;

    // =========================================================
    // ÉTUDIANT
    // =========================================================

    /**
     * POST /api/demandes
     * Soumet une nouvelle demande de stage (multipart/form-data).
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ROLE_ETUDIANT')")
    public ResponseEntity<DemandeStageResponse> soumettre(
            @RequestPart("titreProjet")        String titreProjet,
            @RequestPart("descriptionProjet")  String descriptionProjet,
            @RequestPart(value = "domaine",    required = false) String domaine,
            @RequestPart(value = "niveau",     required = false) String niveau,
            @RequestPart(value = "lieu",       required = false) String lieu,
            @RequestPart("entreprise")         String entreprise,
            @RequestPart(value = "file",       required = false) MultipartFile file,
            Principal principal
    ) {
        DemandeStageRequest req = buildRequest(
                titreProjet, descriptionProjet, domaine, niveau, lieu, entreprise, file
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(demandeService.soumettre(req, principal));
    }

    /**
     * PUT /api/demandes/{id}
     * Modifie une demande existante (statut EN_ATTENTE uniquement).
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ROLE_ETUDIANT')")
    public ResponseEntity<DemandeStageResponse> update(
            @PathVariable Long id,
            @RequestPart("titreProjet")        String titreProjet,
            @RequestPart("descriptionProjet")  String descriptionProjet,
            @RequestPart(value = "domaine",    required = false) String domaine,
            @RequestPart(value = "niveau",     required = false) String niveau,
            @RequestPart(value = "lieu",       required = false) String lieu,
            @RequestPart("entreprise")         String entreprise,
            @RequestPart(value = "file",       required = false) MultipartFile file,
            Principal principal
    ) {
        DemandeStageRequest req = buildRequest(
                titreProjet, descriptionProjet, domaine, niveau, lieu, entreprise, file
        );

        return ResponseEntity.ok(demandeService.update(id, req, principal));
    }

    /**
     * DELETE /api/demandes/{id}
     * Supprime une demande EN_ATTENTE appartenant à l'étudiant connecté.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ETUDIANT')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            Principal principal
    ) {
        demandeService.delete(id, principal);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/demandes/mes-demandes
     * Retourne toutes les demandes de l'étudiant connecté.
     */
    @GetMapping("/mes-demandes")
    @PreAuthorize("hasAuthority('ROLE_ETUDIANT')")
    public ResponseEntity<List<DemandeStageResponse>> getMesDemandes(Principal principal) {
        return ResponseEntity.ok(demandeService.getMesDemandes(principal));
    }

    /**
     * PATCH /api/demandes/{id}/encadrant
     * Permet à l'étudiant de choisir son encadrant (demande VALIDEE uniquement).
     */
    @PatchMapping("/{id}/encadrant")
    @PreAuthorize("hasAuthority('ROLE_ETUDIANT')")
    public ResponseEntity<DemandeStageResponse> choisirEncadrant(
            @PathVariable Long id,
            @Valid @RequestBody ChoixEncadrantRequest req,
            Principal principal
    ) {
        return ResponseEntity.ok(demandeService.choisirEncadrant(id, req, principal));
    }

    // =========================================================
    // ENCADRANT
    // =========================================================

    /**
     * GET /api/demandes/mes-encadrements
     * Retourne les demandes dont l'encadrant connecté est responsable.
     */
    @GetMapping("/mes-encadrements")
    @PreAuthorize("hasAuthority('ROLE_ENCADRANT')")
    public ResponseEntity<List<DemandeStageResponse>> getMesEncadrements(Principal principal) {
        return ResponseEntity.ok(demandeService.getMesEncadrements(principal));
    }

    // =========================================================
    // ADMIN
    // =========================================================

    /**
     * GET /api/demandes
     * Retourne toutes les demandes (tous statuts).
     */
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<DemandeStageResponse>> getAllDemandes() {
        return ResponseEntity.ok(demandeService.getAllDemandes());
    }

    /**
     * GET /api/demandes/en-attente
     * Retourne uniquement les demandes en attente de traitement.
     */
    @GetMapping("/en-attente")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<DemandeStageResponse>> getEnAttente() {
        return ResponseEntity.ok(demandeService.getDemandesEnAttente());
    }

    /**
     * PATCH /api/demandes/{id}/validation
     * Valide ou rejette une demande EN_ATTENTE.
     */
    @PatchMapping("/{id}/validation")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<DemandeStageResponse> valider(
            @PathVariable Long id,
            @Valid @RequestBody ValidationDemandeRequest req
    ) {
        return ResponseEntity.ok(demandeService.valider(id, req));
    }

    // =========================================================
    // PUBLIC
    // =========================================================

    /**
     * GET /api/demandes/{id}
     * Retourne le détail d'une demande (accès public).
     */
    @GetMapping("/{id}")
    public ResponseEntity<DemandeStageResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(demandeService.getById(id));
    }

    // =========================================================
    // HELPER PRIVÉ
    // =========================================================

    /**
     * Construit un {@link DemandeStageRequest} à partir des parts multipart.
     * Gère le stockage du fichier si présent.
     */
    private DemandeStageRequest buildRequest(
            String titreProjet,
            String descriptionProjet,
            String domaine,
            String niveau,
            String lieu,
            String entreprise,
            MultipartFile file
    ) {
        DemandeStageRequest req = new DemandeStageRequest();
        req.setTitreProjet(titreProjet);
        req.setDescriptionProjet(descriptionProjet);
        req.setDomaine(domaine);
        req.setNiveau(niveau);
        req.setLieu(lieu);
        req.setEntreprise(entreprise);

        if (file != null && !file.isEmpty()) {
            String imageUrl = fileStorage.store(file);
            req.setImageDemandeUrl(imageUrl);
        }

        return req;
    }
}
