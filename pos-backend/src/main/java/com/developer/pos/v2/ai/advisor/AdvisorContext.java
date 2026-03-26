package com.developer.pos.v2.ai.advisor;

/**
 * Context assembled for an advisor to make recommendations.
 * Built by AiOperatorService from MCP tool results.
 */
public record AdvisorContext(
    Long merchantId,
    Long storeId,
    AdvisorRole role,
    Object salesData,       // from report.daily_summary
    Object orderData,       // from order.list_all
    Object memberData,      // from member stats
    Object promotionData,   // from promotion.list_rules
    Object catalogData      // from catalog.list_products
) {}
