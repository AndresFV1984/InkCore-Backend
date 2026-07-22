package com.inkcore.domain.user.ports.out;

import com.inkcore.domain.user.model.Permission;

import java.util.Collection;
import java.util.List;

public interface PermissionRepositoryPort {

    List<Permission> findAll();

    List<Permission> findByCodes(Collection<String> codes);
}
