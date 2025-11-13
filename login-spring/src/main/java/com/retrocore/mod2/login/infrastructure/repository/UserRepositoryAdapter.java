package com.retrocore.mod2.login.infrastructure.repository;

import com.retrocore.mod2.login.domain.User;
import com.retrocore.mod2.login.domain.UserRepositoryPort;
import com.retrocore.mod2.login.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final JpaUserRepository jpaUserRepository;
    private final UserMapper userMapper;

    @Override
    public Optional<User> findByUsername(String username) {
        return jpaUserRepository.findByUsername(username)
                .map(userMapper::toDomain);
    }

    @Override
    public boolean existsByUsername(String username) {
        return jpaUserRepository.existsByUsername(username);
    }

    @Override
    public User save(User user) {
        var entity = userMapper.toEntity(user);
        var saved = jpaUserRepository.save(entity);
        return userMapper.toDomain(saved);
    }
}
