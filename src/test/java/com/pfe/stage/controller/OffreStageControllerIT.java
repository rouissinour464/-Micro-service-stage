package com.pfe.stage.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfe.stage.dto.request.OffreStageRequest;
import com.pfe.stage.dto.response.OffreStageResponse;
import com.pfe.stage.security.JwtAuthenticationFilter;
import com.pfe.stage.security.SecurityUtils;
import com.pfe.stage.service.OffreStageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OffreStageController.class)
class OffreStageControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OffreStageService offreStageService;

    @MockBean
    private SecurityUtils securityUtils;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void creer_offre_admin_ok() throws Exception {

        when(securityUtils.getCurrentUserId(any(Principal.class)))
                .thenReturn(1L);

        OffreStageResponse response = new OffreStageResponse(
                1L,
                "Stage Java",
                "Développement Spring Boot",
                "Tech",
                "Tunis",
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(40),
                "Informatique",
                "Java, Spring Boot",
                true,
                1L,
                LocalDateTime.now()
        );

        when(offreStageService.create(any(OffreStageRequest.class), any()))
                .thenReturn(response);

        OffreStageRequest req = new OffreStageRequest();
        req.setTitre("Stage Java");
        req.setDescription("Développement Spring Boot");
        req.setEntreprise("Tech");
        req.setLieu("Tunis");
        req.setDateDebut(LocalDate.now().plusDays(10));
        req.setDateFin(LocalDate.now().plusDays(40));
        req.setDomaine("Informatique");
        req.setCompetencesRequises("Java, Spring Boot");

        mockMvc.perform(
                        post("/api/offres")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req))
                )
                .andExpect(status().isCreated());
    }
}