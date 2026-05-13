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

@RestController
@RequestMapping("/api/demandes")
@RequiredArgsConstructor
public class DemandeStageController {

    private final DemandeStageService demandeService;
    private final FileStorageService fileStorage;

    /* =========================================================
       ÉTUDIANT
       ========================================================= */

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ROLE_ETUDIANT')")
    public ResponseEntity<DemandeStageResponse> soumettre(

            @RequestPart("titreProjet") String titreProjet,
            @RequestPart("descriptionProjet") String descriptionProjet,
            @RequestPart("domaine") String domaine,
            @RequestPart("niveau") String niveau,
            @RequestPart("lieu") String lieu,
            @RequestPart("entreprise") String entreprise,
            @RequestPart(value = "file", required = false) MultipartFile file,
            Principal principal
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

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(demandeService.soumettre(req, principal));
    }

    /* ===================== UPDATE ===================== */

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ROLE_ETUDIANT')")
    public ResponseEntity<DemandeStageResponse> update(
            @PathVariable Long id,

            @RequestPart("titreProjet") String titreProjet,
            @RequestPart("descriptionProjet") String descriptionProjet,
            @RequestPart("domaine") String domaine,
            @RequestPart("niveau") String niveau,
            @RequestPart("lieu") String lieu,
            @RequestPart("entreprise") String entreprise,

            @RequestPart(value = "file", required = false) MultipartFile file,
            Principal principal
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

        return ResponseEntity.ok(
                demandeService.update(id, req, principal)
        );
    }

    /* ===================== DELETE ===================== */

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ETUDIANT')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            Principal principal
    ) {
        demandeService.delete(id, principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/mes-demandes")
    @PreAuthorize("hasAuthority('ROLE_ETUDIANT')")
    public ResponseEntity<List<DemandeStageResponse>> getMesDemandes(Principal principal) {
        return ResponseEntity.ok(demandeService.getMesDemandes(principal));
    }

    @PatchMapping("/{id}/encadrant")
    @PreAuthorize("hasAuthority('ROLE_ETUDIANT')")
    public ResponseEntity<DemandeStageResponse> choisirEncadrant(
            @PathVariable Long id,
            @Valid @RequestBody ChoixEncadrantRequest req,
            Principal principal
    ) {
        return ResponseEntity.ok(
                demandeService.choisirEncadrant(id, req, principal)
        );
    }

    /* =========================================================
       ENCADRANT
       ========================================================= */

    @GetMapping("/mes-encadrements")
    @PreAuthorize("hasAuthority('ROLE_ENCADRANT')")
    public ResponseEntity<List<DemandeStageResponse>> getMesEncadrements(Principal principal) {
        return ResponseEntity.ok(demandeService.getMesEncadrements(principal));
    }

    /* =========================================================
       ADMIN
       ========================================================= */

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<DemandeStageResponse>> getAllDemandes() {
        return ResponseEntity.ok(demandeService.getAllDemandes());
    }

    @GetMapping("/en-attente")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<DemandeStageResponse>> getEnAttente() {
        return ResponseEntity.ok(demandeService.getDemandesEnAttente());
    }

    @PatchMapping("/{id}/validation")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<DemandeStageResponse> valider(
            @PathVariable Long id,
            @Valid @RequestBody ValidationDemandeRequest req
    ) {
        return ResponseEntity.ok(demandeService.valider(id, req));
    }

    /* =========================================================
       PUBLIC
       ========================================================= */

    @GetMapping("/{id}")
    public ResponseEntity<DemandeStageResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(demandeService.getById(id));
    }
}