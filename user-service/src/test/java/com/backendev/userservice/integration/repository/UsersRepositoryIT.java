package com.backendev.userservice.integration.repository;

import com.backendev.userservice.entity.Roles;
import com.backendev.userservice.entity.Users;
import com.backendev.userservice.repository.RolesRepository;
import com.backendev.userservice.repository.UsersRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class UsersRepositoryIT {@Autowired
private UsersRepository usersRepository;

    @Autowired
    private RolesRepository rolesRepository;

    @Test
    void shouldFindUserByEmail() {
        Roles admin = new Roles();
        admin.setName("ADMIN");
        admin = rolesRepository.save(admin);

        Users user = new Users();
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john@test.com");
        user.setPassword("pass123");
        user.getRoles().add(admin);

        usersRepository.save(user);

        Optional<Users> found = usersRepository.findByEmail("john@test.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("john@test.com");
        assertThat(found.get().getRoles()).extracting("name").contains("ADMIN");
    }

    @Test
    void shouldReturnTrueIfEmailExists() {
        Users user = new Users();
        user.setFirstName("Jane");
        user.setLastName("Smith");
        user.setEmail("jane@test.com");
        user.setPassword("pass123");

        usersRepository.save(user);

        assertThat(usersRepository.existsByEmail("jane@test.com")).isTrue();
    }

    @Test
    void shouldReturnFalseIfEmailDoesNotExist() {
        assertThat(usersRepository.existsByEmail("missing@test.com")).isFalse();
    }

}
