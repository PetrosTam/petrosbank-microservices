package com.backendev.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserRegistrationRequest {
    @NotBlank(message = "The name field cannot be blank.")
    private String firstName;
    @NotBlank(message = "The field cannot be blank.")
    private String lastName;
    @Email(message = "Invalid email. Enter a valid email-id.")
    @NotBlank(message = "The email field cannot be blank.")
    private String email;
    @NotBlank(message = "The password field cannot be blank.")
    private String password;
    @NotBlank(message = "The phone number field cannot be blank.")
    private String phone;
    private Set<String> roles;
}
