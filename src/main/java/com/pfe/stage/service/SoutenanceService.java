package com.pfe.stage.service;

import com.pfe.stage.client.AuthClient;
import com.pfe.stage.dto.request.SoutenanceRequest;
import com.pfe.stage.dto.response.SoutenanceResponse;
import com.pfe.stage.entity.Soutenance;
import com.pfe.stage.repository.SoutenanceRepository;
import com.pfe.stage.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class SoutenanceService {

    private final SoutenanceRepository repository;
    private final SecurityUtils securityUtils;
    private final AuthClient authClient;

    // ✅ CACHE THREAD-SAFE
    private final ConcurrentHashMap<Long, String> nameCache =
            new ConcurrentHashMap<>();

    /* =========================================================
       CREATE (ADMIN)
       ========================================================= */

    @Transactional
    public SoutenanceResponse creer(
            SoutenanceRequest req,
            Principal principal
    ) {

        securityUtils.getCurrentUserId(principal);

        // validation jury
        if (
                req.getEncadrantId().equals(req.getRapporteurId()) ||
                req.getEncadrantId().equals(req.getPresidentId()) ||
                req.getRapporteurId().equals(req.getPresidentId())
        ) {
            throw new IllegalArgumentException(
                    "Les membres du jury doivent être différents."
            );
        }

        Soutenance s = new Soutenance();

        s.setEtudiantId(req.getEtudiantId());
        s.setEncadrantId(req.getEncadrantId());
        s.setRapporteurId(req.getRapporteurId());
        s.setPresidentId(req.getPresidentId());
        s.setDateHeure(req.getDateHeure());
        s.setSalle(req.getSalle());

        Soutenance saved = repository.save(s);

        log.info("Soutenance créée id={}", saved.getId());

        return toResponse(saved);
    }

    /* =========================================================
       ADMIN LIST
       ========================================================= */

    public List<SoutenanceResponse> getAll() {
        return repository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /* =========================================================
       ETUDIANT
       ========================================================= */

    public SoutenanceResponse getMaSoutenance(Principal principal) {

        Long etudiantId =
                securityUtils.getCurrentUserId(principal);

        return repository.findByEtudiantId(etudiantId)
                .stream()
                .map(this::toResponse)
                .findFirst()
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Aucune soutenance planifiée."
                        )
                );
    }

    /* =========================================================
       ENCADRANT
       ========================================================= */

    public List<SoutenanceResponse> getMesSoutenancesEncadrant(
            Principal principal
    ) {

        Long encadrantId =
                securityUtils.getCurrentUserId(principal);

        return repository.findByEncadrantId(encadrantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /* =========================================================
       DELETE
       ========================================================= */

    @Transactional
    public void delete(Long id) {

        if (!repository.existsById(id)) {
            throw new EntityNotFoundException(
                    "Soutenance introuvable : " + id
            );
        }

        repository.deleteById(id);

        log.info("Soutenance supprimée id={}", id);
    }

    /* =========================================================
       MAPPING
       ========================================================= */

    private SoutenanceResponse toResponse(Soutenance s) {

        return new SoutenanceResponse(
                s.getId(),

                s.getEtudiantId(),
                getName(s.getEtudiantId()),

                s.getEncadrantId(),
                getName(s.getEncadrantId()),

                s.getRapporteurId(),
                getName(s.getRapporteurId()),

                s.getPresidentId(),
                getName(s.getPresidentId()),

                s.getDateHeure(),
                s.getSalle(),
                s.getCreatedAt()
        );
    }

    /* =========================================================
       FEIGN + CACHE SAFE
       ========================================================= */

    private String getName(Long userId) {

        if (userId == null) return "N/A";

        return nameCache.computeIfAbsent(userId, id -> {

            try {
                String name =
                        authClient.getUserById(id)
                                .getFullName();

                if (name != null && !name.isBlank()) {
                    return name;
                }

            } catch (Exception e) {

                log.warn(
                        "Feign getUserById({}) failed: {}",
                        id,
                        e.getMessage()
                );
            }

            return "User #" + id;
        });
    }
}