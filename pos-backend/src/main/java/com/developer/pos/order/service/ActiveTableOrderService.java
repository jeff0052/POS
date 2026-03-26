package com.developer.pos.order.service;

import com.developer.pos.order.dto.OrderDto;
import com.developer.pos.order.dto.OrderItemDto;
import com.developer.pos.order.dto.QrCurrentOrderResponse;
import com.developer.pos.order.dto.QrOrderItemRequest;
import com.developer.pos.order.dto.QrOrderSettleRequest;
import com.developer.pos.order.dto.QrOrderSubmitRequest;
import com.developer.pos.order.dto.QrOrderSubmitResponse;
import com.developer.pos.order.dto.QrOrderUpdateRequest;
import com.developer.pos.order.entity.OrderEntity;
import com.developer.pos.order.entity.QrTableOrderEntity;
import com.developer.pos.order.repository.OrderRepository;
import com.developer.pos.order.repository.QrTableOrderRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class ActiveTableOrderService {

    private static final AtomicInteger QR_SEQUENCE = new AtomicInteger(18);

    private final OrderRepository orderRepository;
    private final QrTableOrderRepository qrTableOrderRepository;
    private final ObjectMapper objectMapper;

    public ActiveTableOrderService(
        OrderRepository orderRepository,
        QrTableOrderRepository qrTableOrderRepository,
        ObjectMapper objectMapper
    ) {
        this.orderRepository = orderRepository;
        this.qrTableOrderRepository = qrTableOrderRepository;
        this.objectMapper = objectMapper;
    }

    public List<OrderDto> listRecentQrOrders() {
        return qrTableOrderRepository.findTop50ByOrderByCreatedAtDesc()
            .stream()
            .map(this::toQrDto)
            .toList();
    }

    public QrOrderSubmitResponse submitQrOrder(QrOrderSubmitRequest request) {
        List<QrOrderItemRequest> requestItems = request.items() == null ? List.of() : request.items();
        LocalDateTime createdAt = LocalDateTime.now();
        QrTableOrderEntity activeOrder = findActiveTableOrder(request.storeCode(), request.tableCode()).orElse(null);

        if (activeOrder != null) {
            List<QrOrderItemRequest> mergedItems = mergeItems(readItems(activeOrder.getItemsJson()), requestItems);
            OrderAmounts amounts = calculateAmounts(mergedItems);
            activeOrder.setItemsJson(writeItems(mergedItems));
            activeOrder.setSettlementStatus("PENDING_SETTLEMENT");
            activeOrder.setMemberName(Boolean.TRUE.equals(request.memberBound()) ? request.memberName() : activeOrder.getMemberName());
            activeOrder.setMemberTier(Boolean.TRUE.equals(request.memberBound()) ? request.memberTier() : activeOrder.getMemberTier());
            activeOrder.setOriginalAmountCents(amounts.originalAmountCents());
            activeOrder.setMemberDiscountCents(amounts.memberDiscountCents());
            activeOrder.setPromotionDiscountCents(amounts.promotionDiscountCents());
            activeOrder.setPayableAmountCents(amounts.payableAmountCents());
            qrTableOrderRepository.save(activeOrder);

            orderRepository.findByOrderNo(activeOrder.getOrderNo()).ifPresent(order -> {
                order.setPaidAmountCents(amounts.payableAmountCents());
                order.setOrderStatus("PENDING_SETTLEMENT");
                order.setPaymentStatus("UNPAID");
                orderRepository.save(order);
            });

            return new QrOrderSubmitResponse(
                activeOrder.getOrderNo(),
                activeOrder.getQueueNo(),
                request.storeCode(),
                request.storeName(),
                request.tableCode(),
                "QR",
                activeOrder.getSettlementStatus(),
                activeOrder.getMemberName(),
                activeOrder.getMemberTier(),
                amounts.originalAmountCents(),
                amounts.memberDiscountCents(),
                amounts.promotionDiscountCents(),
                amounts.payableAmountCents()
            );
        }

        OrderAmounts amounts = calculateAmounts(requestItems);
        String sequence = String.format("%03d", QR_SEQUENCE.incrementAndGet());
        String orderNo = "QR" + System.currentTimeMillis();
        String queueNo = "QR-" + request.tableCode() + "-" + sequence;

        persistOrderSummary(orderNo, amounts.payableAmountCents(), createdAt);
        persistQrTableOrder(
            request,
            requestItems,
            orderNo,
            queueNo,
            amounts.originalAmountCents(),
            amounts.memberDiscountCents(),
            amounts.promotionDiscountCents(),
            amounts.payableAmountCents(),
            createdAt
        );

        return new QrOrderSubmitResponse(
            orderNo,
            queueNo,
            request.storeCode(),
            request.storeName(),
            request.tableCode(),
            "QR",
            "PENDING_SETTLEMENT",
            Boolean.TRUE.equals(request.memberBound()) ? request.memberName() : null,
            Boolean.TRUE.equals(request.memberBound()) ? request.memberTier() : null,
            amounts.originalAmountCents(),
            amounts.memberDiscountCents(),
            amounts.promotionDiscountCents(),
            amounts.payableAmountCents()
        );
    }

    public QrCurrentOrderResponse getCurrentQrOrder(String storeCode, String tableCode) {
        return findActiveTableOrder(storeCode, tableCode)
            .map(this::toCurrentOrderResponse)
            .orElse(null);
    }

    public QrCurrentOrderResponse updateCurrentQrOrder(QrOrderUpdateRequest request) {
        List<QrOrderItemRequest> items = request.items() == null
            ? List.of()
            : request.items().stream().filter(item -> item.quantity() != null && item.quantity() > 0).toList();

        QrTableOrderEntity entity = findActiveTableOrder(request.storeCode(), request.tableCode())
            .orElseGet(() -> createDraftTableOrder(request, items));

        OrderAmounts amounts = calculateAmounts(items);
        entity.setItemsJson(writeItems(items));
        entity.setSettlementStatus(items.isEmpty() ? "DRAFT" : resolveSettlementStatus(request.settlementStatus(), entity.getSettlementStatus()));
        entity.setOriginalAmountCents(amounts.originalAmountCents());
        entity.setMemberDiscountCents(amounts.memberDiscountCents());
        entity.setPromotionDiscountCents(amounts.promotionDiscountCents());
        entity.setPayableAmountCents(amounts.payableAmountCents());
        qrTableOrderRepository.save(entity);

        orderRepository.findByOrderNo(entity.getOrderNo()).ifPresent(order -> {
            order.setPaidAmountCents(amounts.payableAmountCents());
            order.setOrderStatus(entity.getSettlementStatus());
            order.setPaymentStatus("UNPAID");
            orderRepository.save(order);
        });

        return toCurrentOrderResponse(entity);
    }

    public void clearCurrentQrOrder(String storeCode, String tableCode) {
        findActiveTableOrder(storeCode, tableCode)
            .ifPresent(entity -> {
                qrTableOrderRepository.delete(entity);
                orderRepository.findByOrderNo(entity.getOrderNo()).ifPresent(orderRepository::delete);
            });
    }

    public void settleCurrentQrOrder(QrOrderSettleRequest request) {
        QrTableOrderEntity entity = findActiveTableOrder(request.storeCode(), request.tableCode())
            .orElseThrow(() -> new IllegalArgumentException("QR order not found for settlement"));

        entity.setSettlementStatus("SETTLED");
        qrTableOrderRepository.save(entity);

        orderRepository.findByOrderNo(entity.getOrderNo()).ifPresent(order -> {
            order.setOrderStatus("PAID");
            order.setPaymentStatus("SDK_PAY");
            order.setPaidAmountCents(entity.getPayableAmountCents());
            orderRepository.save(order);
        });
    }

    private java.util.Optional<QrTableOrderEntity> findActiveTableOrder(String storeCode, String tableCode) {
        return qrTableOrderRepository.findTopByStoreCodeAndTableCodeAndSettlementStatusNotOrderByCreatedAtDesc(
            storeCode,
            tableCode,
            "SETTLED"
        );
    }

    private QrTableOrderEntity createDraftTableOrder(QrOrderUpdateRequest request, List<QrOrderItemRequest> items) {
        String sequence = String.format("%03d", QR_SEQUENCE.incrementAndGet());
        String orderNo = "POS" + System.currentTimeMillis();
        String queueNo = "POS-" + request.tableCode() + "-" + sequence;
        LocalDateTime createdAt = LocalDateTime.now();
        OrderAmounts amounts = calculateAmounts(items);
        persistOrderSummary(orderNo, amounts.payableAmountCents(), createdAt);

        QrTableOrderEntity created = new QrTableOrderEntity();
        created.setOrderNo(orderNo);
        created.setQueueNo(queueNo);
        created.setStoreCode(request.storeCode());
        created.setStoreName("Riverside Branch");
        created.setTableCode(request.tableCode());
        created.setSettlementStatus(resolveSettlementStatus(request.settlementStatus(), "DRAFT"));
        created.setOriginalAmountCents(amounts.originalAmountCents());
        created.setMemberDiscountCents(amounts.memberDiscountCents());
        created.setPromotionDiscountCents(amounts.promotionDiscountCents());
        created.setPayableAmountCents(amounts.payableAmountCents());
        created.setItemsJson(writeItems(items));
        created.setCreatedAt(createdAt);
        return qrTableOrderRepository.save(created);
    }

    private String resolveSettlementStatus(String requestedStatus, String fallbackStatus) {
        return requestedStatus == null || requestedStatus.isBlank() ? fallbackStatus : requestedStatus;
    }

    private QrCurrentOrderResponse toCurrentOrderResponse(QrTableOrderEntity entity) {
        return new QrCurrentOrderResponse(
            entity.getOrderNo(),
            entity.getQueueNo(),
            entity.getStoreCode(),
            entity.getStoreName(),
            entity.getTableCode(),
            entity.getSettlementStatus(),
            entity.getMemberName(),
            entity.getMemberTier(),
            entity.getOriginalAmountCents(),
            entity.getMemberDiscountCents(),
            entity.getPromotionDiscountCents(),
            entity.getPayableAmountCents(),
            readItems(entity.getItemsJson())
        );
    }

    private OrderDto toQrDto(QrTableOrderEntity entity) {
        return new OrderDto(
            entity.getId(),
            entity.getOrderNo(),
            entity.getPayableAmountCents(),
            entity.getSettlementStatus(),
            "UNPAID",
            entity.getCreatedAt() == null ? "" : entity.getCreatedAt().toString(),
            "QR guest",
            "NOT_PRINTED",
            readItems(entity.getItemsJson()).stream().map(item -> new OrderItemDto(
                item.productName(),
                item.quantity(),
                safe(item.memberPriceCents()) > 0 ? item.memberPriceCents() * safe(item.quantity()) : item.unitPriceCents() * safe(item.quantity())
            )).toList(),
            entity.getTableCode(),
            "QR",
            entity.getMemberName(),
            entity.getMemberTier(),
            entity.getOriginalAmountCents(),
            entity.getMemberDiscountCents(),
            entity.getPromotionDiscountCents(),
            entity.getPayableAmountCents(),
            List.of()
        );
    }

    private void persistOrderSummary(String orderNo, long payableAmountCents, LocalDateTime createdAt) {
        Long storeId = 1001L; // TODO: Remove V1 hardcode — migrate callers to V2 APIs
        OrderEntity entity = new OrderEntity();
        entity.setOrderNo(orderNo);
        entity.setStoreId(storeId);
        entity.setCashierId(0L);
        entity.setPaidAmountCents(payableAmountCents);
        entity.setOrderStatus("PENDING_SETTLEMENT");
        entity.setPaymentStatus("UNPAID");
        entity.setPrintStatus("NOT_PRINTED");
        entity.setCreatedAt(createdAt);
        orderRepository.save(entity);
    }

    private void persistQrTableOrder(
        QrOrderSubmitRequest request,
        List<QrOrderItemRequest> items,
        String orderNo,
        String queueNo,
        long originalAmountCents,
        long memberDiscountCents,
        long promotionDiscountCents,
        long payableAmountCents,
        LocalDateTime createdAt
    ) {
        QrTableOrderEntity entity = new QrTableOrderEntity();
        entity.setOrderNo(orderNo);
        entity.setQueueNo(queueNo);
        entity.setStoreCode(request.storeCode());
        entity.setStoreName(request.storeName());
        entity.setTableCode(request.tableCode());
        entity.setSettlementStatus("PENDING_SETTLEMENT");
        entity.setMemberName(Boolean.TRUE.equals(request.memberBound()) ? request.memberName() : null);
        entity.setMemberTier(Boolean.TRUE.equals(request.memberBound()) ? request.memberTier() : null);
        entity.setOriginalAmountCents(originalAmountCents);
        entity.setMemberDiscountCents(memberDiscountCents);
        entity.setPromotionDiscountCents(promotionDiscountCents);
        entity.setPayableAmountCents(payableAmountCents);
        entity.setItemsJson(writeItems(items));
        entity.setCreatedAt(createdAt);
        qrTableOrderRepository.save(entity);
    }

    private OrderAmounts calculateAmounts(List<QrOrderItemRequest> items) {
        long originalAmountCents = items.stream()
            .mapToLong(item -> safe(item.unitPriceCents()) * safe(item.quantity()))
            .sum();

        long memberDiscountCents = items.stream()
            .mapToLong(item -> {
                long unitPrice = safe(item.unitPriceCents());
                long memberPrice = item.memberPriceCents() == null ? unitPrice : item.memberPriceCents();
                return Math.max(0L, unitPrice - memberPrice) * safe(item.quantity());
            })
            .sum();

        long promotionDiscountCents = originalAmountCents >= 6000 ? 800L : 0L;
        long payableAmountCents = Math.max(0L, originalAmountCents - memberDiscountCents - promotionDiscountCents);
        return new OrderAmounts(originalAmountCents, memberDiscountCents, promotionDiscountCents, payableAmountCents);
    }

    private List<QrOrderItemRequest> mergeItems(List<QrOrderItemRequest> existing, List<QrOrderItemRequest> incoming) {
        List<QrOrderItemRequest> merged = new ArrayList<>(existing);

        for (QrOrderItemRequest next : incoming) {
            int matchIndex = -1;
            for (int i = 0; i < merged.size(); i++) {
                if (merged.get(i).productName().equals(next.productName())) {
                    matchIndex = i;
                    break;
                }
            }

            if (matchIndex >= 0) {
                QrOrderItemRequest current = merged.get(matchIndex);
                merged.set(matchIndex, new QrOrderItemRequest(
                    current.productId(),
                    current.productName(),
                    current.quantity() + next.quantity(),
                    current.unitPriceCents(),
                    current.memberPriceCents() != null ? current.memberPriceCents() : next.memberPriceCents()
                ));
            } else {
                merged.add(next);
            }
        }

        return merged;
    }

    private String writeItems(List<QrOrderItemRequest> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to persist QR order items", exception);
        }
    }

    private List<QrOrderItemRequest> readItems(String itemsJson) {
        if (itemsJson == null || itemsJson.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(itemsJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to read QR order items", exception);
        }
    }

    private long safe(Long value) {
        return value == null ? 0L : value;
    }

    private long safe(Integer value) {
        return value == null ? 0L : value.longValue();
    }

    private record OrderAmounts(
        long originalAmountCents,
        long memberDiscountCents,
        long promotionDiscountCents,
        long payableAmountCents
    ) {
    }
}
