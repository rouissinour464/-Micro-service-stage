package com.pfe.stage.service;

import com.pfe.stage.dto.request.CommentaireRequest;
import com.pfe.stage.dto.response.CommentaireResponse;
import com.pfe.stage.entity.Commentaire;
import com.pfe.stage.entity.Livrable;
import com.pfe.stage.repository.CommentaireRepository;
import com.pfe.stage.repository.LivrableRepository;
import com.pfe.stage.security.SecurityUtils;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentaireService {

    private final CommentaireRepository commentaireRepo;
    private final LivrableRepository livrableRepo;
    private final SecurityUtils securityUtils;

    @Transactional
    public CommentaireResponse commenter(
            CommentaireRequest req,
            Principal principal
    ) {
        Long encadrantId = securityUtils.getCurrentUserId(principal);
        String auteurNom = extractFullName(principal);

        Livrable livrable = livrableRepo.findById(req.getLivrableId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Livrable introuvable : " + req.getLivrableId()));

        Long encadrantDemande = livrable.getDemande().getEncadrantId();

        if (encadrantDemande == null || !encadrantDemande.equals(encadrantId)) {
            throw new IllegalArgumentException(
                    "Vous n'êtes pas l'encadrant de cet étudiant.");
        }

        Commentaire commentaire = new Commentaire();
        commentaire.setContenu(req.getContenu());
        commentaire.setEncadrantId(encadrantId);
        commentaire.setLivrable(livrable);

        commentaire = commentaireRepo.save(commentaire);

        return toResponse(commentaire, auteurNom);
    }

    @Transactional(readOnly = true)
    public List<CommentaireResponse> getByLivrable(Long livrableId) {

        if (!livrableRepo.existsById(livrableId)) {
            throw new EntityNotFoundException(
                    "Livrable introuvable : " + livrableId);
        }

        return commentaireRepo
                .findByLivrableId(livrableId)
                .stream()
                .map(c -> toResponse(c, "Encadrant"))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommentaireResponse> getMesCommentaires(Principal principal) {

        Long encadrantId = securityUtils.getCurrentUserId(principal);
        String auteurNom = extractFullName(principal);

        return commentaireRepo
                .findByEncadrantId(encadrantId)
                .stream()
                .map(c -> toResponse(c, auteurNom))
                .toList();
    }

    @Transactional
    public void delete(Long id, Principal principal) {

        Long encadrantId = securityUtils.getCurrentUserId(principal);

        Commentaire commentaire = commentaireRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Commentaire introuvable : " + id));

        if (!commentaire.getEncadrantId().equals(encadrantId)) {
            throw new IllegalArgumentException(
                    "Vous ne pouvez supprimer que vos propres commentaires.");
        }

        commentaireRepo.delete(commentaire);
    }

    /* =========================================================
       EXTRAIRE LE NOM — compatible avec UsernamePasswordAuthentication
    ========================================================= */
    private String extractFullName(Principal principal) {
        if (principal instanceof Authentication auth) {
            Object details = auth.getDetails();
            if (details != null) {
                // essaie de caster en Map si ton UserDetails stocke des infos
                try {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> map =
                        (java.util.Map<String, Object>) details;
                    Object name = map.get("full_name");
                    if (name == null) name = map.get("name");
                    if (name != null) return name.toString();
                } catch (ClassCastException ignored) {}
            }
            // fallback : username (email ou login)
            return auth.getName();
        }
        return principal.getName();
    }

    private CommentaireResponse toResponse(Commentaire commentaire, String auteurNom) {
        return new CommentaireResponse(
                commentaire.getId(),
                commentaire.getContenu(),
                commentaire.getEncadrantId(),
                auteurNom,
                commentaire.getLivrable().getId(),
                commentaire.getLivrable().getTitre(),
                commentaire.getCreatedAt()
        );
    }
}