package com.pfe.stage;

import com.pfe.stage.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class StageApplicationTests {

    // ✅ Spring n'instancie plus le vrai FileStorageService
    // qui tente de créer /app/uploads/stage au démarrage
    @MockBean
    private FileStorageService fileStorageService;

    @Test
    void contextLoads() {
    }
}