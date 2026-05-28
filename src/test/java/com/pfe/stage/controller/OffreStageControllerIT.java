package com.pfe.stage.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfe.stage.config.TestSecurityConfig;
import com.pfe.stage.dto.request.OffreStageRequest;
import com.pfe.stage.dto.response.OffreStageResponse;
import com.pfe.stage.security.JwtAuthenticationFilter;
import com.pfe.stage.service.OffreStageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = OffreStageController.class)
@Import(TestSecurityConfig.class)
class OffreStageControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /*
     * Le contrôleur n'utilise PAS SecurityUtils — on ne le mocke pas.
     * Le champ s'appelle "offreService" dans le contrôleur mais @MockBean
     * fonctionne par TYPE : Spring injectera ce mock automatiquement.
     */
    @MockBean
    private OffreStageService offreService;

    /*
     * JwtAuthenticationFilter doit exister comme bean pour éviter une
     * NoSuchBeanDefinitionException, mais TestSecurityConfig ne l'ajoute
     * pas à la chaîne → il ne s'exécute jamais pendant les tests.
     */
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // ── helper ────────────────────────────────────────────────────────────────

    private OffreStageResponse buildResponse() {
        return new OffreStageResponse(
                1L, "Stage Java", "Dev Spring Boot", "Tech", "Tunis",
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(40),
                "Informatique", "Java, Spring Boot", true, 1L, LocalDateTime.now()
        );
    }

    private OffreStageRequest buildRequest() {
        OffreStageRequest req = new OffreStageRequest();
        req.setTitre("Stage Java");
        req.setDescription("Dev Spring Boot");
        req.setEntreprise("Tech");
        req.setLieu("Tunis");
        req.setDateDebut(LocalDate.now().plusDays(10));
        req.setDateFin(LocalDate.now().plusDays(40));
        req.setDomaine("Informatique");
        req.setCompetencesRequises("Java, Spring Boot");
        return req;
    }

    // ── GET /api/offres — public ───────────────────────────────────────────────

    @Test
    void getOffresActives_sansAuth_retourne200() throws Exception {
        when(offreService.getOffresActives()).thenReturn(List.of(buildResponse()));

        mockMvc.perform(get("/api/offres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ── GET /api/offres/{id} — public ─────────────────────────────────────────

    @Test
    void getById_sansAuth_retourne200() throws Exception {
        when(offreService.getById(1L)).thenReturn(buildResponse());

        mockMvc.perform(get("/api/offres/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titre").value("Stage Java"));
    }

    // ── GET /api/offres/all — ADMIN ────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN") // fournit l'authentification pour @PreAuthorize
    void getAllOffres_avecRoleAdmin_retourne200() throws Exception {
        when(offreService.getAllOffres()).thenReturn(List.of(buildResponse(), buildResponse()));

        mockMvc.perform(get("/api/offres/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    // Pas de @WithMockUser → utilisateur anonyme → @PreAuthorize bloque
    void getAllOffres_sansAuth_retourne403() throws Exception {
        mockMvc.perform(get("/api/offres/all"))
                .andExpect(status().isForbidden());
    }

    // ── POST /api/offres — ADMIN ───────────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void create_avecRoleAdmin_retourne201() throws Exception {
        when(offreService.create(any(OffreStageRequest.class), any()))
                .thenReturn(buildResponse());

        mockMvc.perform(post("/api/offres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void create_sansAuth_retourne403() throws Exception {
        mockMvc.perform(post("/api/offres")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isForbidden());
    }

    // ── PUT /api/offres/{id} — ADMIN ───────────────────────────────────────────

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void update_avecRoleAdmin_retourne200() throws Exception {
        when(offreService.update(any(Long.class), any(OffreStageRequest.class)))
                .thenReturn(buildResponse());

        mockMvc.perform(put("/api/offres/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isOk());
    }

    // ── PATCH /api/offres/{id}/toggle — ADMIN ─────────────────────────────────

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void toggle_avecRoleAdmin_retourne200() throws Exception {
        doNothing().when(offreService).toggleActive(1L);

        mockMvc.perform(patch("/api/offres/1/toggle"))
                .andExpect(status().isOk());
    }

    // ── DELETE /api/offres/{id} — ADMIN ───────────────────────────────────────

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void delete_avecRoleAdmin_retourne200() throws Exception {
        doNothing().when(offreService).delete(1L);

        mockMvc.perform(delete("/api/offres/1"))
                .andExpect(status().isOk());
    }

    @Test
    void delete_sansAuth_retourne403() throws Exception {
        mockMvc.perform(delete("/api/offres/1"))
                .andExpect(status().isForbidden());
    }
}