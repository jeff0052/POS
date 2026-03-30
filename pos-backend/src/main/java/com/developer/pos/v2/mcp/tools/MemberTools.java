package com.developer.pos.v2.mcp.tools;

import com.developer.pos.v2.mcp.McpToolRegistry;
import com.developer.pos.v2.mcp.model.RiskLevel;
import com.developer.pos.v2.member.application.service.MemberApplicationService;
import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberEntity;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
public class MemberTools {

    private final McpToolRegistry registry;
    private final MemberApplicationService memberService;
    private final JpaMemberRepository memberRepository;

    public MemberTools(
            McpToolRegistry registry,
            MemberApplicationService memberService,
            JpaMemberRepository memberRepository
    ) {
        this.registry = registry;
        this.memberService = memberService;
        this.memberRepository = memberRepository;
    }

    @PostConstruct
    public void registerTools() {

        registry.register(new McpToolRegistry.ToolDefinition(
                "list_members",
                "Search and list members. Params: merchantId (Long, required), keyword (String, optional — name/phone/memberNo).",
                "member",
                "QUERY",
                null,
                params -> {
                    Long merchantId = toLong(params.get("merchantId"));
                    String keyword = params.containsKey("keyword") ? (String) params.get("keyword") : "";
                    return memberService.searchMembers(merchantId, keyword);
                }
        ));

        registry.register(new McpToolRegistry.ToolDefinition(
                "get_member_profile",
                "Get full profile for a member. Params: memberId (Long), merchantId (Long, required for ownership check).",
                "member",
                "QUERY",
                null,
                params -> {
                    Long memberId = toLong(params.get("memberId"));
                    Long merchantId = toLong(params.get("merchantId"));
                    return memberService.getMember(memberId, merchantId);
                }
        ));

        registry.register(new McpToolRegistry.ToolDefinition(
                "get_churn_risk_members",
                // TODO (P1): enhance with churn detection based on last visit date once
                // a dedicated low-engagement query is added. For now returns all active members
                // because last-visit-date filtering requires a schema/query change.
                "List all active members (placeholder — will be enhanced with churn detection " +
                        "based on last visit date in P1). Params: none.",
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
                }
        ));

        registry.register(new McpToolRegistry.ToolDefinition(
                "update_member_tier",
                "Update a member's tier code. Params: memberId (Long), tierCode (String).",
                "member",
                "ACTION",
                RiskLevel.MEDIUM,
                this::doUpdateMemberTier
        ));
    }

    @Transactional
    public Object doUpdateMemberTier(Map<String, Object> params) {
        Long memberId = toLong(params.get("memberId"));
        String tierCode = (String) params.get("tierCode");

        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId));
        String previousTier = member.getTierCode();
        member.setTierCode(tierCode.trim().toUpperCase());
        memberRepository.save(member);

        return Map.of(
                "memberId", memberId,
                "previousTierCode", previousTier,
                "newTierCode", member.getTierCode()
        );
    }

    private Long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        if (value instanceof String s) return Long.parseLong(s);
        throw new IllegalArgumentException("Cannot convert to Long: " + value);
    }
}
