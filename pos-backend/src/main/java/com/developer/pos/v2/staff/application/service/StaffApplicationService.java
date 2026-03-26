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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class StaffApplicationService implements UseCase {

    private final JpaStaffRepository staffRepository;
    private final JpaRoleRepository roleRepository;
    private final JpaRolePermissionRepository rolePermissionRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public StaffApplicationService(
            JpaStaffRepository staffRepository,
            JpaRoleRepository roleRepository,
            JpaRolePermissionRepository rolePermissionRepository
    ) {
        this.staffRepository = staffRepository;
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Transactional
    public StaffDto createStaff(CreateStaffCommand command) {
        if (command.staffName() == null || command.staffName().isBlank()) {
            throw new IllegalArgumentException("Staff name must not be blank.");
        }
        if (command.staffCode() == null || command.staffCode().isBlank()) {
            throw new IllegalArgumentException("Staff code must not be blank.");
        }
        if (command.pin() == null || command.pin().length() < 4 || command.pin().length() > 6) {
            throw new IllegalArgumentException("PIN must be 4-6 digits.");
        }

        staffRepository.findByStoreIdAndStaffCode(command.storeId(), command.staffCode().trim())
                .ifPresent(existing -> {
                    throw new IllegalStateException("Staff code already exists in this store: " + command.staffCode());
                });

        String roleCode = command.roleCode() == null || command.roleCode().isBlank()
                ? "CASHIER" : command.roleCode().trim().toUpperCase();
        RoleEntity role = roleRepository.findByRoleCode(roleCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid role code: " + roleCode));

        StaffEntity staff = new StaffEntity();
        staff.setStaffId(UUID.randomUUID().toString());
        staff.setMerchantId(command.merchantId());
        staff.setStoreId(command.storeId());
        staff.setStaffName(command.staffName().trim());
        staff.setStaffCode(command.staffCode().trim());
        staff.setPinHash(passwordEncoder.encode(command.pin()));
        staff.setRoleCode(roleCode);
        staff.setStaffStatus("ACTIVE");
        staff.setPhone(command.phone());
        staff.setCreatedAt(LocalDateTime.now());
        staff.setUpdatedAt(LocalDateTime.now());
        StaffEntity saved = staffRepository.save(staff);

        List<String> permissions = getPermissionsForRole(roleCode);
        return toStaffDto(saved, role.getRoleName(), permissions);
    }

    @Transactional
    public StaffDto updateStaff(UpdateStaffCommand command) {
        StaffEntity staff = staffRepository.findByStaffId(command.staffId())
                .orElseThrow(() -> new IllegalArgumentException("Staff not found: " + command.staffId()));

        if (command.staffName() != null && !command.staffName().isBlank()) {
            staff.setStaffName(command.staffName().trim());
        }
        if (command.phone() != null) {
            staff.setPhone(command.phone().trim());
        }
        if (command.staffStatus() != null && !command.staffStatus().isBlank()) {
            staff.setStaffStatus(command.staffStatus().trim().toUpperCase());
        }
        if (command.roleCode() != null && !command.roleCode().isBlank()) {
            String roleCode = command.roleCode().trim().toUpperCase();
            roleRepository.findByRoleCode(roleCode)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid role code: " + roleCode));
            staff.setRoleCode(roleCode);
        }

        staff.setUpdatedAt(LocalDateTime.now());
        StaffEntity saved = staffRepository.save(staff);

        RoleEntity role = roleRepository.findByRoleCode(saved.getRoleCode()).orElse(null);
        List<String> permissions = getPermissionsForRole(saved.getRoleCode());
        return toStaffDto(saved, role != null ? role.getRoleName() : saved.getRoleCode(), permissions);
    }

    @Transactional(readOnly = true)
    public List<StaffDto> listStaff(Long storeId) {
        return staffRepository.findByStoreIdAndStaffStatus(storeId, "ACTIVE").stream()
                .map(staff -> {
                    RoleEntity role = roleRepository.findByRoleCode(staff.getRoleCode()).orElse(null);
                    List<String> permissions = getPermissionsForRole(staff.getRoleCode());
                    return toStaffDto(staff, role != null ? role.getRoleName() : staff.getRoleCode(), permissions);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public StaffDto getStaff(String staffId) {
        StaffEntity staff = staffRepository.findByStaffId(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found: " + staffId));
        RoleEntity role = roleRepository.findByRoleCode(staff.getRoleCode()).orElse(null);
        List<String> permissions = getPermissionsForRole(staff.getRoleCode());
        return toStaffDto(staff, role != null ? role.getRoleName() : staff.getRoleCode(), permissions);
    }

    @Transactional(readOnly = true)
    public StaffPinVerificationResult verifyPin(Long storeId, String staffCode, String pin) {
        StaffEntity staff = staffRepository.findByStoreIdAndStaffCode(storeId, staffCode)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found with code: " + staffCode));

        if (!"ACTIVE".equals(staff.getStaffStatus())) {
            throw new IllegalStateException("Staff account is inactive.");
        }

        if (!passwordEncoder.matches(pin, staff.getPinHash())) {
            throw new IllegalArgumentException("Invalid PIN.");
        }

        List<String> permissions = getPermissionsForRole(staff.getRoleCode());
        return new StaffPinVerificationResult(
                staff.getStaffId(),
                staff.getStaffName(),
                staff.getRoleCode(),
                permissions
        );
    }

    @Transactional
    public void deactivateStaff(String staffId) {
        StaffEntity staff = staffRepository.findByStaffId(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff not found: " + staffId));
        staff.setStaffStatus("INACTIVE");
        staff.setUpdatedAt(LocalDateTime.now());
        staffRepository.save(staff);
    }

    private List<String> getPermissionsForRole(String roleCode) {
        return rolePermissionRepository.findByRoleCode(roleCode).stream()
                .map(RolePermissionEntity::getPermissionCode)
                .toList();
    }

    private StaffDto toStaffDto(StaffEntity staff, String roleName, List<String> permissions) {
        return new StaffDto(
                staff.getStaffId(),
                staff.getMerchantId(),
                staff.getStoreId(),
                staff.getStaffName(),
                staff.getStaffCode(),
                staff.getRoleCode(),
                roleName,
                staff.getStaffStatus(),
                staff.getPhone(),
                permissions
        );
    }
}
