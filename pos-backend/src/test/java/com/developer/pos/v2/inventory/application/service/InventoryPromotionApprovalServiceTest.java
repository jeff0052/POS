package com.developer.pos.v2.inventory.application.service;

import com.developer.pos.auth.security.AuthenticatedActor;
import com.developer.pos.v2.common.application.StoreAccessEnforcer;
import com.developer.pos.v2.inventory.application.dto.InventoryDrivenPromotionDto;
import com.developer.pos.v2.inventory.infrastructure.persistence.entity.InventoryDrivenPromotionEntity;
import com.developer.pos.v2.inventory.infrastructure.persistence.repository.JpaInventoryDrivenPromotionRepository;
import com.developer.pos.v2.promotion.infrastructure.persistence.entity.PromotionRuleEntity;
import com.developer.pos.v2.promotion.infrastructure.persistence.repository.JpaPromotionRuleConditionRepository;
import com.developer.pos.v2.promotion.infrastructure.persistence.repository.JpaPromotionRuleRepository;
import com.developer.pos.v2.promotion.infrastructure.persistence.repository.JpaPromotionRuleRewardRepository;
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

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryPromotionApprovalServiceTest {

    @Mock JpaInventoryDrivenPromotionRepository promotionRepository;
    @Mock JpaPromotionRuleRepository ruleRepository;
    @Mock JpaPromotionRuleConditionRepository conditionRepository;
    @Mock JpaPromotionRuleRewardRepository rewardRepository;
    @Mock JpaStoreLookupRepository storeLookupRepository;

    MockedStatic<SecurityContextHolder> securityMock;

    @AfterEach
    void tearDown() {
        if (securityMock != null) securityMock.close();
    }

    private InventoryPromotionApprovalService buildService() {
        StoreAccessEnforcer enforcer = new StoreAccessEnforcer(storeLookupRepository);
        return new InventoryPromotionApprovalService(
            promotionRepository, ruleRepository, conditionRepository, rewardRepository, enforcer);
    }

    private void setupActor(Long merchantId, Long storeId, Set<String> permissions) {
        AuthenticatedActor actor = new AuthenticatedActor(
            1L, "manager", "M001", "STORE_MANAGER",
            merchantId, storeId, Set.of(storeId), permissions);
        SecurityContext ctx = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getPrincipal()).thenReturn(actor);
        lenient().when(ctx.getAuthentication()).thenReturn(auth);
        securityMock = mockStatic(SecurityContextHolder.class);
        securityMock.when(SecurityContextHolder::getContext).thenReturn(ctx);
    }

    private StoreEntity buildStore(Long merchantId) {
        try {
            java.lang.reflect.Constructor<StoreEntity> ctor = StoreEntity.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            StoreEntity store = ctor.newInstance();
            setField(store, "merchantId", merchantId);
            return store;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private InventoryDrivenPromotionEntity makeDraftEntity(Long id, Long storeId, String status) {
        InventoryDrivenPromotionEntity entity = new InventoryDrivenPromotionEntity(
            storeId, 100L, 10L, "NEAR_EXPIRY", new BigDecimal("20.00"),
            "[50]", LocalDateTime.now().plusDays(7));
        try {
            setField(entity, "id", id);
            if (!"DRAFT".equals(status)) {
                setField(entity, "draftStatus", status);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return entity;
    }

    private void setField(Object obj, String name, Object value) throws Exception {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, value);
    }

    @Test
    void approveDraft_createsPromotionRuleAndMarksApproved() {
        setupActor(5L, 10L, Set.of("PROMOTION_APPROVE"));
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(buildStore(5L)));

        InventoryDrivenPromotionEntity draft = makeDraftEntity(42L, 10L, "DRAFT");
        when(promotionRepository.findById(42L)).thenReturn(Optional.of(draft));

        PromotionRuleEntity savedRule = new PromotionRuleEntity();
        savedRule.setId(99L);
        when(ruleRepository.save(any())).thenReturn(savedRule);
        when(conditionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(rewardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(promotionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InventoryDrivenPromotionDto result = buildService().approveDraft(10L, 42L);

        assertThat(result.draftStatus()).isEqualTo("APPROVED");
        assertThat(result.promotionRuleId()).isEqualTo(99L);
        verify(ruleRepository).save(any());
        verify(conditionRepository).save(any());
        verify(rewardRepository).save(any());
    }

    @Test
    void approveDraft_wrongStore_throwsSecurity() {
        setupActor(5L, 10L, Set.of("PROMOTION_APPROVE"));
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(buildStore(5L)));

        // Draft belongs to store 99, but we call with store 10
        InventoryDrivenPromotionEntity draft = makeDraftEntity(42L, 99L, "DRAFT");
        when(promotionRepository.findById(42L)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> buildService().approveDraft(10L, 42L))
            .isInstanceOf(SecurityException.class);
    }

    @Test
    void rejectDraft_marksRejected() {
        setupActor(5L, 10L, Set.of("PROMOTION_APPROVE"));
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(buildStore(5L)));

        InventoryDrivenPromotionEntity draft = makeDraftEntity(42L, 10L, "DRAFT");
        when(promotionRepository.findById(42L)).thenReturn(Optional.of(draft));
        when(promotionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InventoryDrivenPromotionDto result = buildService().rejectDraft(10L, 42L);

        assertThat(result.draftStatus()).isEqualTo("REJECTED");
    }

    @Test
    void approveDraft_notDraft_throwsIllegalState() {
        setupActor(5L, 10L, Set.of("PROMOTION_APPROVE"));
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(buildStore(5L)));

        InventoryDrivenPromotionEntity draft = makeDraftEntity(42L, 10L, "REJECTED");
        when(promotionRepository.findById(42L)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> buildService().approveDraft(10L, 42L))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void listDrafts_returnsDraftsByStatus() {
        setupActor(5L, 10L, Set.of("INVENTORY_VIEW"));
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(buildStore(5L)));

        InventoryDrivenPromotionEntity draft1 = makeDraftEntity(1L, 10L, "DRAFT");
        InventoryDrivenPromotionEntity draft2 = makeDraftEntity(2L, 10L, "DRAFT");
        when(promotionRepository.findByStoreIdAndDraftStatusOrderByCreatedAtDesc(10L, "DRAFT"))
            .thenReturn(List.of(draft1, draft2));

        List<InventoryDrivenPromotionDto> results = buildService().listDrafts(10L, "DRAFT");

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(d -> "DRAFT".equals(d.draftStatus()));
    }

    @Test
    void listDrafts_noStatusFilter_returnsAll() {
        setupActor(5L, 10L, Set.of("INVENTORY_VIEW"));
        when(storeLookupRepository.findById(10L)).thenReturn(Optional.of(buildStore(5L)));

        InventoryDrivenPromotionEntity draft = makeDraftEntity(1L, 10L, "DRAFT");
        InventoryDrivenPromotionEntity approved = makeDraftEntity(2L, 10L, "APPROVED");
        when(promotionRepository.findByStoreIdOrderByCreatedAtDesc(10L))
            .thenReturn(List.of(draft, approved));

        List<InventoryDrivenPromotionDto> results = buildService().listDrafts(10L, null);

        assertThat(results).hasSize(2);
    }
}
