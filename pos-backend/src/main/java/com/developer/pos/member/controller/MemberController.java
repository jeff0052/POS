package com.developer.pos.member.controller;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.member.dto.MemberDto;
import com.developer.pos.member.dto.MemberTierDto;
import com.developer.pos.member.dto.PointsRecordDto;
import com.developer.pos.member.dto.RechargeRecordDto;
import com.developer.pos.member.service.MemberService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    public ApiResponse<List<MemberDto>> listMembers() {
        return ApiResponse.success(memberService.listMembers());
    }

    @GetMapping("/tiers")
    public ApiResponse<List<MemberTierDto>> listTiers() {
        return ApiResponse.success(memberService.listTiers());
    }

    @GetMapping("/recharges")
    public ApiResponse<List<RechargeRecordDto>> listRecharges() {
        return ApiResponse.success(memberService.listRecharges());
    }

    @GetMapping("/points")
    public ApiResponse<List<PointsRecordDto>> listPointsLedger() {
        return ApiResponse.success(memberService.listPointsLedger());
    }
}
