package com.pfe.stage.service;

import com.pfe.stage.dto.request.OffreStageRequest;
import com.pfe.stage.dto.response.OffreStageResponse;
import com.pfe.stage.entity.OffreStage;
import com.pfe.stage.repository.OffreStageRepository;
import com.pfe.stage.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OffreStageService {

    private final OffreStageRepository repo;
    private final SecurityUtils securityUtils;

    public List<OffreStageResponse> getOffresActives() {
        return repo.findByActiveTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<OffreStageResponse> getAllOffres() {
        return repo.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public OffreStageResponse getById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public OffreStageResponse create(OffreStageRequest req, Principal principal) {
        OffreStage o = new OffreStage();
        o.setTitre(req.getTitre());
        o.setDescription(req.getDescription());
        o.setEntreprise(req.getEntreprise());
        o.setLieu(req.getLieu());
        o.setDateDebut(req.getDateDebut());
        o.setDateFin(req.getDateFin());
        o.setDomaine(req.getDomaine());
        o.setCompetencesRequises(req.getCompetencesRequises());
        o.setCreateurId(securityUtils.getCurrentUserId(principal));
        return toResponse(repo.save(o));
    }

    @Transactional
    public OffreStageResponse update(Long id, OffreStageRequest req) {
        OffreStage o = findById(id);
        o.setTitre(req.getTitre());
        o.setDescription(req.getDescription());
        o.setEntreprise(req.getEntreprise());
        o.setLieu(req.getLieu());
        o.setDateDebut(req.getDateDebut());
        o.setDateFin(req.getDateFin());
        o.setDomaine(req.getDomaine());
        o.setCompetencesRequises(req.getCompetencesRequises());
        return toResponse(o);
    }

    @Transactional
    public void toggleActive(Long id) {
        OffreStage o = findById(id);
        o.setActive(!o.isActive());
    }

    @Transactional
    public void delete(Long id) {
        repo.delete(findById(id));
    }

    private OffreStage findById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Offre non trouvée"));
    }

    private OffreStageResponse toResponse(OffreStage o) {
        return new OffreStageResponse(
                o.getId(),
                o.getTitre(),
                o.getDescription(),
                o.getEntreprise(),
                o.getLieu(),
                o.getDateDebut(),
                o.getDateFin(),
                o.getDomaine(),
                o.getCompetencesRequises(),
                o.isActive(),
                o.getCreateurId(),   // ✅ AJOUT OBLIGATOIRE
                o.getCreatedAt()
        );
    }
}
