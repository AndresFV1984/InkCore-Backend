package com.inkcore.application.user.usecase;

import com.inkcore.domain.user.model.Permission;
import com.inkcore.domain.user.ports.out.PermissionRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListPermissionsUseCase {

    private final PermissionRepositoryPort permissionRepository;

    public ListPermissionsUseCase(PermissionRepositoryPort permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    @Transactional(readOnly = true)
    public List<Permission> execute() {
        return permissionRepository.findAll();
    }
}
