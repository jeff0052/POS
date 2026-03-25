package com.developer.pos.v2.catalog.application.service;

import com.developer.pos.v2.catalog.application.dto.AdminCatalogCategoryDto;
import com.developer.pos.v2.catalog.application.dto.AdminCatalogProductDto;
import com.developer.pos.v2.catalog.application.dto.QrMenuDto;
import com.developer.pos.v2.common.application.UseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class AdminCatalogReadService implements UseCase {

    private final QrMenuApplicationService qrMenuApplicationService;

    public AdminCatalogReadService(QrMenuApplicationService qrMenuApplicationService) {
        this.qrMenuApplicationService = qrMenuApplicationService;
    }

    @Transactional(readOnly = true)
    public List<AdminCatalogProductDto> getProducts(String storeCode) {
        QrMenuDto menu = qrMenuApplicationService.getMenu(storeCode);
        return menu.categories().stream()
                .flatMap(category -> category.items().stream().map(item -> new AdminCatalogProductDto(
                        item.skuId(),
                        item.skuName(),
                        item.skuCode(),
                        item.unitPriceCents(),
                        999,
                        "ENABLED",
                        category.categoryName()
                )))
                .sorted(Comparator.comparing(AdminCatalogProductDto::categoryName).thenComparing(AdminCatalogProductDto::name))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminCatalogCategoryDto> getCategories(String storeCode) {
        QrMenuDto menu = qrMenuApplicationService.getMenu(storeCode);
        return menu.categories().stream()
                .map(category -> new AdminCatalogCategoryDto(
                        category.categoryId(),
                        category.categoryName(),
                        0,
                        "ENABLED"
                ))
                .sorted(Comparator.comparing(AdminCatalogCategoryDto::name))
                .toList();
    }
}
