package com.powercity.power_city_platform.repository;

import com.powercity.power_city_platform.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    List<RolePermission> findByRoleName(String roleName);

    List<RolePermission> findByRoleNameIn(Set<String> roleNames);

    @Query("SELECT rp.permissionKey FROM RolePermission rp WHERE rp.roleName IN :roleNames")
    Set<String> findPermissionKeysByRoleNames(@Param("roleNames") Set<String> roleNames);

    @Modifying
    @Query("DELETE FROM RolePermission rp WHERE rp.roleName = :roleName")
    void deleteByRoleName(@Param("roleName") String roleName);
}
