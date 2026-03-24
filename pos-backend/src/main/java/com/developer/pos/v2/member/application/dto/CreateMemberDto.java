package com.developer.pos.v2.member.application.dto;

public record CreateMemberDto(
        Long id,
        String memberNo,
        String name,
        String phone,
        String tierCode,
        String memberStatus
) {
}
