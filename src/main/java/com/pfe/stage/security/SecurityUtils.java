package com.pfe.stage.security;

import org.springframework.stereotype.Component;
import java.security.Principal;

@Component
public class SecurityUtils {

    public Long getCurrentUserId(Principal principal) {

        if (principal == null || principal.getName() == null) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }

        try {
            // ✅ subject du JWT = userId
            return Long.parseLong(principal.getName());
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                "JWT invalide : subject != userId"
            );
        }
    }
}
