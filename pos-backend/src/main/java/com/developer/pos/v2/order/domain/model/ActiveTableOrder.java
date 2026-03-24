package com.developer.pos.v2.order.domain.model;

import com.developer.pos.v2.common.domain.AggregateRoot;
import com.developer.pos.v2.order.domain.source.OrderSource;
import com.developer.pos.v2.order.domain.status.ActiveOrderStatus;
import com.developer.pos.v2.store.domain.model.TableRef;

import java.util.List;

public record ActiveTableOrder(
        String activeOrderId,
        String orderNo,
        TableRef table,
        OrderSource orderSource,
        ActiveOrderStatus status,
        Long memberId,
        List<ActiveTableOrderItem> items
) implements AggregateRoot {

    public long originalAmountCents() {
        return items.stream()
                .mapToLong(ActiveTableOrderItem::lineTotalCents)
                .sum();
    }
}
