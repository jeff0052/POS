package com.developer.pos.order.service;

import com.developer.pos.order.dto.OrderDto;
import com.developer.pos.order.dto.OrderItemDto;
import com.developer.pos.order.dto.OrderListResponse;
import com.developer.pos.order.dto.QrOrderItemRequest;
import com.developer.pos.order.dto.QrOrderSubmitRequest;
import com.developer.pos.order.dto.QrOrderSubmitResponse;
import com.developer.pos.order.entity.OrderEntity;
import com.developer.pos.order.repository.OrderRepository;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private static final AtomicInteger QR_SEQUENCE = new AtomicInteger(18);

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
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
}
