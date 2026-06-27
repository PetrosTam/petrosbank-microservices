package com.backendev.userservice.security;

import com.backendev.userservice.entity.Users;
import com.backendev.userservice.repository.UsersRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AppUserServiceDetails implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(AppUserServiceDetails.class);

    private final UsersRepository usersRepository;

    public AppUserServiceDetails(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("User service details for the user {}", email);
         Optional<Users> users = usersRepository.findByEmail(email);
         return users.map(AppUserDetails::new)
                 .orElseThrow(() -> new UsernameNotFoundException("User not found. " + email));
    }
}
