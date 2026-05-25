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

    // =========================================================
    // ÉTUDIANT
    // =========================================================

    @Transactional
    public DemandeStageResponse soumettre(DemandeStageRequest req, Principal principal) {
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
        log.info("Nouvelle demande créée : id={}, étudiant={}", saved.getId(), etudiantId);
        return toResponse(saved);
    }

    @Transactional
    public DemandeStageResponse update(Long id, DemandeStageRequest req, Principal principal) {
        DemandeStage demande = findById(id);
        Long etudiantId = securityUtils.getCurrentUserId(principal);

        verifierProprietaire(demande.getEtudiantId(), etudiantId,
                "Vous ne pouvez modifier que vos propres demandes.");
        verifierStatut(demande, DemandeStatus.EN_ATTENTE,
                "Impossible de modifier une demande déjà traitée.");

        demande.setTitreProjet(req.getTitreProjet());
        demande.setDescriptionProjet(req.getDescriptionProjet());
        demande.setDomaine(req.getDomaine());
        demande.setNiveau(req.getNiveau());
        demande.setLieu(req.getLieu());
        demande.setEntreprise(req.getEntreprise());

        if (req.getImageDemandeUrl() != null && !req.getImageDemandeUrl().isBlank()) {
            demande.setImageDemandeUrl(req.getImageDemandeUrl());
        }

        DemandeStage saved = demandeRepo.save(demande);
        log.info("Demande {} modifiée par étudiant {}", id, etudiantId);
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id, Principal principal) {
        DemandeStage demande = findById(id);
        Long etudiantId = securityUtils.getCurrentUserId(principal);

        verifierProprietaire(demande.getEtudiantId(), etudiantId,
                "Vous ne pouvez supprimer que vos propres demandes.");
        verifierStatut(demande, DemandeStatus.EN_ATTENTE,
                "Impossible de supprimer une demande déjà traitée.");

        demandeRepo.delete(demande);
        log.info("Demande {} supprimée par étudiant {}", id, etudiantId);
    }

    public List<DemandeStageResponse> getMesDemandes(Principal principal) {
        Long etudiantId = securityUtils.getCurrentUserId(principal);
        return demandeRepo.findByEtudiantId(etudiantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public DemandeStageResponse choisirEncadrant(
            Long demandeId,
            ChoixEncadrantRequest req,
            Principal principal
    ) {
        DemandeStage demande = findById(demandeId);
        Long etudiantId = securityUtils.getCurrentUserId(principal);

        verifierProprietaire(demande.getEtudiantId(), etudiantId, "Accès interdit.");

        if (demande.getStatus() != DemandeStatus.VALIDEE) {
            throw new IllegalStateException(
                    "Seule une demande validée peut recevoir un encadrant.");
        }

        if (demande.getEncadrantId() != null) {
            throw new IllegalStateException(
                    "Un encadrant a déjà été attribué à cette demande.");
        }

        long charge = demandeRepo.countByEncadrantId(req.getEncadrantId());
        if (charge >= MAX_ETUDIANTS_PAR_ENCADRANT) {
            throw new IllegalStateException(
                    "Cet encadrant a atteint sa capacité maximale ("
                    + MAX_ETUDIANTS_PAR_ENCADRANT + " étudiants).");
        }

        demande.setEncadrantId(req.getEncadrantId());
        DemandeStage saved = demandeRepo.save(demande);
        log.info("Encadrant {} assigné à la demande {}", req.getEncadrantId(), demandeId);
        return toResponse(saved);
    }

    // =========================================================
    // ENCADRANT
    // =========================================================

    public List<DemandeStageResponse> getMesEncadrements(Principal principal) {
        Long encadrantId = securityUtils.getCurrentUserId(principal);
        return demandeRepo.findByEncadrantId(encadrantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================================================
    // ADMIN
    // =========================================================

    public List<DemandeStageResponse> getAllDemandes() {
        return demandeRepo.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<DemandeStageResponse> getDemandesEnAttente() {
        return demandeRepo.findByStatus(DemandeStatus.EN_ATTENTE)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public DemandeStageResponse valider(Long id, ValidationDemandeRequest req) {
        DemandeStage demande = findById(id);

        if (demande.getStatus() != DemandeStatus.EN_ATTENTE) {
            throw new IllegalStateException("Cette demande a déjà été traitée.");
        }

        demande.setStatus(req.getStatus());
        demande.setCommentaireAdmin(req.getCommentaire());
        demande.setDateValidation(LocalDateTime.now());

        DemandeStage saved = demandeRepo.save(demande);
        log.info("Demande {} -> {}", id, req.getStatus());
        return toResponse(saved);
    }

    // =========================================================
    // PUBLIC
    // =========================================================

    public DemandeStageResponse getById(Long id) {
        return toResponse(findById(id));
    }

    // =========================================================
    // HELPERS PRIVÉS
    // =========================================================

    private DemandeStage findById(Long id) {
        return demandeRepo.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Demande introuvable : " + id));
    }

    private void verifierProprietaire(Long ownerId, Long currentId, String message) {
        if (!ownerId.equals(currentId)) {
            throw new IllegalArgumentException(message);
        }
    }

    private void verifierStatut(DemandeStage demande, DemandeStatus attendu, String message) {
        if (demande.getStatus() != attendu) {
            throw new IllegalStateException(message);
        }
    }

    private String getUserNom(Long userId, String fallbackPrefix) {
        if (userId == null) return null;
        return nameCache.computeIfAbsent(userId, id -> {
            try {
                String fullName = authClient.getUserById(id).getFullName();
                if (fullName != null && !fullName.isBlank()) {
                    return fullName;
                }
            } catch (Exception e) {
                log.warn("getUserById({}) failed : {}", id, e.getMessage());
            }
            return fallbackPrefix + " #" + id;
        });
    }

    // =========================================================
    // MAPPING DTO
    // =========================================================

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