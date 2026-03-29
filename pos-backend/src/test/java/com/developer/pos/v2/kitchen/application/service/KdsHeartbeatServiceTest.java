package com.developer.pos.v2.kitchen.application.service;

import com.developer.pos.auth.security.AuthContext;
import com.developer.pos.auth.security.AuthenticatedActor;
import com.developer.pos.v2.common.application.StoreAccessEnforcer;
import com.developer.pos.v2.kitchen.application.dto.StationHeartbeatDto;
import com.developer.pos.v2.kitchen.infrastructure.persistence.entity.KitchenStationEntity;
import com.developer.pos.v2.kitchen.infrastructure.persistence.repository.JpaKitchenStationRepository;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreLookupRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KdsHeartbeatServiceTest {

    @Mock JpaKitchenStationRepository stationRepository;
    @Mock JpaStoreLookupRepository storeLookupRepository;
    MockedStatic<SecurityContextHolder> securityMock;

    @AfterEach
    void tearDown() {
        if (securityMock != null) securityMock.close();
    }

    private KdsHeartbeatService buildService() {
        StoreAccessEnforcer enforcer = new StoreAccessEnforcer(storeLookupRepository);
        return new KdsHeartbeatService(stationRepository, enforcer);
    }

    private void setupActor(Long merchantId, Long storeId) {
        AuthenticatedActor actor = new AuthenticatedActor(
            1L, "kds-device", "KDS001", "KITCHEN_STAFF",
            merchantId, storeId, Set.of(storeId), Set.of("KDS_OPERATE"));
        SecurityContext ctx = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(actor);
        when(ctx.getAuthentication()).thenReturn(auth);
        securityMock = mockStatic(SecurityContextHolder.class);
        securityMock.when(SecurityContextHolder::getContext).thenReturn(ctx);
    }

    private KitchenStationEntity buildStation(Long id, Long storeId, String healthStatus) {
        KitchenStationEntity station = new KitchenStationEntity(storeId, "S01", "Wok Station", 0);
        // Use reflection to set id (protected no-arg constructor hides it)
        try {
            var f = KitchenStationEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(station, id);
            var h = KitchenStationEntity.class.getDeclaredField("kdsHealthStatus");
            h.setAccessible(true);
            h.set(station, healthStatus);
        } catch (Exception e) { throw new RuntimeException(e); }
        return station;
    }

    @Test
    void heartbeat_onlineStation_updatesTimestampAndStaysOnline() {
        setupActor(100L, 10L);
        KitchenStationEntity station = buildStation(1L, 10L, "ONLINE");
        StoreEntity store = mock(StoreEntity.class);
        when(store.getMerchantId()).thenReturn(100L);
        when(stationRepository.findById(1L)).thenReturn(Optional.of(station));
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(store));
        when(stationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StationHeartbeatDto result = buildService().heartbeat(1L);

        assertThat(result.stationId()).isEqualTo(1L);
        assertThat(result.kdsHealthStatus()).isEqualTo("ONLINE");
        assertThat(station.getLastHeartbeatAt()).isNotNull();
        verify(stationRepository).save(station);
    }

    @Test
    void heartbeat_offlineStation_restoresOnline() {
        setupActor(100L, 10L);
        KitchenStationEntity station = buildStation(1L, 10L, "OFFLINE");
        StoreEntity store = mock(StoreEntity.class);
        when(store.getMerchantId()).thenReturn(100L);
        when(stationRepository.findById(1L)).thenReturn(Optional.of(station));
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(store));
        when(stationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StationHeartbeatDto result = buildService().heartbeat(1L);

        assertThat(result.kdsHealthStatus()).isEqualTo("ONLINE");
        assertThat(station.isOnline()).isTrue();
    }

    @Test
    void heartbeat_stationNotFound_throwsIllegalArgument() {
        // No auth setup needed — service throws before reaching enforcer
        when(stationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> buildService().heartbeat(99L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("99");
    }

    @Test
    void heartbeat_wrongMerchant_throwsSecurityException() {
        setupActor(100L, 10L);
        KitchenStationEntity station = buildStation(1L, 10L, "ONLINE");
        StoreEntity store = mock(StoreEntity.class);
        when(store.getMerchantId()).thenReturn(999L); // different merchant
        when(stationRepository.findById(1L)).thenReturn(Optional.of(station));
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(store));

        assertThatThrownBy(() -> buildService().heartbeat(1L))
            .isInstanceOf(SecurityException.class);
    }
}
