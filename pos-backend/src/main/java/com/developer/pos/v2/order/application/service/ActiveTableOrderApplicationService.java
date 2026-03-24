package com.developer.pos.v2.order.application.service;

import com.developer.pos.v2.catalog.domain.model.SkuRef;
import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.member.domain.policy.MemberDiscountPolicy;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberRepository;
import com.developer.pos.v2.order.application.command.ReplaceActiveTableOrderItemsCommand;
import com.developer.pos.v2.order.application.command.SubmitQrOrderingCommand;
import com.developer.pos.v2.order.application.dto.ActiveTableOrderDto;
import com.developer.pos.v2.order.application.dto.OrderStageTransitionDto;
import com.developer.pos.v2.order.application.dto.QrOrderingContextDto;
import com.developer.pos.v2.order.application.dto.QrOrderingSubmitResultDto;
import com.developer.pos.v2.order.application.dto.SubmittedOrderDto;
import com.developer.pos.v2.order.application.query.GetActiveTableOrderQuery;
import com.developer.pos.v2.order.domain.source.OrderSource;
import com.developer.pos.v2.order.domain.status.ActiveOrderStatus;
import com.developer.pos.v2.order.domain.model.ActiveTableOrder;
import com.developer.pos.v2.order.domain.model.ActiveTableOrderItem;
import com.developer.pos.v2.order.infrastructure.persistence.entity.ActiveTableOrderEntity;
import com.developer.pos.v2.order.infrastructure.persistence.entity.ActiveTableOrderItemEntity;
import com.developer.pos.v2.order.infrastructure.persistence.entity.SubmittedOrderEntity;
import com.developer.pos.v2.order.infrastructure.persistence.entity.SubmittedOrderItemEntity;
import com.developer.pos.v2.order.infrastructure.persistence.entity.TableSessionEntity;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaActiveTableOrderRepository;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaSubmittedOrderRepository;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaTableSessionRepository;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreEntity;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreTableEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreLookupRepository;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreRepository;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreTableRepository;
import com.developer.pos.v2.store.domain.model.TableRef;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.time.OffsetDateTime;

@Service
public class ActiveTableOrderApplicationService implements UseCase {

    private final JpaActiveTableOrderRepository activeTableOrderRepository;
    private final JpaStoreRepository storeRepository;
    private final JpaStoreLookupRepository storeLookupRepository;
    private final JpaStoreTableRepository storeTableRepository;
    private final JpaMemberRepository memberRepository;
    private final JpaTableSessionRepository tableSessionRepository;
    private final JpaSubmittedOrderRepository submittedOrderRepository;

    public ActiveTableOrderApplicationService(
            JpaActiveTableOrderRepository activeTableOrderRepository,
            JpaStoreRepository storeRepository,
            JpaStoreLookupRepository storeLookupRepository,
            JpaStoreTableRepository storeTableRepository,
            JpaMemberRepository memberRepository,
            JpaTableSessionRepository tableSessionRepository,
            JpaSubmittedOrderRepository submittedOrderRepository
    ) {
        this.activeTableOrderRepository = activeTableOrderRepository;
        this.storeRepository = storeRepository;
        this.storeLookupRepository = storeLookupRepository;
        this.storeTableRepository = storeTableRepository;
        this.memberRepository = memberRepository;
        this.tableSessionRepository = tableSessionRepository;
        this.submittedOrderRepository = submittedOrderRepository;
    }

    @Transactional(readOnly = true)
    public ActiveTableOrderDto getActiveTableOrder(GetActiveTableOrderQuery query) {
        return activeTableOrderRepository.findByStoreIdAndTableId(query.storeId(), query.tableId())
                .filter(entity -> entity.getStatus() != ActiveOrderStatus.SETTLED)
                .map(this::toDto)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public QrOrderingContextDto getQrOrderingContext(String storeCode, String tableCode) {
        StoreEntity store = storeLookupRepository.findByStoreCode(storeCode)
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeCode));
        StoreTableEntity table = storeTableRepository.findByStoreIdAndTableCode(store.getId(), tableCode)
                .orElseThrow(() -> new IllegalArgumentException("Table not found: " + tableCode));

        ActiveTableOrderDto currentActiveOrder = activeTableOrderRepository.findByStoreIdAndTableId(store.getId(), table.getId())
                .filter(entity -> entity.getStatus() != ActiveOrderStatus.SETTLED)
                .map(this::toDto)
                .orElse(null);
        List<SubmittedOrderDto> submittedOrders = getSubmittedOrders(store.getId(), table.getId());

        return new QrOrderingContextDto(
                store.getId(),
                store.getStoreCode(),
                store.getStoreName(),
                table.getId(),
                table.getTableCode(),
                table.getTableName(),
                table.getTableStatus(),
                currentActiveOrder,
                submittedOrders
        );
    }

    @Transactional(readOnly = true)
    public List<SubmittedOrderDto> getSubmittedOrders(Long storeId, Long tableId) {
        return tableSessionRepository.findFirstByStoreIdAndTableIdAndSessionStatusOrderByIdDesc(storeId, tableId, "OPEN")
                .map(session -> submittedOrderRepository.findByTableSessionIdAndSettlementStatusOrderByIdAsc(session.getId(), "UNPAID")
                        .stream()
                        .map(this::toSubmittedDto)
                        .toList())
                .orElse(List.of());
    }

    @Transactional
    public ActiveTableOrderDto replaceItems(ReplaceActiveTableOrderItemsCommand command) {
        StoreEntity store = storeRepository.findById(command.storeId())
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + command.storeId()));
        StoreTableEntity table = storeTableRepository.findByIdAndStoreId(command.tableId(), command.storeId())
                .orElseThrow(() -> new IllegalArgumentException("Table not found in store: " + command.tableId()));

        ActiveTableOrderEntity entity = activeTableOrderRepository.findByStoreIdAndTableId(command.storeId(), command.tableId())
                .orElseGet(() -> createEntity(store, table, command));

        entity.setOrderSource(command.orderSource());
        entity.setMemberId(command.memberId());
        if (entity.getStatus() == null || entity.getStatus() == ActiveOrderStatus.SETTLED) {
            entity.setStatus(ActiveOrderStatus.DRAFT);
        }

        List<ActiveTableOrderItemEntity> items = command.items().stream()
                .map(item -> {
                    ActiveTableOrderItemEntity next = new ActiveTableOrderItemEntity();
                    next.setSkuId(item.skuId());
                    next.setSkuCodeSnapshot(item.skuCode());
                    next.setSkuNameSnapshot(item.skuName());
                    next.setQuantity(item.quantity());
                    next.setUnitPriceSnapshotCents(item.unitPriceCents());
                    next.setItemRemark(item.remark());
                    next.setLineTotalCents(item.unitPriceCents() * item.quantity());
                    return next;
                })
                .toList();

        entity.replaceItems(items);

        long originalAmount = items.stream()
                .mapToLong(ActiveTableOrderItemEntity::getLineTotalCents)
                .sum();

        entity.setOriginalAmountCents(originalAmount);
        long memberDiscount = resolveMemberDiscount(entity.getMemberId(), originalAmount);
        entity.setMemberDiscountCents(memberDiscount);
        entity.setPromotionDiscountCents(0);
        entity.setPayableAmountCents(Math.max(0, originalAmount - memberDiscount));
        entity.setStatus(ActiveOrderStatus.DRAFT);
        ActiveTableOrderEntity saved = activeTableOrderRepository.save(entity);
        table.setTableStatus("ORDERING");
        storeTableRepository.save(table);
        return toDto(saved);
    }

    @Transactional
    public QrOrderingSubmitResultDto submitQrOrdering(SubmitQrOrderingCommand command) {
        StoreEntity store = storeLookupRepository.findByStoreCode(command.storeCode())
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + command.storeCode()));
        StoreTableEntity table = storeTableRepository.findByStoreIdAndTableCode(store.getId(), command.tableCode())
                .orElseThrow(() -> new IllegalArgumentException("Table not found: " + command.tableCode()));

        ActiveTableOrderEntity entity = activeTableOrderRepository.findByStoreIdAndTableId(store.getId(), table.getId())
                .filter(existing -> existing.getStatus() != ActiveOrderStatus.SETTLED)
                .orElseGet(() -> createEntity(store, table, new ReplaceActiveTableOrderItemsCommand(
                        store.getId(),
                        table.getId(),
                        table.getTableCode(),
                        OrderSource.QR,
                        command.memberId(),
                        List.of()
                )));

        entity.setOrderSource(OrderSource.QR);
        if (command.memberId() != null) {
            entity.setMemberId(command.memberId());
        }
        if (entity.getStatus() == null || entity.getStatus() == ActiveOrderStatus.SETTLED) {
            entity.setStatus(ActiveOrderStatus.SUBMITTED);
        }

        List<ActiveTableOrderItemEntity> mergedItems = mergeQrItems(entity.getItems(), command.items());
        entity.replaceItems(mergedItems);

        long originalAmount = mergedItems.stream()
                .mapToLong(ActiveTableOrderItemEntity::getLineTotalCents)
                .sum();

        entity.setOriginalAmountCents(originalAmount);
        long memberDiscount = resolveMemberDiscount(entity.getMemberId(), originalAmount);
        entity.setMemberDiscountCents(memberDiscount);
        entity.setPromotionDiscountCents(0);
        entity.setPayableAmountCents(Math.max(0, originalAmount - memberDiscount));
        entity.setStatus(ActiveOrderStatus.SUBMITTED);

        ActiveTableOrderEntity saved = activeTableOrderRepository.save(entity);
        table.setTableStatus("DINING");
        storeTableRepository.save(table);
        persistSubmittedOrder(
                store,
                table,
                OrderSource.QR,
                entity.getMemberId(),
                entity.getActiveOrderId(),
                command.items().stream().map(item -> {
                    ActiveTableOrderItemEntity next = new ActiveTableOrderItemEntity();
                    next.setSkuId(item.skuId());
                    next.setSkuCodeSnapshot(item.skuCode());
                    next.setSkuNameSnapshot(item.skuName());
                    next.setQuantity(item.quantity());
                    next.setUnitPriceSnapshotCents(item.unitPriceCents());
                    next.setItemRemark(item.remark());
                    next.setLineTotalCents(item.unitPriceCents() * item.quantity());
                    return next;
                }).toList(),
                originalAmount,
                memberDiscount,
                entity.getPromotionDiscountCents(),
                Math.max(0, originalAmount - memberDiscount - entity.getPromotionDiscountCents())
        );

        int totalItemCount = saved.getItems().stream()
                .mapToInt(ActiveTableOrderItemEntity::getQuantity)
                .sum();

        return new QrOrderingSubmitResultDto(
                saved.getActiveOrderId(),
                saved.getOrderNo(),
                table.getTableCode(),
                saved.getStatus().name(),
                saved.getPayableAmountCents(),
                totalItemCount
        );
    }

    @Transactional
    public OrderStageTransitionDto moveToSettlement(String activeOrderId) {
        ActiveTableOrderEntity entity = activeTableOrderRepository.findByActiveOrderId(activeOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Active order not found: " + activeOrderId));

        if (entity.getStatus() != ActiveOrderStatus.DRAFT && entity.getStatus() != ActiveOrderStatus.SUBMITTED) {
            throw new IllegalStateException("Only draft or submitted orders can move to settlement.");
        }

        entity.setStatus(ActiveOrderStatus.PENDING_SETTLEMENT);
        ActiveTableOrderEntity saved = activeTableOrderRepository.save(entity);

        StoreTableEntity table = storeTableRepository.findByIdAndStoreId(saved.getTableId(), saved.getStoreId())
                .orElseThrow(() -> new IllegalStateException("Table missing for active order: " + saved.getId()));
        table.setTableStatus("PENDING_SETTLEMENT");
        storeTableRepository.save(table);

        return new OrderStageTransitionDto(saved.getActiveOrderId(), saved.getStatus().name());
    }

    @Transactional
    public OrderStageTransitionDto submitToKitchen(String activeOrderId) {
        ActiveTableOrderEntity entity = activeTableOrderRepository.findByActiveOrderId(activeOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Active order not found: " + activeOrderId));

        if (entity.getStatus() != ActiveOrderStatus.DRAFT) {
            throw new IllegalStateException("Only draft orders can be submitted to kitchen.");
        }

        entity.setStatus(ActiveOrderStatus.SUBMITTED);
        ActiveTableOrderEntity saved = activeTableOrderRepository.save(entity);

        StoreTableEntity table = storeTableRepository.findByIdAndStoreId(saved.getTableId(), saved.getStoreId())
                .orElseThrow(() -> new IllegalStateException("Table missing for active order: " + saved.getId()));
        table.setTableStatus("DINING");
        storeTableRepository.save(table);
        StoreEntity store = storeRepository.findById(saved.getStoreId())
                .orElseThrow(() -> new IllegalStateException("Store missing for active order: " + saved.getId()));
        persistSubmittedOrder(
                store,
                table,
                saved.getOrderSource(),
                saved.getMemberId(),
                saved.getActiveOrderId(),
                saved.getItems(),
                saved.getOriginalAmountCents(),
                saved.getMemberDiscountCents(),
                saved.getPromotionDiscountCents(),
                saved.getPayableAmountCents()
        );
        activeTableOrderRepository.delete(saved);

        return new OrderStageTransitionDto(saved.getActiveOrderId(), saved.getStatus().name());
    }

    @Transactional
    public void deleteEmptyDraft(String activeOrderId) {
        ActiveTableOrderEntity entity = activeTableOrderRepository.findByActiveOrderId(activeOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Active order not found: " + activeOrderId));

        if (entity.getStatus() != ActiveOrderStatus.DRAFT) {
            throw new IllegalStateException("Only draft active orders can be deleted.");
        }

        if (!entity.getItems().isEmpty()) {
            throw new IllegalStateException("Only empty draft active orders can be deleted.");
        }

        StoreTableEntity table = storeTableRepository.findByIdAndStoreId(entity.getTableId(), entity.getStoreId())
                .orElseThrow(() -> new IllegalStateException("Table missing for active order: " + entity.getId()));

        activeTableOrderRepository.delete(entity);

        table.setTableStatus("AVAILABLE");
        storeTableRepository.save(table);
    }

    private ActiveTableOrderEntity createEntity(
            StoreEntity store,
            StoreTableEntity table,
            ReplaceActiveTableOrderItemsCommand command
    ) {
        ActiveTableOrderEntity entity = new ActiveTableOrderEntity();
        entity.setActiveOrderId("ato_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        entity.setOrderNo("ATO" + System.currentTimeMillis());
        entity.setMerchantId(store.getMerchantId());
        entity.setStoreId(store.getId());
        entity.setTableId(table.getId());
        entity.setOrderSource(command.orderSource());
        entity.setDiningType("DINE_IN");
        entity.setStatus(ActiveOrderStatus.DRAFT);
        return entity;
    }

    private List<ActiveTableOrderItemEntity> mergeQrItems(
            List<ActiveTableOrderItemEntity> existingItems,
            List<SubmitQrOrderingCommand.SubmitQrOrderingItemInput> incomingItems
    ) {
        List<ActiveTableOrderItemEntity> merged = existingItems.stream()
                .map(existing -> {
                    ActiveTableOrderItemEntity copy = new ActiveTableOrderItemEntity();
                    copy.setSkuId(existing.getSkuId());
                    copy.setSkuCodeSnapshot(existing.getSkuCodeSnapshot());
                    copy.setSkuNameSnapshot(existing.getSkuNameSnapshot());
                    copy.setQuantity(existing.getQuantity());
                    copy.setUnitPriceSnapshotCents(existing.getUnitPriceSnapshotCents());
                    copy.setMemberPriceSnapshotCents(existing.getMemberPriceSnapshotCents());
                    copy.setItemRemark(existing.getItemRemark());
                    copy.setLineTotalCents(existing.getLineTotalCents());
                    return copy;
                })
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));

        for (SubmitQrOrderingCommand.SubmitQrOrderingItemInput incoming : incomingItems) {
            ActiveTableOrderItemEntity matched = merged.stream()
                    .filter(item -> item.getSkuId().equals(incoming.skuId())
                            && java.util.Objects.equals(item.getItemRemark(), incoming.remark()))
                    .findFirst()
                    .orElse(null);

            if (matched == null) {
                ActiveTableOrderItemEntity next = new ActiveTableOrderItemEntity();
                next.setSkuId(incoming.skuId());
                next.setSkuCodeSnapshot(incoming.skuCode());
                next.setSkuNameSnapshot(incoming.skuName());
                next.setQuantity(incoming.quantity());
                next.setUnitPriceSnapshotCents(incoming.unitPriceCents());
                next.setItemRemark(incoming.remark());
                next.setLineTotalCents(incoming.unitPriceCents() * incoming.quantity());
                merged.add(next);
            } else {
                int nextQuantity = matched.getQuantity() + incoming.quantity();
                matched.setQuantity(nextQuantity);
                matched.setLineTotalCents(matched.getUnitPriceSnapshotCents() * nextQuantity);
            }
        }

        return merged;
    }

    private long resolveMemberDiscount(Long memberId, long originalAmountCents) {
        if (memberId == null) {
            return 0;
        }

        return memberRepository.findById(memberId)
                .map(member -> MemberDiscountPolicy.calculate(originalAmountCents, member.getTierCode()))
                .orElse(0L);
    }

    private TableSessionEntity findOrCreateOpenSession(StoreEntity store, StoreTableEntity table) {
        return tableSessionRepository.findFirstByStoreIdAndTableIdAndSessionStatusOrderByIdDesc(store.getId(), table.getId(), "OPEN")
                .orElseGet(() -> {
                    TableSessionEntity session = new TableSessionEntity();
                    session.setSessionId("TS" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
                    session.setMerchantId(store.getMerchantId());
                    session.setStoreId(store.getId());
                    session.setTableId(table.getId());
                    session.setSessionStatus("OPEN");
                    return tableSessionRepository.save(session);
                });
    }

    private void persistSubmittedOrder(
            StoreEntity store,
            StoreTableEntity table,
            OrderSource source,
            Long memberId,
            String sourceActiveOrderId,
            List<ActiveTableOrderItemEntity> items,
            long originalAmountCents,
            long memberDiscountCents,
            long promotionDiscountCents,
            long payableAmountCents
    ) {
        if (items.isEmpty()) {
            return;
        }

        TableSessionEntity session = findOrCreateOpenSession(store, table);
        SubmittedOrderEntity submittedOrder = new SubmittedOrderEntity();
        submittedOrder.setSubmittedOrderId("SO" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        submittedOrder.setTableSessionId(session.getId());
        submittedOrder.setMerchantId(store.getMerchantId());
        submittedOrder.setStoreId(store.getId());
        submittedOrder.setTableId(table.getId());
        submittedOrder.setSourceOrderType(source.name());
        submittedOrder.setSourceActiveOrderId(sourceActiveOrderId);
        submittedOrder.setOrderNo("SO-" + table.getTableCode() + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6));
        submittedOrder.setFulfillmentStatus("SUBMITTED");
        submittedOrder.setSettlementStatus("UNPAID");
        submittedOrder.setMemberId(memberId);
        submittedOrder.setOriginalAmountCents(originalAmountCents);
        submittedOrder.setMemberDiscountCents(memberDiscountCents);
        submittedOrder.setPromotionDiscountCents(promotionDiscountCents);
        submittedOrder.setPayableAmountCents(payableAmountCents);
        submittedOrder.replaceItems(items.stream().map(this::toSubmittedItem).toList());
        submittedOrderRepository.save(submittedOrder);
    }

    private SubmittedOrderItemEntity toSubmittedItem(ActiveTableOrderItemEntity item) {
        SubmittedOrderItemEntity next = new SubmittedOrderItemEntity();
        next.setSkuId(item.getSkuId());
        next.setSkuCodeSnapshot(item.getSkuCodeSnapshot());
        next.setSkuNameSnapshot(item.getSkuNameSnapshot());
        next.setQuantity(item.getQuantity());
        next.setUnitPriceSnapshotCents(item.getUnitPriceSnapshotCents());
        next.setItemRemark(item.getItemRemark());
        next.setLineTotalCents(item.getLineTotalCents());
        return next;
    }

    private ActiveTableOrderDto toDto(ActiveTableOrderEntity entity) {
        StoreTableEntity table = storeTableRepository.findByIdAndStoreId(entity.getTableId(), entity.getStoreId())
                .orElseThrow(() -> new IllegalStateException("Table missing for active order: " + entity.getId()));

        List<ActiveTableOrderDto.ActiveTableOrderItemDto> items = entity.getItems().stream()
                .sorted(Comparator.comparing(ActiveTableOrderItemEntity::getId, Comparator.nullsLast(Long::compareTo)))
                .map(item -> new ActiveTableOrderDto.ActiveTableOrderItemDto(
                        item.getSkuId(),
                        item.getSkuCodeSnapshot(),
                        item.getSkuNameSnapshot(),
                        item.getQuantity(),
                        item.getUnitPriceSnapshotCents(),
                        item.getItemRemark(),
                        item.getLineTotalCents()
                ))
                .toList();

        return new ActiveTableOrderDto(
                entity.getActiveOrderId(),
                entity.getOrderNo(),
                entity.getStoreId(),
                entity.getTableId(),
                table.getTableCode(),
                entity.getOrderSource(),
                entity.getStatus(),
                entity.getMemberId(),
                items,
                new ActiveTableOrderDto.PricingDto(
                        entity.getOriginalAmountCents(),
                        entity.getMemberDiscountCents(),
                        entity.getPromotionDiscountCents(),
                        entity.getPayableAmountCents()
                )
        );
    }

    private SubmittedOrderDto toSubmittedDto(SubmittedOrderEntity entity) {
        return new SubmittedOrderDto(
                entity.getSubmittedOrderId(),
                entity.getOrderNo(),
                entity.getSourceOrderType(),
                entity.getFulfillmentStatus(),
                entity.getSettlementStatus(),
                entity.getMemberId(),
                new SubmittedOrderDto.PricingDto(
                        entity.getOriginalAmountCents(),
                        entity.getMemberDiscountCents(),
                        entity.getPromotionDiscountCents(),
                        entity.getPayableAmountCents()
                ),
                entity.getItems().stream()
                        .map(item -> new SubmittedOrderDto.ItemDto(
                                item.getSkuId(),
                                item.getSkuCodeSnapshot(),
                                item.getSkuNameSnapshot(),
                                item.getQuantity(),
                                item.getUnitPriceSnapshotCents(),
                                item.getItemRemark(),
                                item.getLineTotalCents()
                        ))
                        .toList()
        );
    }
}
