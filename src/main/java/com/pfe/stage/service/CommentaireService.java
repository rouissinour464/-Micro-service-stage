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

    // ── ENCADRANT : commenter ────────────────────────────────
    @Transactional
    public CommentaireResponse commenter(CommentaireRequest req, Principal principal) {

        Long encadrantId = securityUtils.getCurrentUserId(principal);

        Livrable livrable = livrableRepo.findById(req.getLivrableId())
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Livrable introuvable : " + req.getLivrableId()
                        )
                );

        Long encadrantDemande = livrable.getDemande().getEncadrantId();
        if (encadrantDemande == null || !encadrantDemande.equals(encadrantId)) {
            throw new IllegalArgumentException(
                    "Vous n'êtes pas l'encadrant de cet étudiant."
            );
        }

        Commentaire commentaire = new Commentaire();
        commentaire.setContenu(req.getContenu());
        commentaire.setEncadrantId(encadrantId);
        commentaire.setLivrable(livrable);

        return toResponse(commentaireRepo.save(commentaire));
    }

    // ── Lire commentaires d'un livrable ─────────────────────
    // ✅ FIX: @Transactional garde la session JPA ouverte → résout LazyInitializationException
    @Transactional(readOnly = true)
    public List<CommentaireResponse> getByLivrable(Long livrableId) {

        if (!livrableRepo.existsById(livrableId)) {
            throw new EntityNotFoundException(
                    "Livrable introuvable : " + livrableId
            );
        }

        return commentaireRepo.findByLivrableId(livrableId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── ENCADRANT : mes commentaires ─────────────────────────
    @Transactional(readOnly = true)
    public List<CommentaireResponse> getMesCommentaires(Principal principal) {
        Long encadrantId = securityUtils.getCurrentUserId(principal);

        return commentaireRepo.findByEncadrantId(encadrantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ── ENCADRANT : supprimer ────────────────────────────────
    @Transactional
    public void delete(Long id, Principal principal) {

        Long encadrantId = securityUtils.getCurrentUserId(principal);

        Commentaire commentaire = commentaireRepo.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Commentaire introuvable : " + id
                        )
                );

        if (!commentaire.getEncadrantId().equals(encadrantId)) {
            throw new IllegalArgumentException(
                    "Vous ne pouvez supprimer que vos propres commentaires."
            );
        }

        commentaireRepo.delete(commentaire);
    }

    // ── Mapper ───────────────────────────────────────────────
    // ✅ FIX: accès à livrable.getTitre() dans session ouverte grâce à @Transactional
    private CommentaireResponse toResponse(Commentaire c) {
        return new CommentaireResponse(
                c.getId(),
                c.getContenu(),
                c.getEncadrantId(),
                c.getLivrable().getId(),
                c.getLivrable().getTitre(),
                c.getCreatedAt()
        );
    }
}