package com.developer.pos.v2.member.interfaces.rest;

import com.developer.pos.auth.security.AuthenticatedActor;
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
import com.developer.pos.v2.member.interfaces.rest.request.UpdateMemberRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    public ApiResponse<List<MemberSummaryDto>> search(
            @RequestParam(defaultValue = "") String keyword) {
        AuthenticatedActor actor = currentActor();
        return ApiResponse.success(memberApplicationService.searchMembers(actor.merchantId(), keyword));
    }

    @GetMapping("/by-phone")
    public ApiResponse<MemberSummaryDto> getByPhone(@RequestParam String phone) {
        AuthenticatedActor actor = currentActor();
        return ApiResponse.success(memberApplicationService.getMemberByPhone(actor.merchantId(), phone));
    }

    @GetMapping("/{memberId}")
    public ApiResponse<MemberDetailDto> getMember(@PathVariable Long memberId) {
        AuthenticatedActor actor = currentActor();
        return ApiResponse.success(memberApplicationService.getMember(memberId, actor.merchantId()));
    }

    @PutMapping("/{memberId}")
    public ApiResponse<MemberDetailDto> updateMember(
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateMemberRequest request
    ) {
        AuthenticatedActor actor = currentActor();
        return ApiResponse.success(
                memberApplicationService.updateMember(
                        memberId,
                        actor.merchantId(),
                        request.name(),
                        request.phone(),
                        request.tierCode(),
                        request.memberStatus()
                )
        );
    }

    @GetMapping("/recharge-records")
    public ApiResponse<List<MemberRechargeRecordDto>> getRechargeRecords() {
        AuthenticatedActor actor = currentActor();
        return ApiResponse.success(memberApplicationService.getRechargeRecords(actor.merchantId()));
    }

    @GetMapping("/points-ledger")
    public ApiResponse<List<MemberPointsRecordDto>> getPointsRecords() {
        AuthenticatedActor actor = currentActor();
        return ApiResponse.success(memberApplicationService.getPointsRecords(actor.merchantId()));
    }

    @PostMapping
    public ApiResponse<CreateMemberDto> createMember(@Valid @RequestBody CreateMemberRequest request) {
        AuthenticatedActor actor = currentActor();
        return ApiResponse.success(
                memberApplicationService.createMember(
                        actor.merchantId(),
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
        AuthenticatedActor actor = currentActor();
        return ApiResponse.success(memberApplicationService.bindActiveOrder(memberId, actor.merchantId(), request.activeOrderId()));
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
        AuthenticatedActor actor = currentActor();
        return ApiResponse.success(
                memberApplicationService.rechargeMember(
                        memberId,
                        actor.merchantId(),
                        request.amountCents(),
                        request.bonusAmountCents(),
                        actor.username()
                )
        );
    }

    @PostMapping("/{memberId}/points-adjustment")
    public ApiResponse<MemberPointsAdjustmentResultDto> adjustPoints(
            @PathVariable Long memberId,
            @Valid @RequestBody MemberPointsAdjustmentRequest request
    ) {
        AuthenticatedActor actor = currentActor();
        return ApiResponse.success(
                memberApplicationService.adjustPoints(
                        memberId,
                        actor.merchantId(),
                        request.pointsDelta(),
                        request.changeType(),
                        request.source(),
                        actor.username()
                )
        );
    }

    private AuthenticatedActor currentActor() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedActor actor)) {
            throw new IllegalStateException("No authenticated actor in security context");
        }
        return actor;
    }
}
