package com.developer.pos.member.dto;

import java.util.List;

public record MemberTierDto(
    Long id,
    String name,
    String upgradeRule,
    List<String> benefits
) {
}
