package com.pfe.stage.controller;

import com.pfe.stage.dto.request.SoutenanceRequest;
import com.pfe.stage.dto.response.SoutenanceResponse;
import com.pfe.stage.service.SoutenanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/soutenances")
@RequiredArgsConstructor
public class SoutenanceController {

    private final SoutenanceService service;

    /* ───── ADMIN ───── */

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<SoutenanceResponse> creer(
            @Valid @RequestBody SoutenanceRequest request,
            Principal principal
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.creer(request, principal));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<SoutenanceResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /* ───── ÉTUDIANT ───── */

    @GetMapping("/ma-soutenance")
    @PreAuthorize("hasAuthority('ROLE_ETUDIANT')")
    public ResponseEntity<SoutenanceResponse> getMaSoutenance(
            Principal principal
    ) {
        return ResponseEntity.ok(
                service.getMaSoutenance(principal)
        );
    }

    /* ───── ENCADRANT ───── */

    @GetMapping("/mes-etudiants")
    @PreAuthorize("hasAuthority('ROLE_ENCADRANT')")
    public ResponseEntity<List<SoutenanceResponse>> getMesSoutenances(
            Principal principal
    ) {
        return ResponseEntity.ok(
                service.getMesSoutenancesEncadrant(principal)
        );
    }
}
