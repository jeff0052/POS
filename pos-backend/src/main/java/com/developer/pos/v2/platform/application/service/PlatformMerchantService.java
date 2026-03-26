package com.developer.pos.v2.platform.application.service;

import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.store.infrastructure.persistence.repository.JpaStoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Map;

@Service
public class PlatformMerchantService implements UseCase {

    @PersistenceContext
    private EntityManager em;

    private final JpaStoreRepository storeRepository;

    public PlatformMerchantService(JpaStoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listMerchants() {
        List<Object[]> rows = em.createNativeQuery(
                "SELECT m.id, m.merchant_code, m.merchant_name, m.status, m.currency_code, m.timezone, " +
                "(SELECT COUNT(*) FROM brands b WHERE b.merchant_id = m.id) as brand_count, " +
                "(SELECT COUNT(*) FROM stores s WHERE s.merchant_id = m.id) as store_count " +
                "FROM merchants m ORDER BY m.id"
        ).getResultList();

        return rows.stream().map(r -> Map.<String, Object>of(
                "id", r[0],
                "merchantCode", r[1] != null ? r[1] : "",
                "merchantName", r[2] != null ? r[2] : "",
                "status", r[3] != null ? r[3] : "ACTIVE",
                "currencyCode", r[4] != null ? r[4] : "SGD",
                "timezone", r[5] != null ? r[5] : "Asia/Singapore",
                "brandCount", r[6],
                "storeCount", r[7]
        )).toList();
    }

    @Transactional
    public void createMerchant(String merchantCode, String merchantName) {
        em.createNativeQuery(
                "INSERT INTO merchants (merchant_code, merchant_name) VALUES (:code, :name)"
        ).setParameter("code", merchantCode)
         .setParameter("name", merchantName)
         .executeUpdate();
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listBrands(Long merchantId) {
        List<Object[]> rows = em.createNativeQuery(
                "SELECT b.id, b.brand_code, b.brand_name, b.brand_status, " +
                "(SELECT COUNT(*) FROM brand_countries bc WHERE bc.brand_id = b.id) as country_count, " +
                "(SELECT COUNT(*) FROM stores s WHERE s.brand_id = b.id) as store_count " +
                "FROM brands b WHERE b.merchant_id = :merchantId ORDER BY b.id"
        ).setParameter("merchantId", merchantId).getResultList();

        return rows.stream().map(r -> Map.<String, Object>of(
                "id", r[0],
                "brandCode", r[1] != null ? r[1] : "",
                "brandName", r[2] != null ? r[2] : "",
                "status", r[3] != null ? r[3] : "ACTIVE",
                "countryCount", r[4],
                "storeCount", r[5]
        )).toList();
    }

    @Transactional
    public void createBrand(Long merchantId, String brandCode, String brandName) {
        em.createNativeQuery(
                "INSERT INTO brands (merchant_id, brand_code, brand_name) VALUES (:mid, :code, :name)"
        ).setParameter("mid", merchantId)
         .setParameter("code", brandCode)
         .setParameter("name", brandName)
         .executeUpdate();
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listCountries(Long brandId) {
        List<Object[]> rows = em.createNativeQuery(
                "SELECT bc.id, bc.country_code, bc.country_name, bc.currency_code, bc.timezone, " +
                "bc.tax_rate_percent, bc.country_status, " +
                "(SELECT COUNT(*) FROM stores s WHERE s.country_id = bc.id) as store_count " +
                "FROM brand_countries bc WHERE bc.brand_id = :brandId ORDER BY bc.id"
        ).setParameter("brandId", brandId).getResultList();

        return rows.stream().map(r -> Map.<String, Object>of(
                "id", r[0],
                "countryCode", r[1] != null ? r[1] : "",
                "countryName", r[2] != null ? r[2] : "",
                "currencyCode", r[3] != null ? r[3] : "SGD",
                "timezone", r[4] != null ? r[4] : "",
                "taxRatePercent", r[5],
                "status", r[6] != null ? r[6] : "ACTIVE",
                "storeCount", r[7]
        )).toList();
    }

    @Transactional
    public void createCountry(Long brandId, String countryCode, String countryName, String currencyCode, String timezone) {
        em.createNativeQuery(
                "INSERT INTO brand_countries (brand_id, country_code, country_name, currency_code, timezone) " +
                "VALUES (:bid, :code, :name, :currency, :tz)"
        ).setParameter("bid", brandId)
         .setParameter("code", countryCode)
         .setParameter("name", countryName)
         .setParameter("currency", currencyCode != null ? currencyCode : "SGD")
         .setParameter("tz", timezone != null ? timezone : "Asia/Singapore")
         .executeUpdate();
    }
}
