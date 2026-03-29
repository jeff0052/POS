package com.developer.pos.v2.order.application.service;

import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberEntity;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberRepository;
import com.developer.pos.v2.order.application.dto.MerchantAdminOrderDto;
import com.developer.pos.v2.order.application.dto.MerchantAdminOrderItemDto;
import com.developer.pos.v2.order.domain.status.ActiveOrderStatus;
import com.developer.pos.v2.order.infrastructure.persistence.entity.ActiveTableOrderEntity;
import com.developer.pos.v2.order.infrastructure.persistence.entity.ActiveTableOrderItemEntity;
import com.developer.pos.v2.order.infrastructure.persistence.entity.SubmittedOrderEntity;
import com.developer.pos.v2.order.infrastructure.persistence.entity.SubmittedOrderItemEntity;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaActiveTableOrderRepository;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaSubmittedOrderRepository;
import com.developer.pos.v2.store.infrastructure.persistence.entity.StoreTableEntity;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreTableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MerchantOrderReadService implements UseCase {

    private final JpaActiveTableOrderRepository activeTableOrderRepository;
    private final JpaSubmittedOrderRepository submittedOrderRepository;
    private final JpaStoreTableRepository storeTableRepository;
    private final JpaMemberRepository memberRepository;

    public MerchantOrderReadService(
            JpaActiveTableOrderRepository activeTableOrderRepository,
            JpaSubmittedOrderRepository submittedOrderRepository,
            JpaStoreTableRepository storeTableRepository,
            JpaMemberRepository memberRepository
    ) {
        this.activeTableOrderRepository = activeTableOrderRepository;
        this.submittedOrderRepository = submittedOrderRepository;
        this.storeTableRepository = storeTableRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public List<MerchantAdminOrderDto> getOrders(Long storeId) {
        List<ActiveTableOrderEntity> activeOrders = activeTableOrderRepository.findAllByStoreIdOrderByIdDesc(storeId);
        List<SubmittedOrderEntity> submittedOrders = submittedOrderRepository.findAllByStoreIdOrderByIdDesc(storeId);

        Map<Long, StoreTableEntity> tablesById = storeTableRepository.findAll().stream()
                .filter(table -> table.getStoreId().equals(storeId))
                .collect(Collectors.toMap(StoreTableEntity::getId, Function.identity()));

        Map<Long, MemberEntity> membersById = loadMembers(
                activeOrders.stream().map(ActiveTableOrderEntity::getMemberId).toList(),
                submittedOrders.stream().map(SubmittedOrderEntity::getMemberId).toList()
        );

        List<MerchantAdminOrderDto> activeDtos = activeOrders.stream()
                .filter(order -> order.getStatus() != ActiveOrderStatus.SETTLED)
                .map(order -> toActiveOrderDto(
                        order,
                        tablesById.get(order.getTableId()),
                        order.getMemberId() != null ? membersById.get(order.getMemberId()) : null
                ))
                .toList();

        List<MerchantAdminOrderDto> submittedDtos = submittedOrders.stream()
                .map(order -> toSubmittedOrderDto(
                        order,
                        tablesById.get(order.getTableId()),
                        order.getMemberId() != null ? membersById.get(order.getMemberId()) : null
                ))
                .toList();

        return java.util.stream.Stream.concat(activeDtos.stream(), submittedDtos.stream())
                .sorted((left, right) -> right.createdAt().compareTo(left.createdAt()))
                .toList();
    }

    private Map<Long, MemberEntity> loadMembers(Collection<Long> activeMemberIds, Collection<Long> submittedMemberIds) {
        List<Long> memberIds = java.util.stream.Stream.concat(activeMemberIds.stream(), submittedMemberIds.stream())
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (memberIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, MemberEntity> result = new LinkedHashMap<>();
        memberRepository.findAllById(memberIds).forEach(member -> result.put(member.getId(), member));
        return result;
    }

    private MerchantAdminOrderDto toActiveOrderDto(
            ActiveTableOrderEntity order,
            StoreTableEntity table,
            MemberEntity member
    ) {
        List<MerchantAdminOrderItemDto> items = order.getItems().stream()
                .map(this::toActiveItemDto)
                .toList();

        return new MerchantAdminOrderDto(
                order.getActiveOrderId(),
                order.getOrderNo(),
                order.getStoreId(),
                order.getTableId(),
                table != null ? table.getTableCode() : null,
                order.getOrderSource().name(),
                order.getStatus().name(),
                "UNPAID",
                order.getCreatedAt(),
                member != null ? member.getName() : null,
                member != null ? member.getTierCode() : null,
                order.getOriginalAmountCents(),
                order.getMemberDiscountCents(),
                order.getPromotionDiscountCents(),
                order.getPayableAmountCents(),
                List.of(),
                items
        );
    }

    private MerchantAdminOrderDto toSubmittedOrderDto(
            SubmittedOrderEntity order,
            StoreTableEntity table,
            MemberEntity member
    ) {
        String status = switch (order.getSettlementStatus()) {
            case "SETTLED" -> "PAID";
            default -> "PENDING_SETTLEMENT".equals(table != null ? table.getTableStatus() : null)
                    ? "PENDING_SETTLEMENT"
                    : "SUBMITTED";
        };

        List<MerchantAdminOrderItemDto> items = order.getItems().stream()
                .map(this::toSubmittedItemDto)
                .toList();

        return new MerchantAdminOrderDto(
                order.getSubmittedOrderId(),
                order.getOrderNo(),
                order.getStoreId(),
                order.getTableId(),
                table != null ? table.getTableCode() : null,
                order.getSourceOrderType(),
                status,
                "SETTLED".equals(order.getSettlementStatus()) ? "CASH" : "UNPAID",
                order.getSubmittedAt(),
                member != null ? member.getName() : null,
                member != null ? member.getTierCode() : null,
                order.getOriginalAmountCents(),
                order.getMemberDiscountCents(),
                order.getPromotionDiscountCents(),
                order.getPayableAmountCents(),
                List.of(),
                items
        );
    }

    private MerchantAdminOrderItemDto toActiveItemDto(ActiveTableOrderItemEntity item) {
        return new MerchantAdminOrderItemDto(
                item.getSkuNameSnapshot(),
                item.getQuantity(),
                item.getLineTotalCents(),
                item.getLineTotalCents(),
                0,
                0,
                false,
                item.isBuffetIncluded(),
                item.getBuffetSurchargeCents(),
                item.getBuffetInclusionType()
        );
    }

    private MerchantAdminOrderItemDto toSubmittedItemDto(SubmittedOrderItemEntity item) {
        return new MerchantAdminOrderItemDto(
                item.getSkuNameSnapshot(),
                item.getQuantity(),
                item.getLineTotalCents(),
                item.getLineTotalCents(),
                0,
                0,
                false,
                item.isBuffetIncluded(),
                item.getBuffetSurchargeCents(),
                item.getBuffetInclusionType()
        );
    }
}
