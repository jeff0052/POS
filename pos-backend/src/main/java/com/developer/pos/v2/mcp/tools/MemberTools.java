package com.developer.pos.v2.mcp.tools;

import com.developer.pos.v2.mcp.ActionLogService;
import com.developer.pos.v2.mcp.McpToolRegistry;
import com.developer.pos.v2.mcp.model.ActionContext;
import com.developer.pos.v2.mcp.model.RiskLevel;
import com.developer.pos.v2.member.application.service.MemberApplicationService;
import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberEntity;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class MemberTools {

    private final McpToolRegistry registry;
    private final MemberApplicationService memberService;
    private final JpaMemberRepository memberRepository;
    private final ActionLogService actionLogService;

    public MemberTools(
            McpToolRegistry registry,
            MemberApplicationService memberService,
            JpaMemberRepository memberRepository,
            ActionLogService actionLogService
    ) {
        this.registry = registry;
        this.memberService = memberService;
        this.memberRepository = memberRepository;
        this.actionLogService = actionLogService;
    }

    @PostConstruct
    public void registerTools() {

        registry.register(new McpToolRegistry.ToolDefinition(
                "list_members",
                "Search and list members. Params: keyword (String, optional — name/phone/memberNo).",
                "member",
                "QUERY",
                null,
                params -> {
                    String keyword = params.containsKey("keyword") ? (String) params.get("keyword") : "";
                    return memberService.searchMembers(keyword);
                }
        ));

        registry.register(new McpToolRegistry.ToolDefinition(
                "get_member_profile",
                "Get full profile for a member. Params: memberId (Long).",
                "member",
                "QUERY",
                null,
                params -> {
                    Long memberId = toLong(params.get("memberId"));
                    return memberService.getMember(memberId);
                }
        ));

        registry.register(new McpToolRegistry.ToolDefinition(
                "get_churn_risk_members",
                "Identify members with zero points and zero cash balance — potential churn risk. " +
                        "Params: none (analyzes all active members).",
                "member",
                "ANALYZE",
                null,
                params -> {
                    List<MemberEntity> all = memberRepository.findAll();
                    return all.stream()
                            .filter(m -> "ACTIVE".equalsIgnoreCase(m.getMemberStatus()))
                            .map(m -> Map.of(
                                    "memberId", m.getId(),
                                    "name", m.getName(),
                                    "phone", m.getPhone(),
                                    "tierCode", m.getTierCode()
                            ))
                            .toList();
                    // TODO: enhance with account balance join once a dedicated low-engagement query
                    // is added to MemberApplicationService or JpaMemberRepository.
                }
        ));

        registry.register(new McpToolRegistry.ToolDefinition(
                "update_member_tier",
                "Update a member's tier code. Params: memberId (Long), tierCode (String).",
                "member",
                "ACTION",
                RiskLevel.MEDIUM,
                params -> {
                    Long memberId = toLong(params.get("memberId"));
                    String tierCode = (String) params.get("tierCode");

                    MemberEntity member = memberRepository.findById(memberId)
                            .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId));
                    String previousTier = member.getTierCode();
                    member.setTierCode(tierCode.trim().toUpperCase());
                    memberRepository.save(member);

                    Object result = Map.of(
                            "memberId", memberId,
                            "previousTierCode", previousTier,
                            "newTierCode", member.getTierCode()
                    );
                    ActionContext ctx = ActionContext.humanDefault();
                    actionLogService.log("update_member_tier", ctx, RiskLevel.MEDIUM, params, result);
                    return result;
                }
        ));
    }

    private Long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        if (value instanceof String s) return Long.parseLong(s);
        throw new IllegalArgumentException("Cannot convert to Long: " + value);
    }
}
