package com.backendev.userservice.integration.repository;

import com.backendev.userservice.entity.Roles;
import com.backendev.userservice.repository.RolesRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class RolesRepositoryIT {

    @Autowired
    private RolesRepository rolesRepository;

    @Test
    void shouldFindRoleByName() {
        Roles role = new Roles();
        role.setName("ADMIN");
        rolesRepository.save(role);

        Optional<Roles> found = rolesRepository.findByName("ADMIN");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("ADMIN");
    }

    @Test
    void shouldReturnEmptyIfRoleNotFound() {
        Optional<Roles> result = rolesRepository.findByName("UNKNOWN");
        assertThat(result).isNotPresent();
    }

}
