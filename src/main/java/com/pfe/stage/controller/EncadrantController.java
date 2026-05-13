package com.pfe.stage.controller;

import com.pfe.stage.client.AuthClient;
import com.pfe.stage.dto.response.EncadrantInfo;
import com.pfe.stage.repository.DemandeStageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/encadrants")
@RequiredArgsConstructor
public class EncadrantController {

    private static final int MAX_ETUDIANTS = 10;

    private final DemandeStageRepository demandeRepo;
    private final AuthClient authClient;

    @GetMapping
    public ResponseEntity<List<EncadrantInfo>> getAllEncadrants() {

        // ✅ Récupère les encadrants avec fullName depuis auth-service
        List<EncadrantInfo> result = authClient.getUsersByRole("ENCADRANT")
                .stream()
                .map(u -> {
                    long nb = demandeRepo.countByEncadrantId(u.getId());
                    int rest = MAX_ETUDIANTS - (int) nb;
                    return new EncadrantInfo(
                            u.getId(),
                            u.getFullName(),   // ← nom réel
                            nb,
                            MAX_ETUDIANTS,
                            rest,
                            nb < MAX_ETUDIANTS
                    );
                })
                .toList();

        return ResponseEntity.ok(result);
    }
}