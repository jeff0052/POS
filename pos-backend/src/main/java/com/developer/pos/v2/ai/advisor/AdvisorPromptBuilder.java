package com.developer.pos.v2.ai.advisor;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Builds LLM prompts for each advisor role based on assembled context.
 * The output prompt is sent to an external LLM API to generate recommendations.
 */
public class AdvisorPromptBuilder {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String build(AdvisorContext context) {
        String roleInstruction = switch (context.role()) {
            case MENU_ADVISOR -> """
                你是一个餐厅菜单顾问。根据以下销售数据和商品目录，分析：
                1. 哪些菜品销量低且毛利差，建议下架
                2. 哪些菜品是明星产品，建议主推
                3. 是否有适合的套餐组合建议
                输出具体的建议方案，每条建议包含：标题、原因、建议操作。
                """;
            case MARKETING_ADVISOR -> """
                你是一个餐厅营销顾问。根据以下销售数据和促销规则，分析：
                1. 当前促销效果如何
                2. 是否需要新的满减/折扣活动
                3. 针对客流低的时段提出具体方案
                输出具体的促销草案，包含：规则名称、条件、奖励、建议执行时间。
                """;
            case MEMBER_ADVISOR -> """
                你是一个餐厅会员顾问。根据以下会员数据，分析：
                1. 高价值会员是谁，如何维护
                2. 哪些会员有流失风险（30天未消费）
                3. 是否需要调整充值活动或积分规则
                输出具体的会员运营建议。
                """;
            case OPERATIONS_ADVISOR -> """
                你是一个餐厅经营顾问。根据以下经营数据，生成：
                1. 今日经营摘要（销售额、订单数、客单价、翻台率）
                2. 与历史同期对比，标记异常指标
                3. 具体的改善建议
                用老板能听懂的话输出。
                """;
            case KITCHEN_ADVISOR -> """
                你是一个餐厅出品顾问。根据以下订单数据，分析：
                1. 平均出菜时间是否正常
                2. 退菜/取消最多的菜品
                3. 高峰时段的出品瓶颈
                输出具体的出品优化建议。
                """;
        };

        StringBuilder sb = new StringBuilder();
        sb.append(roleInstruction).append("\n\n");
        sb.append("## 数据上下文\n\n");

        try {
            if (context.salesData() != null) {
                sb.append("### 销售数据\n```json\n").append(MAPPER.writeValueAsString(context.salesData())).append("\n```\n\n");
            }
            if (context.orderData() != null) {
                sb.append("### 订单数据\n```json\n").append(MAPPER.writeValueAsString(context.orderData())).append("\n```\n\n");
            }
            if (context.catalogData() != null) {
                sb.append("### 商品目录\n```json\n").append(MAPPER.writeValueAsString(context.catalogData())).append("\n```\n\n");
            }
            if (context.promotionData() != null) {
                sb.append("### 促销规则\n```json\n").append(MAPPER.writeValueAsString(context.promotionData())).append("\n```\n\n");
            }
        } catch (Exception e) {
            sb.append("(data serialization error: ").append(e.getMessage()).append(")\n");
        }

        sb.append("请用中文输出建议，每条建议包含：标题、风险等级(LOW/MEDIUM/HIGH)、具体操作建议。\n");
        sb.append("输出格式为 JSON 数组：[{\"title\": \"...\", \"summary\": \"...\", \"riskLevel\": \"...\", \"proposedAction\": \"...\"}]\n");

        return sb.toString();
    }
}
