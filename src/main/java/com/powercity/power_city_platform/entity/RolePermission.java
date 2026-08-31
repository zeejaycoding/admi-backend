package com.powercity.power_city_platform.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "role_permissions",
    uniqueConstraints = @UniqueConstraint(columnNames = {"role_name", "permission_key"})
)
public class RolePermission extends BaseEntity {

    @Column(name = "role_name", nullable = false, length = 100)
    private String roleName;

    @Column(name = "permission_key", nullable = false, length = 100)
    private String permissionKey;

    public RolePermission() {}

    public RolePermission(String roleName, String permissionKey) {
        this.roleName = roleName;
        this.permissionKey = permissionKey;
    }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public String getPermissionKey() { return permissionKey; }
    public void setPermissionKey(String permissionKey) { this.permissionKey = permissionKey; }
}
