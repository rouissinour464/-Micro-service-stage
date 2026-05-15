package com.pfe.stage.controller;

import com.pfe.stage.dto.request.CommentaireRequest;
import com.pfe.stage.dto.response.CommentaireResponse;
import com.pfe.stage.service.CommentaireService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/commentaires")
@RequiredArgsConstructor
@CrossOrigin("*")
public class CommentaireController {

    private final CommentaireService commentaireService;

    /* =========================================================
       AJOUTER COMMENTAIRE
    ========================================================= */

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ENCADRANT')")
    public ResponseEntity<CommentaireResponse> commenter(
            @Valid @RequestBody CommentaireRequest request,
            Principal principal
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        commentaireService.commenter(
                                request,
                                principal
                        )
                );
    }

    /* =========================================================
       COMMENTAIRES PAR LIVRABLE
    ========================================================= */

    @GetMapping("/livrable/{livrableId}")
    public ResponseEntity<List<CommentaireResponse>>
    getByLivrable(
            @PathVariable Long livrableId
    ) {

        return ResponseEntity.ok(
                commentaireService.getByLivrable(
                        livrableId
                )
        );
    }

    /* =========================================================
       MES COMMENTAIRES
    ========================================================= */

    @GetMapping("/mes-commentaires")
    @PreAuthorize("hasAuthority('ROLE_ENCADRANT')")
    public ResponseEntity<List<CommentaireResponse>>
    getMesCommentaires(
            Principal principal
    ) {

        return ResponseEntity.ok(
                commentaireService.getMesCommentaires(
                        principal
                )
        );
    }

    /* =========================================================
       SUPPRIMER COMMENTAIRE
    ========================================================= */

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ENCADRANT')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            Principal principal
    ) {

        commentaireService.delete(
                id,
                principal
        );

        return ResponseEntity
                .noContent()
                .build();
    }
}