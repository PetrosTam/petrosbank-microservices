package com.backendev.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserRegistrationResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private Set<String> roles;
}
