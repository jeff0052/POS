package com.developer.pos.order.service;

import com.developer.pos.order.dto.OrderDto;
import com.developer.pos.order.dto.OrderListResponse;
import com.developer.pos.order.entity.OrderEntity;
import com.developer.pos.order.repository.OrderRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ActiveTableOrderService activeTableOrderService;

    public OrderService(
        OrderRepository orderRepository,
        ActiveTableOrderService activeTableOrderService
    ) {
        this.orderRepository = orderRepository;
        this.activeTableOrderService = activeTableOrderService;
    }

    public OrderListResponse list() {
        List<TimedOrder> posOrders = orderRepository.findTop50ByStoreIdOrderByCreatedAtDesc(1001L)
            .stream()
            .map(this::toPosDto)
            .map(dto -> new TimedOrder(dto, parseTime(dto.createdAt())))
            .toList();

        List<TimedOrder> activeTableOrders = activeTableOrderService.listRecentQrOrders()
            .stream()
            .map(dto -> new TimedOrder(dto, parseTime(dto.createdAt())))
            .toList();

        List<OrderDto> merged = java.util.stream.Stream.concat(posOrders.stream(), activeTableOrders.stream())
            .sorted(Comparator.comparing(TimedOrder::createdAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .map(TimedOrder::order)
            .limit(50)
            .toList();

        return new OrderListResponse(merged, merged.size());
    }

    private OrderDto toPosDto(OrderEntity entity) {
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

    private LocalDateTime parseTime(String createdAt) {
        try {
            return createdAt == null || createdAt.isBlank() ? null : LocalDateTime.parse(createdAt);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private record TimedOrder(OrderDto order, LocalDateTime createdAt) {
    }
}
