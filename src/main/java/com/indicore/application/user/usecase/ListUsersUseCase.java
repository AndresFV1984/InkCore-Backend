package com.indicore.application.user.usecase;

import com.indicore.domain.user.model.User;
import com.indicore.domain.user.ports.out.UserRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListUsersUseCase {

    private final UserRepositoryPort userRepository;

    public ListUsersUseCase(UserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<User> execute() {
        return userRepository.findAll();
    }
}
