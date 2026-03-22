package com.developer.pos.order.service;

import com.developer.pos.order.dto.OrderDto;
import com.developer.pos.order.dto.OrderItemDto;
import com.developer.pos.order.dto.OrderListResponse;
import com.developer.pos.order.entity.OrderEntity;
import com.developer.pos.order.repository.OrderRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

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
            List.of()
        );
    }
}
