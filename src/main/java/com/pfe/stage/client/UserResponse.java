package com.pfe.stage.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserResponse {

    private Long id;
    private String fullName;   
    private String role;

    public String getFullName() {
        return fullName != null ? fullName : "";
    }
}
