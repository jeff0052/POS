package com.developer.pos.order.service;

import com.developer.pos.order.dto.QrCurrentOrderResponse;
import com.developer.pos.order.dto.OrderDto;
import com.developer.pos.order.dto.OrderItemDto;
import com.developer.pos.order.dto.OrderListResponse;
import com.developer.pos.order.dto.QrOrderItemRequest;
import com.developer.pos.order.dto.QrOrderSubmitRequest;
import com.developer.pos.order.dto.QrOrderSubmitResponse;
import com.developer.pos.order.entity.OrderEntity;
import com.developer.pos.order.entity.QrTableOrderEntity;
import com.developer.pos.order.repository.OrderRepository;
import com.developer.pos.order.repository.QrTableOrderRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private static final AtomicInteger QR_SEQUENCE = new AtomicInteger(18);

    private final OrderRepository orderRepository;
    private final QrTableOrderRepository qrTableOrderRepository;
    private final ObjectMapper objectMapper;

    public OrderService(
        OrderRepository orderRepository,
        QrTableOrderRepository qrTableOrderRepository,
        ObjectMapper objectMapper
    ) {
        this.orderRepository = orderRepository;
        this.qrTableOrderRepository = qrTableOrderRepository;
        this.objectMapper = objectMapper;
    }

    public OrderListResponse list() {
        List<OrderDto> items = orderRepository.findTop50ByStoreIdOrderByCreatedAtDesc(1001L)
            .stream()
            .map(this::toDto)
            .toList();
        return new OrderListResponse(items, items.size());
    }

    public QrOrderSubmitResponse submitQrOrder(QrOrderSubmitRequest request) {
        List<QrOrderItemRequest> items = request.items() == null ? List.of() : request.items();

        long originalAmountCents = items.stream()
            .mapToLong(item -> safe(item.unitPriceCents()) * safe(item.quantity()))
            .sum();

        long memberDiscountCents = Boolean.TRUE.equals(request.memberBound())
            ? items.stream().mapToLong(item -> {
                long unitPrice = safe(item.unitPriceCents());
                long memberPrice = item.memberPriceCents() == null ? unitPrice : item.memberPriceCents();
                return Math.max(0L, unitPrice - memberPrice) * safe(item.quantity());
            }).sum()
            : 0L;

        long promotionDiscountCents = originalAmountCents >= 6000 ? 800L : 0L;
        long payableAmountCents = Math.max(0L, originalAmountCents - memberDiscountCents - promotionDiscountCents);

        String sequence = String.format("%03d", QR_SEQUENCE.incrementAndGet());
        String orderNo = "QR" + System.currentTimeMillis();
        String queueNo = "QR-" + request.tableCode() + "-" + sequence;
        LocalDateTime createdAt = LocalDateTime.now();

        persistOrderSummary(orderNo, payableAmountCents, createdAt);
        persistQrTableOrder(request, items, orderNo, queueNo, originalAmountCents, memberDiscountCents, promotionDiscountCents, payableAmountCents, createdAt);

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
            originalAmountCents,
            memberDiscountCents,
            promotionDiscountCents,
            payableAmountCents
        );
    }

    public QrCurrentOrderResponse getCurrentQrOrder(String storeCode, String tableCode) {
        return qrTableOrderRepository
            .findTopByStoreCodeAndTableCodeOrderByCreatedAtDesc(storeCode, tableCode)
            .map(entity -> new QrCurrentOrderResponse(
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
            ))
            .orElse(null);
    }

    private OrderDto toDto(OrderEntity entity) {
        return new OrderDto(
            entity.getId(),
            entity.getOrderNo(),
            entity.getPaidAmountCents() == null ? 0L : entity.getPaidAmountCents(),
            entity.getOrderStatus(),
            entity.getPaymentStatus() == null ? "SDK_PAY" : entity.getPaymentStatus(),
            entity.getCreatedAt() == null ? "" : entity.getCreatedAt().toString(),
            "-",
            entity.getPrintStatus(),
            List.of(),
            "T2",
            "POS",
            "Lina Chen",
            "Gold",
            3200L,
            200L,
            200L,
            entity.getPaidAmountCents() == null ? 0L : entity.getPaidAmountCents(),
            List.of("Peach Soda")
        );
    }

    private long safe(Long value) {
        return value == null ? 0L : value;
    }

    private long safe(Integer value) {
        return value == null ? 0L : value.longValue();
    }

    private void persistOrderSummary(String orderNo, long payableAmountCents, LocalDateTime createdAt) {
        OrderEntity entity = new OrderEntity();
        entity.setOrderNo(orderNo);
        entity.setStoreId(1001L);
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
}
