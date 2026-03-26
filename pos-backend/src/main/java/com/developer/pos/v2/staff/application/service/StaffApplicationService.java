package com.developer.pos.v2.staff.application.service;

import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.staff.application.command.CreateStaffCommand;
import com.developer.pos.v2.staff.application.command.UpdateStaffCommand;
import com.developer.pos.v2.staff.application.dto.StaffDto;
import com.developer.pos.v2.staff.application.dto.StaffPinVerificationResult;
import com.developer.pos.v2.staff.infrastructure.persistence.entity.RoleEntity;
import com.developer.pos.v2.staff.infrastructure.persistence.entity.RolePermissionEntity;
import com.developer.pos.v2.staff.infrastructure.persistence.entity.StaffEntity;
import com.developer.pos.v2.staff.infrastructure.persistence.repository.JpaRolePermissionRepository;
import com.developer.pos.v2.staff.infrastructure.persistence.repository.JpaRoleRepository;
import com.developer.pos.v2.staff.infrastructure.persistence.repository.JpaStaffRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class StaffApplicationService implements UseCase {

    private final JpaStaffRepository staffRepository;
    private final JpaRoleRepository roleRepository;
    private final JpaRolePermissionRepository rolePermissionRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public StaffApplicationService(JpaStaffRepository staffRepository,
                                   JpaRoleRepository roleRepository,
                                   JpaRolePermissionRepository rolePermissionRepository) {
        this.staffRepository = staffRepository;
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @Transactional
    public StaffDto createStaff(CreateStaffCommand command) {
        staffRepository.findByStoreIdAndStaffCode(command.storeId(), command.staffCode())
                .ifPresent(existing -> {
                    throw new IllegalStateException("Staff code already exists: " + command.staffCode());
                });

        StaffEntity entity = new StaffEntity();
        entity.setStaffId("STF" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        entity.setMerchantId(command.merchantId());
        entity.setStoreId(command.storeId());
        entity.setStaffName(command.staffName());
        entity.setStaffCode(command.staffCode());
        entity.setPinHash(passwordEncoder.encode(command.pin()));
        entity.setRoleCode(command.roleCode());
        entity.setStaffStatus("ACTIVE");
        entity.setPhone(command.phone());
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        StaffEntity saved = staffRepository.save(entity);
        return toDto(saved);
    }

    @Transactional
    public StaffDto updateStaff(UpdateStaffCommand command) {
        StaffEntity entity = staffRepository.findByStaffId(command.staffId())
                .orElseThrow(() -> new IllegalArgumentException("Staff not found: " + command.staffId()));
        if (command.staffName() != null) entity.setStaffName(command.staffName());
        if (command.roleCode() != null) entity.setRoleCode(command.roleCode());
        if (command.phone() != null) entity.setPhone(command.phone());
        if (command.staffStatus() != null) entity.setStaffStatus(command.staffStatus());
        entity.setUpdatedAt(OffsetDateTime.now());
        StaffEntity saved = staffRepository.save(entity);
        return toDto(saved);
    }

    @Transactional(readOnly = true)
    public List<StaffDto> listStaff(Long storeId) {
        return staffRepository.findByStoreIdAndStaffStatusOrderByStaffCodeAsc(storeId, "ACTIVE")
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public StaffDto getStaff(String staffId) {
        StaffEntity entity = staffRepository.findByStaffId(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found: " + staffId));
        return toDto(entity);
    }

    @Transactional(readOnly = true)
    public StaffPinVerificationResult verifyPin(Long storeId, String staffCode, String pin) {
        StaffEntity entity = staffRepository.findByStoreIdAndStaffCode(storeId, staffCode)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found: " + staffCode));
        if (!"ACTIVE".equals(entity.getStaffStatus())) {
            throw new IllegalStateException("Staff is not active.");
        }
        if (!passwordEncoder.matches(pin, entity.getPinHash())) {
            throw new IllegalArgumentException("Invalid PIN.");
        }
        List<String> permissions = getPermissions(entity.getRoleCode());
        return new StaffPinVerificationResult(entity.getStaffId(), entity.getStaffName(), entity.getRoleCode(), permissions);
    }

    @Transactional
    public void deactivateStaff(String staffId) {
        StaffEntity entity = staffRepository.findByStaffId(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found: " + staffId));
        entity.setStaffStatus("INACTIVE");
        entity.setUpdatedAt(OffsetDateTime.now());
        staffRepository.save(entity);
    }

    private List<String> getPermissions(String roleCode) {
        return rolePermissionRepository.findByRoleCode(roleCode)
                .stream().map(RolePermissionEntity::getPermissionCode).toList();
    }

    private StaffDto toDto(StaffEntity entity) {
        String roleName = roleRepository.findByRoleCode(entity.getRoleCode())
                .map(RoleEntity::getRoleName).orElse(entity.getRoleCode());
        List<String> permissions = getPermissions(entity.getRoleCode());
        return new StaffDto(
                entity.getStaffId(), entity.getMerchantId(), entity.getStoreId(),
                entity.getStaffName(), entity.getStaffCode(), entity.getRoleCode(),
                roleName, entity.getStaffStatus(), entity.getPhone(), permissions
        );
    }
}
