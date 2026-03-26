package com.developer.pos.v2.ai.advisor;

public enum AdvisorRole {
    MENU_ADVISOR("菜单顾问", "分析 SKU 销量、毛利、退菜率，建议下架/主推/套餐"),
    MARKETING_ADVISOR("营销顾问", "分析客流趋势、促销效果，生成满减/活动方案"),
    MEMBER_ADVISOR("会员顾问", "分析消费频次、流失风险，推荐召回/充值/权益方案"),
    OPERATIONS_ADVISOR("经营顾问", "生成日报周报、毛利分析、异常预警、翻台率优化"),
    KITCHEN_ADVISOR("出品顾问", "分析出菜时间、退菜原因，建议高峰简化/瓶颈优化");

    private final String displayName;
    private final String description;

    AdvisorRole(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
