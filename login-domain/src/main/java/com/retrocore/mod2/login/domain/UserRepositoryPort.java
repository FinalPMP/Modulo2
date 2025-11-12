package com.retrocore.mod2.login.domain;

import java.util.Optional;

public interface UserRepositoryPort {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    User save(User user);
}
