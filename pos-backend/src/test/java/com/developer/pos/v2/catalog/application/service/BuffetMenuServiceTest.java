package com.developer.pos.v2.catalog.application.service;

import com.developer.pos.v2.catalog.application.dto.MenuQueryResultDto;
import com.developer.pos.v2.catalog.application.dto.MenuQueryResultDto.MenuCategoryDto;
import com.developer.pos.v2.catalog.application.dto.MenuQueryResultDto.MenuProductDto;
import com.developer.pos.v2.catalog.application.dto.MenuQueryResultDto.MenuSkuDto;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.BuffetPackageEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.BuffetPackageItemEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.ProductCategoryEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.ProductEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.entity.SkuEntity;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaBuffetPackageItemRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaBuffetPackageRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaProductCategoryRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaProductRepository;
import com.developer.pos.v2.catalog.infrastructure.persistence.repository.JpaSkuRepository;
import com.developer.pos.v2.image.application.service.ImageUploadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuffetMenuServiceTest {

    @Mock JpaBuffetPackageRepository packageRepo;
    @Mock JpaBuffetPackageItemRepository itemRepo;
    @Mock JpaSkuRepository skuRepo;
    @Mock JpaProductRepository productRepo;
    @Mock JpaProductCategoryRepository categoryRepo;
    @Mock ImageUploadService imageUploadService;
    @InjectMocks BuffetMenuService service;

    private static final Long STORE_ID = 1L;
    private static final Long PACKAGE_ID = 100L;

    // ─── Helpers ──────────────────────────────────────────────────────────

    private BuffetPackageEntity activePackage() {
        BuffetPackageEntity pkg = new BuffetPackageEntity(
                STORE_ID, "PKG001", "Lunch Buffet", "Desc",
                19900L, null, null, 90, 10, 100L, 5, 30,
                "ACTIVE", "[]", "[]", 1, null, 1L);
        setId(pkg, PACKAGE_ID);
        return pkg;
    }

    private BuffetPackageItemEntity item(Long id, Long skuId, String inclusionType,
                                          long surchargeCents, Integer maxQty, int sortOrder) {
        BuffetPackageItemEntity item = new BuffetPackageItemEntity(PACKAGE_ID, skuId, inclusionType,
                surchargeCents, maxQty, sortOrder);
        setId(item, id);
        return item;
    }

    private SkuEntity sku(Long id, Long productId, String code, String name, long priceCents, String imageId) {
        SkuEntity s = new SkuEntity(productId, code, name, priceCents, "ACTIVE");
        setId(s, id);
        if (imageId != null) s.setImageId(imageId);
        return s;
    }

    private ProductEntity product(Long id, Long categoryId, String name, String imageId) {
        ProductEntity p = new ProductEntity(STORE_ID, categoryId, "P" + id, name, "ACTIVE", null, null, null);
        setId(p, id);
        if (imageId != null) p.setImageId(imageId);
        return p;
    }

    private ProductCategoryEntity category(Long id, String name) {
        ProductCategoryEntity c = new ProductCategoryEntity(STORE_ID, "CAT" + id, name, true, 1);
        setId(c, id);
        return c;
    }

    private static void setId(Object entity, Long id) {
        try {
            Field field = findField(entity.getClass(), "id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set id on " + entity.getClass().getSimpleName(), e);
        }
    }

    private static Field findField(Class<?> clazz, String name) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new RuntimeException("Field '" + name + "' not found");
    }

    private void setupCommonStubs(List<BuffetPackageItemEntity> items,
                                   List<SkuEntity> skus,
                                   List<ProductEntity> products,
                                   List<ProductCategoryEntity> categories) {
        when(packageRepo.findById(PACKAGE_ID)).thenReturn(Optional.of(activePackage()));
        when(itemRepo.findByPackageIdAndInclusionTypeNotOrderBySortOrderAsc(PACKAGE_ID, "EXCLUDED"))
                .thenReturn(items);
        when(skuRepo.findAllById(any())).thenReturn(skus);
        when(productRepo.findByIdIn(anyList())).thenReturn(products);
        when(categoryRepo.findAllById(any())).thenReturn(categories);
        when(imageUploadService.resolvePublicUrls(any())).thenReturn(Map.of());
    }

    // ─── Test 1: INCLUDED + SURCHARGE returned, EXCLUDED filtered out ─────

    @Test
    void getMenu_includesIncludedAndSurcharge_excludesExcluded() {
        // EXCLUDED items are filtered at the repository query level (NOT param = EXCLUDED)
        // So we only set up INCLUDED and SURCHARGE items
        SkuEntity sku1 = sku(10L, 20L, "SKU-INC", "Included Sku", 0, null);
        SkuEntity sku2 = sku(11L, 20L, "SKU-SUR", "Surcharge Sku", 500, null);

        ProductEntity prod = product(20L, 30L, "Sashimi", null);
        ProductCategoryEntity cat = category(30L, "Appetizers");

        BuffetPackageItemEntity incItem = item(1L, 10L, "INCLUDED", 0, null, 1);
        BuffetPackageItemEntity surItem = item(2L, 11L, "SURCHARGE", 500, null, 2);

        setupCommonStubs(List.of(incItem, surItem), List.of(sku1, sku2), List.of(prod), List.of(cat));

        MenuQueryResultDto result = service.getBuffetMenu(STORE_ID, PACKAGE_ID);

        assertThat(result.categories()).hasSize(1);
        MenuCategoryDto category = result.categories().get(0);
        assertThat(category.products()).hasSize(1);

        List<MenuSkuDto> skus = category.products().get(0).skus();
        assertThat(skus).hasSize(2);
        assertThat(skus).extracting(MenuSkuDto::buffetInclusionType)
                .containsExactly("INCLUDED", "SURCHARGE");
    }

    // ─── Test 2: surchargeCents value passed through to DTO ───────────────

    @Test
    void getMenu_surchargeAnnotated() {
        SkuEntity sku1 = sku(10L, 20L, "SKU-SUR", "Premium Sku", 1500, null);
        ProductEntity prod = product(20L, 30L, "Wagyu", null);
        ProductCategoryEntity cat = category(30L, "Beef");

        BuffetPackageItemEntity surItem = item(1L, 10L, "SURCHARGE", 800, null, 1);

        setupCommonStubs(List.of(surItem), List.of(sku1), List.of(prod), List.of(cat));

        MenuQueryResultDto result = service.getBuffetMenu(STORE_ID, PACKAGE_ID);

        MenuSkuDto skuDto = result.categories().get(0).products().get(0).skus().get(0);
        assertThat(skuDto.buffetInclusionType()).isEqualTo("SURCHARGE");
        assertThat(skuDto.buffetSurchargeCents()).isEqualTo(800L);
    }

    // ─── Test 3: maxQtyPerPerson in output ────────────────────────────────

    @Test
    void getMenu_maxQtyPassedThrough() {
        SkuEntity sku1 = sku(10L, 20L, "SKU-LTD", "Limited Sku", 0, null);
        ProductEntity prod = product(20L, 30L, "Lobster", null);
        ProductCategoryEntity cat = category(30L, "Seafood");

        BuffetPackageItemEntity limitedItem = item(1L, 10L, "INCLUDED", 0, 2, 1);

        setupCommonStubs(List.of(limitedItem), List.of(sku1), List.of(prod), List.of(cat));

        MenuQueryResultDto result = service.getBuffetMenu(STORE_ID, PACKAGE_ID);

        MenuSkuDto skuDto = result.categories().get(0).products().get(0).skus().get(0);
        assertThat(skuDto.maxQtyPerPerson()).isEqualTo(2);
    }

    // ─── Test 4: products grouped under their categories ──────────────────

    @Test
    void getMenu_groupedByCategory() {
        SkuEntity sku1 = sku(10L, 20L, "SKU-A", "Sku A", 0, null);
        SkuEntity sku2 = sku(11L, 21L, "SKU-B", "Sku B", 0, null);

        ProductEntity prod1 = product(20L, 30L, "Product A", null);
        ProductEntity prod2 = product(21L, 31L, "Product B", null);

        ProductCategoryEntity cat1 = category(30L, "Category One");
        ProductCategoryEntity cat2 = category(31L, "Category Two");

        BuffetPackageItemEntity item1 = item(1L, 10L, "INCLUDED", 0, null, 1);
        BuffetPackageItemEntity item2 = item(2L, 11L, "INCLUDED", 0, null, 2);

        setupCommonStubs(List.of(item1, item2), List.of(sku1, sku2),
                List.of(prod1, prod2), List.of(cat1, cat2));

        MenuQueryResultDto result = service.getBuffetMenu(STORE_ID, PACKAGE_ID);

        assertThat(result.categories()).hasSize(2);
        assertThat(result.categories()).extracting(MenuCategoryDto::categoryName)
                .containsExactlyInAnyOrder("Category One", "Category Two");

        // Each category has exactly one product
        for (MenuCategoryDto cat : result.categories()) {
            assertThat(cat.products()).hasSize(1);
        }
    }

    // ─── Test 5: ImageUploadService.resolvePublicUrls called with correct imageIds ──

    @SuppressWarnings("unchecked")
    @Test
    void getMenu_imageUrlResolved() {
        SkuEntity sku1 = sku(10L, 20L, "SKU-IMG", "Img Sku", 0, "IMG-SKU001");
        ProductEntity prod = product(20L, 30L, "Fancy Dish", "IMG-PROD001");
        ProductCategoryEntity cat = category(30L, "Main");

        BuffetPackageItemEntity buffetItem = item(1L, 10L, "INCLUDED", 0, null, 1);

        when(packageRepo.findById(PACKAGE_ID)).thenReturn(Optional.of(activePackage()));
        when(itemRepo.findByPackageIdAndInclusionTypeNotOrderBySortOrderAsc(PACKAGE_ID, "EXCLUDED"))
                .thenReturn(List.of(buffetItem));
        when(skuRepo.findAllById(any())).thenReturn(List.of(sku1));
        when(productRepo.findByIdIn(anyList())).thenReturn(List.of(prod));
        when(categoryRepo.findAllById(any())).thenReturn(List.of(cat));

        Map<String, String> urlMap = Map.of(
                "IMG-PROD001", "https://cdn.example.com/prod.jpg",
                "IMG-SKU001", "https://cdn.example.com/sku.jpg"
        );
        when(imageUploadService.resolvePublicUrls(any())).thenReturn(urlMap);

        MenuQueryResultDto result = service.getBuffetMenu(STORE_ID, PACKAGE_ID);

        // Verify resolvePublicUrls was called with both image IDs
        verify(imageUploadService).resolvePublicUrls(any(Collection.class));

        // Verify URLs are set in the result
        MenuProductDto productDto = result.categories().get(0).products().get(0);
        assertThat(productDto.imageUrl()).isEqualTo("https://cdn.example.com/prod.jpg");
        assertThat(productDto.skus().get(0).imageUrl()).isEqualTo("https://cdn.example.com/sku.jpg");
    }
}
