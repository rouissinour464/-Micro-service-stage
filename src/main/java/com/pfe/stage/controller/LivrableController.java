package com.pfe.stage.controller;

import com.pfe.stage.dto.response.LivrableResponse;
import com.pfe.stage.enums.TypeLivrable;
import com.pfe.stage.service.FileStorageService;
import com.pfe.stage.service.LivrableService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/livrables")
@RequiredArgsConstructor
public class LivrableController {

    private final LivrableService livrableService;
    private final FileStorageService fileStorage;

    /* ===================== UPLOAD ===================== */

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ROLE_ETUDIANT')")
    public LivrableResponse deposer(
            @RequestParam Long demandeId,
            @RequestParam String titre,
            @RequestParam(required = false) String description,
            @RequestParam TypeLivrable typeLivrable,
            @RequestPart MultipartFile file,
            Principal principal
    ) {
        return livrableService.deposer(demandeId, titre, description, typeLivrable, file, principal);
    }

    /* ===================== UPDATE ===================== */

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ROLE_ETUDIANT')")
    public LivrableResponse update(
            @PathVariable Long id,
            @RequestParam String titre,
            @RequestParam(required = false) String description,
            @RequestParam TypeLivrable typeLivrable,
            @RequestPart(required = false) MultipartFile file,
            Principal principal
    ) {
        return livrableService.update(id, titre, description, typeLivrable, file, principal);
    }

    /* ===================== ETUDIANT ===================== */

    @GetMapping("/mes-livrables")
    @PreAuthorize("hasAuthority('ROLE_ETUDIANT')")
    public List<LivrableResponse> mesLivrables(Principal principal) {
        return livrableService.getMesLivrables(principal);
    }

    /* ===================== ENCADRANT ===================== */

    @GetMapping("/mes-etudiants")
    @PreAuthorize("hasAuthority('ROLE_ENCADRANT')")
    public List<LivrableResponse> livrablesEncadrant(Principal principal) {
        return livrableService.getLivrablesEncadrant(principal);
    }

    @GetMapping("/encadrant/etudiants")
    @PreAuthorize("hasAuthority('ROLE_ENCADRANT')")
    public List<Long> etudiantsAvecLivrables(Principal principal) {
        return livrableService.getEtudiantsAvecLivrables(principal);
    }

    /* ===================== GENERIC ===================== */

    @GetMapping("/demande/{demandeId}")
    public List<LivrableResponse> byDemande(@PathVariable Long demandeId) {
        return livrableService.getByDemande(demandeId);
    }

    @GetMapping("/{id}")
    public LivrableResponse getById(@PathVariable Long id) {
        return livrableService.getById(id);
    }

    /* ===================== DOWNLOAD ===================== */

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyAuthority('ROLE_ETUDIANT','ROLE_ENCADRANT')")
    public ResponseEntity<Resource> download(@PathVariable Long id) throws MalformedURLException {

        String chemin = livrableService.getChemin(id);

        if (chemin == null || chemin.isBlank()) {
            return ResponseEntity.notFound().build();
        }

        Path file = fileStorage.load(chemin);
        Resource resource = new UrlResource(file.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getFileName() + "\"")
                .body(resource);
    }

    /* ===================== DELETE ETUDIANT ===================== */

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ETUDIANT')")
    public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
        livrableService.delete(id, principal);
        return ResponseEntity.noContent().build();
    }

    /* ===================== DELETE ENCADRANT ===================== */

    @DeleteMapping("/{id}/encadrant")
    @PreAuthorize("hasAuthority('ROLE_ENCADRANT')")
    public ResponseEntity<Void> deleteByEncadrant(@PathVariable Long id, Principal principal) {
        livrableService.deleteByEncadrant(id, principal);
        return ResponseEntity.noContent().build();
    }
}