package com.pfe.stage.service;

import com.pfe.stage.dto.request.DemandeStageRequest;
import com.pfe.stage.dto.response.DemandeStageResponse;
import com.pfe.stage.entity.DemandeStage;
import com.pfe.stage.enums.DemandeStatus;
import com.pfe.stage.repository.DemandeStageRepository;
import com.pfe.stage.security.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemandeStageServiceTest {

    @Mock
    private DemandeStageRepository demandeRepo;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private Principal principal;

    @InjectMocks
    private DemandeStageService demandeService;

    @Test
    void soumettre_ok() {

        // GIVEN
        DemandeStageRequest req = new DemandeStageRequest();
        req.setTitreProjet("Projet IA");
        req.setDescriptionProjet("Analyse et IA");
        req.setDomaine("Informatique");
        req.setNiveau("Master");
        req.setLieu("Tunis");
        req.setEntreprise("Entreprise X");
        req.setImageDemandeUrl("image.png");

        DemandeStage saved = new DemandeStage();
        saved.setId(100L);
        saved.setEtudiantId(10L);
        saved.setStatus(DemandeStatus.EN_ATTENTE);

        when(securityUtils.getCurrentUserId(principal)).thenReturn(10L);
        when(demandeRepo.save(any(DemandeStage.class))).thenReturn(saved);

        // WHEN
        DemandeStageResponse response = demandeService.soumettre(req, principal);

        // THEN
        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(DemandeStatus.EN_ATTENTE, response.getStatus());

        verify(demandeRepo, times(1)).save(any(DemandeStage.class));
    }
}