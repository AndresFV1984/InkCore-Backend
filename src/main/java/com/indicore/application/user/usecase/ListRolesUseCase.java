package com.indicore.application.user.usecase;

import com.indicore.domain.user.model.Role;
import com.indicore.domain.user.ports.out.RoleRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListRolesUseCase {

    private final RoleRepositoryPort roleRepository;

    public ListRolesUseCase(RoleRepositoryPort roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public List<Role> execute() {
        return roleRepository.findAll();
    }
}
