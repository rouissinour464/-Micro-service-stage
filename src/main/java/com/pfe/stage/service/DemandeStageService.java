package com.pfe.stage.service;

import com.pfe.stage.client.AuthClient;
import com.pfe.stage.dto.request.ChoixEncadrantRequest;
import com.pfe.stage.dto.request.DemandeStageRequest;
import com.pfe.stage.dto.request.ValidationDemandeRequest;
import com.pfe.stage.dto.response.DemandeStageResponse;
import com.pfe.stage.entity.DemandeStage;
import com.pfe.stage.enums.DemandeStatus;
import com.pfe.stage.repository.DemandeStageRepository;
import com.pfe.stage.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class DemandeStageService {

    private static final int MAX_ETUDIANTS_PAR_ENCADRANT = 10;

    private final DemandeStageRepository demandeRepo;
    private final SecurityUtils securityUtils;
    private final AuthClient authClient;

    private final ConcurrentHashMap<Long, String> nameCache =
            new ConcurrentHashMap<>();

    /* =====================================================
       ÉTUDIANT
       ===================================================== */

    @Transactional
    public DemandeStageResponse soumettre(
            DemandeStageRequest req,
            Principal principal
    ) {
        Long etudiantId = securityUtils.getCurrentUserId(principal);

        DemandeStage demande = new DemandeStage();
        demande.setEtudiantId(etudiantId);
        demande.setTitreProjet(req.getTitreProjet());
        demande.setDescriptionProjet(req.getDescriptionProjet());
        demande.setDomaine(req.getDomaine());
        demande.setNiveau(req.getNiveau());
        demande.setLieu(req.getLieu());
        demande.setEntreprise(req.getEntreprise());
        demande.setImageDemandeUrl(req.getImageDemandeUrl());
        demande.setStatus(DemandeStatus.EN_ATTENTE);

        DemandeStage saved = demandeRepo.save(demande);
        log.info("Nouvelle demande : id={}, étudiant={}", saved.getId(), etudiantId);
        return toResponse(saved);
    }

    public List<DemandeStageResponse> getMesDemandes(Principal principal) {
        Long etudiantId = securityUtils.getCurrentUserId(principal);
        return demandeRepo.findByEtudiantId(etudiantId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public DemandeStageResponse choisirEncadrant(
            Long demandeId,
            ChoixEncadrantRequest req,
            Principal principal
    ) {
        DemandeStage demande = findById(demandeId);
        Long etudiantId = securityUtils.getCurrentUserId(principal);

        if (!demande.getEtudiantId().equals(etudiantId))
            throw new IllegalArgumentException("Accès interdit");

        if (demande.getStatus() != DemandeStatus.VALIDEE)
            throw new IllegalStateException("Demande non validée");

        if (demande.getEncadrantId() != null)
            throw new IllegalStateException("Encadrant déjà choisi");

        if (demandeRepo.countByEncadrantId(req.getEncadrantId()) >= MAX_ETUDIANTS_PAR_ENCADRANT)
            throw new IllegalStateException("Capacité maximale atteinte");

        demande.setEncadrantId(req.getEncadrantId());
        DemandeStage saved = demandeRepo.save(demande);
        log.info("Encadrant {} assigné à la demande {}", req.getEncadrantId(), demandeId);
        return toResponse(saved);
    }

    /* =====================================================
       ENCADRANT
       ===================================================== */

    public List<DemandeStageResponse> getMesEncadrements(Principal principal) {
        Long encadrantId = securityUtils.getCurrentUserId(principal);
        return demandeRepo.findByEncadrantId(encadrantId)
                .stream().map(this::toResponse).toList();
    }

    /* =====================================================
       ADMIN
       ===================================================== */

    public List<DemandeStageResponse> getAllDemandes() {
        return demandeRepo.findAll().stream().map(this::toResponse).toList();
    }

    public List<DemandeStageResponse> getDemandesEnAttente() {
        return demandeRepo.findByStatus(DemandeStatus.EN_ATTENTE)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public DemandeStageResponse valider(Long id, ValidationDemandeRequest req) {
        DemandeStage demande = findById(id);

        if (demande.getStatus() != DemandeStatus.EN_ATTENTE)
            throw new IllegalStateException("Demande déjà traitée");

        demande.setStatus(req.getStatus());
        demande.setCommentaireAdmin(req.getCommentaire());
        demande.setDateValidation(LocalDateTime.now());

        DemandeStage saved = demandeRepo.save(demande);
        log.info("Demande {} -> {}", id, req.getStatus());
        return toResponse(saved);
    }

    public DemandeStageResponse getById(Long id) {
        return toResponse(findById(id));
    }

    /* =====================================================
       HELPERS
       ===================================================== */

    private DemandeStage findById(Long id) {
        return demandeRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Demande introuvable : " + id));
    }

    private String getUserNom(Long userId, String fallbackPrefix) {
        if (userId == null) return null;
        return nameCache.computeIfAbsent(userId, id -> {
            try {
                String fullName = authClient.getUserById(id).getFullName();
                if (fullName != null && !fullName.isBlank()) return fullName;
            } catch (Exception e) {
                log.warn("getUserById({}) failed : {}", id, e.getMessage());
            }
            return fallbackPrefix + " #" + id;
        });
    }

    /* =====================================================
       MAPPING
       ===================================================== */

    private DemandeStageResponse toResponse(DemandeStage d) {
        return new DemandeStageResponse(
                d.getId(),
                d.getEtudiantId(),
                getUserNom(d.getEtudiantId(), "Étudiant"),
                d.getTitreProjet(),
                d.getDescriptionProjet(),
                d.getDomaine(),
                d.getNiveau(),
                d.getLieu(),
                d.getEntreprise(),
                d.getStatus(),
                d.getImageDemandeUrl(),
                d.getCommentaireAdmin(),
                d.getEncadrantId(),
                getUserNom(d.getEncadrantId(), "Encadrant"), 
                d.getDateValidation(),
                d.getCreatedAt()
        );
    }
}