package com.pfe.stage.service;

import com.pfe.stage.client.AuthClient;
import com.pfe.stage.client.UserResponse;
import com.pfe.stage.dto.response.LivrableResponse;
import com.pfe.stage.entity.DemandeStage;
import com.pfe.stage.entity.Livrable;
import com.pfe.stage.enums.TypeLivrable;
import com.pfe.stage.repository.CommentaireRepository;
import com.pfe.stage.repository.DemandeStageRepository;
import com.pfe.stage.repository.LivrableRepository;
import com.pfe.stage.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class LivrableService {

    private final LivrableRepository livrableRepo;
    private final DemandeStageRepository demandeRepo;
    private final CommentaireRepository commentaireRepo;
    private final FileStorageService fileStorage;
    private final SecurityUtils securityUtils;
    private final AuthClient authClient;

    private final ConcurrentHashMap<Long, String> userCache = new ConcurrentHashMap<>();

    /* =====================================================
       DEPOT
       ===================================================== */

    @Transactional
    public LivrableResponse deposer(
            Long demandeId, String titre, String description,
            TypeLivrable type, MultipartFile file, Principal principal
    ) {
        Long etudiantId = securityUtils.getCurrentUserId(principal);

        DemandeStage demande = demandeRepo.findById(demandeId)
                .orElseThrow(() -> new EntityNotFoundException("Demande introuvable : " + demandeId));

        if (!demande.getEtudiantId().equals(etudiantId))
            throw new IllegalArgumentException("Accès interdit");

        String chemin = fileStorage.store(file);

        Livrable livrable = new Livrable();
        livrable.setTitre(titre);
        livrable.setDescription(description);
        livrable.setNomFichier(file.getOriginalFilename());
        livrable.setCheminFichier(chemin);
        livrable.setTypeMime(file.getContentType());
        livrable.setTailleFichier(file.getSize());
        livrable.setTypeLivrable(type);
        livrable.setEtudiantId(etudiantId);
        livrable.setDemande(demande);

        Livrable saved = livrableRepo.save(livrable);
        log.info("Livrable déposé : id={}, étudiant={}", saved.getId(), etudiantId);
        return toResponse(saved);
    }

    /* =====================================================
       UPDATE (étudiant — ses propres livrables)
       ===================================================== */

    @Transactional
    public LivrableResponse update(
            Long id, String titre, String description,
            TypeLivrable type, MultipartFile file, Principal principal
    ) {
        Long etudiantId = securityUtils.getCurrentUserId(principal);
        Livrable livrable = findById(id);

        if (!livrable.getEtudiantId().equals(etudiantId))
            throw new IllegalArgumentException("Modification interdite");

        livrable.setTitre(titre);
        livrable.setDescription(description);
        livrable.setTypeLivrable(type);

        // Nouveau fichier optionnel
        if (file != null && !file.isEmpty()) {
            fileStorage.delete(livrable.getCheminFichier());   // supprimer l'ancien
            livrable.setNomFichier(file.getOriginalFilename());
            livrable.setCheminFichier(fileStorage.store(file));
            livrable.setTypeMime(file.getContentType());
            livrable.setTailleFichier(file.getSize());
        }

        Livrable saved = livrableRepo.save(livrable);
        log.info("Livrable mis à jour : id={}, étudiant={}", id, etudiantId);
        return toResponse(saved);
    }

    /* =====================================================
       LISTES
       ===================================================== */

    @Transactional(readOnly = true)
    public List<LivrableResponse> getMesLivrables(Principal principal) {
        Long etudiantId = securityUtils.getCurrentUserId(principal);
        return livrableRepo.findByEtudiantId(etudiantId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<LivrableResponse> getLivrablesEncadrant(Principal principal) {
        Long encadrantId = securityUtils.getCurrentUserId(principal);
        return livrableRepo.findByEncadrantId(encadrantId)
                .stream().map(this::toResponse).toList();
    }

    public List<Long> getEtudiantsAvecLivrables(Principal principal) {
        Long encadrantId = securityUtils.getCurrentUserId(principal);
        return livrableRepo.findEtudiantsAyantDepose(encadrantId);
    }

    @Transactional(readOnly = true)
    public List<LivrableResponse> getByDemande(Long demandeId) {
        return livrableRepo.findByDemandeId(demandeId)
                .stream().map(this::toResponse).toList();
    }

    /* =====================================================
       DETAIL
       ===================================================== */

    @Transactional(readOnly = true)
    public LivrableResponse getById(Long id) {
        return toResponse(findById(id));
    }

    public String getChemin(Long id) {
        return findById(id).getCheminFichier();
    }

    /* =====================================================
       DELETE — étudiant (son propre livrable)
       ===================================================== */

    @Transactional
    public void delete(Long id, Principal principal) {
        Long etudiantId = securityUtils.getCurrentUserId(principal);
        Livrable livrable = findById(id);

        if (!livrable.getEtudiantId().equals(etudiantId))
            throw new IllegalArgumentException("Suppression interdite");

        commentaireRepo.deleteByLivrableId(id);
        fileStorage.delete(livrable.getCheminFichier());
        livrableRepo.delete(livrable);

        log.info("Livrable supprimé (étudiant) : id={}, étudiant={}", id, etudiantId);
    }

    /* =====================================================
       DELETE — encadrant (livrables de ses étudiants)
       ===================================================== */

    @Transactional
    public void deleteByEncadrant(Long id, Principal principal) {
        Long encadrantId = securityUtils.getCurrentUserId(principal);
        Livrable livrable = findById(id);

        // Vérifier que le livrable appartient bien à un étudiant encadré
        boolean estEncadrant = livrableRepo.findByEncadrantId(encadrantId)
                .stream().anyMatch(l -> l.getId().equals(id));

        if (!estEncadrant)
            throw new IllegalArgumentException("Suppression interdite : ce livrable ne fait pas partie de vos étudiants");

        commentaireRepo.deleteByLivrableId(id);
        fileStorage.delete(livrable.getCheminFichier());
        livrableRepo.delete(livrable);

        log.info("Livrable supprimé (encadrant) : id={}, encadrant={}", id, encadrantId);
    }

    /* =====================================================
       HELPERS
       ===================================================== */

    private Livrable findById(Long id) {
        return livrableRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Livrable introuvable : " + id));
    }

    private String getFullName(Long userId) {
        return userCache.computeIfAbsent(userId, uid -> {
            try {
                UserResponse user = authClient.getUserById(uid);
                if (user != null && user.getFullName() != null && !user.getFullName().isBlank())
                    return user.getFullName();
            } catch (Exception e) {
                log.warn("Feign getUserById({}) failed : {}", uid, e.getMessage());
            }
            return "Étudiant #" + uid;
        });
    }

    /* =====================================================
       MAPPING
       ===================================================== */

    private LivrableResponse toResponse(Livrable livrable) {
        return new LivrableResponse(
                livrable.getId(),
                livrable.getTitre(),
                livrable.getDescription(),
                livrable.getNomFichier(),
                livrable.getTypeMime(),
                livrable.getTailleFichier(),
                livrable.getTypeLivrable(),
                livrable.getEtudiantId(),
                getFullName(livrable.getEtudiantId()),
                livrable.getDemande().getId(),
                livrable.getCreatedAt()
        );
    }
}
