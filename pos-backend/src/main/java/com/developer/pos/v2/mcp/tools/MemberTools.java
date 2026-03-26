package com.developer.pos.v2.mcp.tools;

import com.developer.pos.v2.mcp.core.ActionContext;
import com.developer.pos.v2.mcp.core.McpTool;
import com.developer.pos.v2.member.application.service.MemberApplicationService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component("memberSearch")
class SearchMembers implements McpTool {
    private final MemberApplicationService memberService;
    SearchMembers(MemberApplicationService memberService) { this.memberService = memberService; }

    @Override public String name() { return "member.search"; }
    @Override public String description() { return "Search members by phone number or name"; }
    @Override public Map<String, Object> inputSchema() {
        return Map.of("type", "object", "properties", Map.of(
                "keyword", Map.of("type", "string", "description", "Phone or name to search")
        ), "required", List.of("keyword"));
    }
    @Override public Object execute(Map<String, Object> params, ActionContext ctx) {
        String keyword = (String) params.get("keyword");
        return memberService.searchMembers(keyword);
    }
}

@Component("memberGetDetail")
class GetMemberDetail implements McpTool {
    private final MemberApplicationService memberService;
    GetMemberDetail(MemberApplicationService memberService) { this.memberService = memberService; }

    @Override public String name() { return "member.get_detail"; }
    @Override public String description() { return "Get full member details including account balance, points, tier"; }
    @Override public Map<String, Object> inputSchema() {
        return Map.of("type", "object", "properties", Map.of(
                "memberId", Map.of("type", "number")
        ), "required", List.of("memberId"));
    }
    @Override public Object execute(Map<String, Object> params, ActionContext ctx) {
        Long memberId = ((Number) params.get("memberId")).longValue();
        return memberService.getMember(memberId);
    }
}

@Component("memberRecharge")
class RechargeMember implements McpTool {
    private final MemberApplicationService memberService;
    RechargeMember(MemberApplicationService memberService) { this.memberService = memberService; }

    @Override public String name() { return "member.recharge"; }
    @Override public String description() { return "Recharge a member's cash balance (top-up stored value)"; }
    @Override public Map<String, Object> inputSchema() {
        return Map.of("type", "object", "properties", Map.of(
                "memberId", Map.of("type", "number"),
                "amountCents", Map.of("type", "number", "description", "Recharge amount in cents"),
                "bonusAmountCents", Map.of("type", "number", "description", "Bonus amount in cents (default 0)")
        ), "required", List.of("memberId", "amountCents"));
    }
    @Override public Object execute(Map<String, Object> params, ActionContext ctx) {
        Long memberId = ((Number) params.get("memberId")).longValue();
        long amountCents = ((Number) params.get("amountCents")).longValue();
        long bonusCents = params.containsKey("bonusAmountCents") ? ((Number) params.get("bonusAmountCents")).longValue() : 0;
        String operator = ctx.actorId() != null ? ctx.actorId() : "MCP";
        return memberService.rechargeMember(memberId, amountCents, bonusCents, operator);
    }
}
