package com.backendev.userservice.mapper;

import com.backendev.userservice.dto.UserDTO;
import com.backendev.userservice.dto.UserProfileDTO;
import com.backendev.userservice.dto.UserRegistrationRequest;
import com.backendev.userservice.dto.UserRegistrationResponse;
import com.backendev.userservice.entity.Roles;
import com.backendev.userservice.entity.Users;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDTO toDto(Users users);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "roles", ignore = true)
    Users toEntity(UserRegistrationRequest request);

    @Mapping(target = "roles", source = "roles", qualifiedByName = "rolesToRoleNames")
    UserRegistrationResponse toResponse(Users users);

    @Named("rolesToRoleNames")
    default Set<String> rolesToRoleNames(Set<Roles> roles) {
        return roles.stream()
                .map(Roles::getName)
                .collect(Collectors.toSet());
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "roles", ignore = true)
    void updateUserFromDto(UserProfileDTO userProfileDTO, @MappingTarget Users users);

}
