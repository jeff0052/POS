package com.developer.pos.v2.kitchen.application.service;

import com.developer.pos.auth.security.AuthenticatedActor;
import com.developer.pos.v2.common.application.StoreAccessEnforcer;
import com.developer.pos.v2.kitchen.application.dto.KitchenTicketDto;
import com.developer.pos.v2.kitchen.infrastructure.persistence.entity.KitchenTicketEntity;
import com.developer.pos.v2.kitchen.infrastructure.persistence.repository.JpaKitchenTicketRepository;
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

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketStatusServiceTest {

    @Mock JpaKitchenTicketRepository ticketRepository;
    @Mock JpaStoreLookupRepository storeLookupRepository;
    MockedStatic<SecurityContextHolder> securityMock;

    @AfterEach
    void tearDown() {
        if (securityMock != null) securityMock.close();
    }

    private TicketStatusService buildService() {
        StoreAccessEnforcer enforcer = new StoreAccessEnforcer(storeLookupRepository);
        return new TicketStatusService(ticketRepository, enforcer);
    }

    private void setupActor(Set<String> permissions) {
        AuthenticatedActor actor = new AuthenticatedActor(
            1L, "staff", "S001", "KITCHEN_STAFF",
            100L, 10L, Set.of(10L), permissions);
        SecurityContext ctx = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(actor);
        when(ctx.getAuthentication()).thenReturn(auth);
        securityMock = mockStatic(SecurityContextHolder.class);
        securityMock.when(SecurityContextHolder::getContext).thenReturn(ctx);
    }

    private KitchenTicketEntity buildTicket(Long id, Long storeId, String currentStatus) {
        KitchenTicketEntity ticket = new KitchenTicketEntity("KT-001", storeId, 5L, "T01", 1L, 100L, 1);
        try {
            var f = KitchenTicketEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(ticket, id);
            var s = KitchenTicketEntity.class.getDeclaredField("ticketStatus");
            s.setAccessible(true);
            s.set(ticket, currentStatus);
        } catch (Exception e) { throw new RuntimeException(e); }
        return ticket;
    }

    private StoreEntity buildStore(Long merchantId) {
        StoreEntity store = mock(StoreEntity.class);
        when(store.getMerchantId()).thenReturn(merchantId);
        return store;
    }

    @Test
    void updateStatus_submittedToPreparing_succeeds() {
        setupActor(Set.of("KDS_OPERATE"));
        KitchenTicketEntity ticket = buildTicket(1L, 10L, "SUBMITTED");
        StoreEntity store1 = buildStore(100L);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(store1));
        when(ticketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        KitchenTicketDto result = buildService().updateStatus(1L, "PREPARING");

        assertThat(result.ticketStatus()).isEqualTo("PREPARING");
        assertThat(result.startedAt()).isNotNull();
    }

    @Test
    void updateStatus_preparingToReady_setsReadyAt() {
        setupActor(Set.of("KDS_OPERATE"));
        KitchenTicketEntity ticket = buildTicket(1L, 10L, "PREPARING");
        StoreEntity store2 = buildStore(100L);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(store2));
        when(ticketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        KitchenTicketDto result = buildService().updateStatus(1L, "READY");

        assertThat(result.ticketStatus()).isEqualTo("READY");
        assertThat(result.readyAt()).isNotNull();
    }

    @Test
    void updateStatus_readyToServed_setsServedAt() {
        setupActor(Set.of("KDS_OPERATE"));
        KitchenTicketEntity ticket = buildTicket(1L, 10L, "READY");
        StoreEntity store3 = buildStore(100L);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(store3));
        when(ticketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        KitchenTicketDto result = buildService().updateStatus(1L, "SERVED");

        assertThat(result.ticketStatus()).isEqualTo("SERVED");
        assertThat(result.servedAt()).isNotNull();
    }

    @Test
    void updateStatus_cancelRequiresTicketCancelPermission() {
        setupActor(Set.of("KDS_OPERATE")); // no TICKET_CANCEL
        KitchenTicketEntity ticket = buildTicket(1L, 10L, "SUBMITTED");
        StoreEntity store4 = buildStore(100L);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(store4));

        assertThatThrownBy(() -> buildService().updateStatus(1L, "CANCELLED"))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("TICKET_CANCEL");
    }

    @Test
    void updateStatus_invalidTransition_throws422() {
        setupActor(Set.of("KDS_OPERATE"));
        KitchenTicketEntity ticket = buildTicket(1L, 10L, "READY");
        StoreEntity store5 = buildStore(100L);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(store5));

        // READY → PREPARING is invalid
        assertThatThrownBy(() -> buildService().updateStatus(1L, "PREPARING"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void updateStatus_ticketNotFound_throwsIllegalArgument() {
        // No auth setup needed — service throws before reaching enforcer
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> buildService().updateStatus(99L, "PREPARING"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("99");
    }

    @Test
    void updateStatus_terminalState_throwsIllegalState() {
        setupActor(Set.of("KDS_OPERATE", "TICKET_CANCEL"));
        KitchenTicketEntity ticket = buildTicket(1L, 10L, "SERVED");
        StoreEntity store6 = buildStore(100L);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(store6));

        assertThatThrownBy(() -> buildService().updateStatus(1L, "PREPARING"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("terminal");
    }
}
