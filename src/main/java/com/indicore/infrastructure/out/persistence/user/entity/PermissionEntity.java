package com.indicore.infrastructure.out.persistence.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "permissions", schema = "indicolors")
public class PermissionEntity {

    @Id
    @Column(name = "permission_id")
    private UUID permissionId;

    @Column(nullable = false, length = 80, unique = true)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    public PermissionEntity() {
    }

    public UUID getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(UUID permissionId) {
        this.permissionId = permissionId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
