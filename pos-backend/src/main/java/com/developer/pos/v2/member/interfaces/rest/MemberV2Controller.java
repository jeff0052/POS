package com.developer.pos.v2.member.interfaces.rest;

import com.developer.pos.common.response.ApiResponse;
import com.developer.pos.v2.common.interfaces.rest.V2Api;
import com.developer.pos.v2.member.application.dto.BindMemberResultDto;
import com.developer.pos.v2.member.application.dto.CreateMemberDto;
import com.developer.pos.v2.member.application.dto.MemberDetailDto;
import com.developer.pos.v2.member.application.dto.MemberPointsAdjustmentResultDto;
import com.developer.pos.v2.member.application.dto.MemberPointsRecordDto;
import com.developer.pos.v2.member.application.dto.MemberRechargeRecordDto;
import com.developer.pos.v2.member.application.dto.MemberRechargeResultDto;
import com.developer.pos.v2.member.application.dto.MemberSummaryDto;
import com.developer.pos.v2.member.application.service.MemberApplicationService;
import com.developer.pos.v2.member.interfaces.rest.request.BindMemberActiveOrderRequest;
import com.developer.pos.v2.member.interfaces.rest.request.CreateMemberRequest;
import com.developer.pos.v2.member.interfaces.rest.request.MemberPointsAdjustmentRequest;
import com.developer.pos.v2.member.interfaces.rest.request.MemberRechargeRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v2/members")
public class MemberV2Controller implements V2Api {

    private final MemberApplicationService memberApplicationService;

    public MemberV2Controller(MemberApplicationService memberApplicationService) {
        this.memberApplicationService = memberApplicationService;
    }

    @GetMapping
    public ApiResponse<List<MemberSummaryDto>> search(@RequestParam(defaultValue = "") String keyword) {
        return ApiResponse.success(memberApplicationService.searchMembers(keyword));
    }

    @GetMapping("/by-phone")
    public ApiResponse<MemberSummaryDto> getByPhone(@RequestParam String phone) {
        return ApiResponse.success(memberApplicationService.getMemberByPhone(phone));
    }

    @GetMapping("/{memberId}")
    public ApiResponse<MemberDetailDto> getMember(@PathVariable Long memberId) {
        return ApiResponse.success(memberApplicationService.getMember(memberId));
    }

    @GetMapping("/recharge-records")
    public ApiResponse<List<MemberRechargeRecordDto>> getRechargeRecords(@RequestParam Long merchantId) {
        return ApiResponse.success(memberApplicationService.getRechargeRecords(merchantId));
    }

    @GetMapping("/points-ledger")
    public ApiResponse<List<MemberPointsRecordDto>> getPointsRecords(@RequestParam Long merchantId) {
        return ApiResponse.success(memberApplicationService.getPointsRecords(merchantId));
    }

    @PostMapping
    public ApiResponse<CreateMemberDto> createMember(@Valid @RequestBody CreateMemberRequest request) {
        return ApiResponse.success(
                memberApplicationService.createMember(
                        request.merchantId(),
                        request.name(),
                        request.phone(),
                        request.tierCode()
                )
        );
    }

    @PostMapping("/{memberId}/bind-active-order")
    public ApiResponse<BindMemberResultDto> bindActiveOrder(
            @PathVariable Long memberId,
            @Valid @RequestBody BindMemberActiveOrderRequest request
    ) {
        return ApiResponse.success(memberApplicationService.bindActiveOrder(memberId, request.activeOrderId()));
    }

    @PostMapping("/unbind-active-order")
    public ApiResponse<BindMemberResultDto> unbindActiveOrder(
            @Valid @RequestBody BindMemberActiveOrderRequest request
    ) {
        return ApiResponse.success(memberApplicationService.unbindActiveOrder(request.activeOrderId()));
    }

    @PostMapping("/{memberId}/recharge")
    public ApiResponse<MemberRechargeResultDto> rechargeMember(
            @PathVariable Long memberId,
            @Valid @RequestBody MemberRechargeRequest request
    ) {
        return ApiResponse.success(
                memberApplicationService.rechargeMember(
                        memberId,
                        request.amountCents(),
                        request.bonusAmountCents(),
                        request.operatorName()
                )
        );
    }

    @PostMapping("/{memberId}/points-adjustment")
    public ApiResponse<MemberPointsAdjustmentResultDto> adjustPoints(
            @PathVariable Long memberId,
            @Valid @RequestBody MemberPointsAdjustmentRequest request
    ) {
        return ApiResponse.success(
                memberApplicationService.adjustPoints(
                        memberId,
                        request.pointsDelta(),
                        request.changeType(),
                        request.source(),
                        request.operatorName()
                )
        );
    }
}
