package com.developer.pos.v2.catalog.application.service;

import com.developer.pos.v2.catalog.application.dto.QrMenuDto;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaQrMenuRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.QrMenuProjection;
import com.developer.pos.v2.common.application.UseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class QrMenuApplicationService implements UseCase {

    private final JpaQrMenuRepository qrMenuRepository;

    public QrMenuApplicationService(JpaQrMenuRepository qrMenuRepository) {
        this.qrMenuRepository = qrMenuRepository;
    }

    @Transactional(readOnly = true)
    public QrMenuDto getMenu(String storeCode) {
        List<QrMenuProjection> rows = qrMenuRepository.findQrMenuByStoreCode(storeCode);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("No QR menu available for store: " + storeCode);
        }

        QrMenuProjection first = rows.get(0);
        Map<Long, CategoryAccumulator> categories = new LinkedHashMap<>();

        for (QrMenuProjection row : rows) {
            CategoryAccumulator category = categories.computeIfAbsent(
                    row.getCategoryId(),
                    ignored -> new CategoryAccumulator(
                            row.getCategoryId(),
                            row.getCategoryCode(),
                            row.getCategoryName(),
                            new ArrayList<>()
                    )
            );

            category.items().add(new QrMenuDto.MenuItemDto(
                    row.getProductId(),
                    row.getProductCode(),
                    row.getProductName(),
                    row.getSkuId(),
                    row.getSkuCode(),
                    row.getSkuName(),
                    row.getUnitPriceCents()
            ));
        }

        return new QrMenuDto(
                first.getStoreId(),
                first.getStoreCode(),
                first.getStoreName(),
                categories.values().stream()
                        .map(category -> new QrMenuDto.CategoryDto(
                                category.categoryId(),
                                category.categoryCode(),
                                category.categoryName(),
                                category.items()
                        ))
                        .toList()
        );
    }

    private record CategoryAccumulator(
            Long categoryId,
            String categoryCode,
            String categoryName,
            List<QrMenuDto.MenuItemDto> items
    ) {
    }
}
