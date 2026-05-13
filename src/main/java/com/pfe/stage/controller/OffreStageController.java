package com.pfe.stage.controller;

import com.pfe.stage.dto.request.OffreStageRequest;
import com.pfe.stage.dto.response.OffreStageResponse;
import com.pfe.stage.service.OffreStageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/offres")
@RequiredArgsConstructor
public class OffreStageController {

    private final OffreStageService offreService;

    /* ───────── ÉTUDIANT (PUBLIC) ───────── */

    @GetMapping
    public List<OffreStageResponse> getOffresActives() {
        return offreService.getOffresActives();
    }

    @GetMapping("/{id}")
    public OffreStageResponse getById(@PathVariable Long id) {
        return offreService.getById(id);
    }

    /* ───────── ADMIN ───────── */

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<OffreStageResponse> getAllOffres() {
        return offreService.getAllOffres();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<OffreStageResponse> create(
            @RequestBody @Valid OffreStageRequest req,
            Principal principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(offreService.create(req, principal));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public OffreStageResponse update(
            @PathVariable Long id,
            @RequestBody @Valid OffreStageRequest req
    ) {
        return offreService.update(id, req);
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void toggle(@PathVariable Long id) {
        offreService.toggleActive(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public void delete(@PathVariable Long id) {
        offreService.delete(id);
    }
}