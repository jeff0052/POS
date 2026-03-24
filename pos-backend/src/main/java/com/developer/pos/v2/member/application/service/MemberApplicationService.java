package com.developer.pos.v2.member.application.service;

import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.member.application.dto.BindMemberResultDto;
import com.developer.pos.v2.member.application.dto.MemberDetailDto;
import com.developer.pos.v2.member.application.dto.MemberSummaryDto;
import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberAccountEntity;
import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberEntity;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberAccountRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberRepository;
import com.developer.pos.v2.order.infrastructure.persistence.entity.ActiveTableOrderEntity;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaActiveTableOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MemberApplicationService implements UseCase {

    private final JpaMemberRepository memberRepository;
    private final JpaMemberAccountRepository memberAccountRepository;
    private final JpaActiveTableOrderRepository activeTableOrderRepository;

    public MemberApplicationService(
            JpaMemberRepository memberRepository,
            JpaMemberAccountRepository memberAccountRepository,
            JpaActiveTableOrderRepository activeTableOrderRepository
    ) {
        this.memberRepository = memberRepository;
        this.memberAccountRepository = memberAccountRepository;
        this.activeTableOrderRepository = activeTableOrderRepository;
    }

    @Transactional(readOnly = true)
    public List<MemberSummaryDto> searchMembers(String keyword) {
        return memberRepository.searchActiveMembers(keyword == null ? "" : keyword.trim()).stream()
                .map(member -> {
                    MemberAccountEntity account = memberAccountRepository.findByMemberId(member.getId()).orElse(null);
                    return new MemberSummaryDto(
                            member.getId(),
                            member.getMemberNo(),
                            member.getName(),
                            member.getPhone(),
                            member.getTierCode(),
                            account == null ? 0 : account.getPointsBalance(),
                            account == null ? 0 : account.getCashBalanceCents()
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public MemberDetailDto getMember(Long memberId) {
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId));
        MemberAccountEntity account = memberAccountRepository.findByMemberId(memberId).orElse(null);

        return new MemberDetailDto(
                member.getId(),
                member.getMemberNo(),
                member.getName(),
                member.getPhone(),
                member.getTierCode(),
                member.getMemberStatus(),
                account == null ? 0 : account.getPointsBalance(),
                account == null ? 0 : account.getCashBalanceCents(),
                account == null ? 0 : account.getLifetimeSpendCents(),
                account == null ? 0 : account.getLifetimeRechargeCents()
        );
    }

    @Transactional
    public BindMemberResultDto bindActiveOrder(Long memberId, String activeOrderId) {
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId));
        ActiveTableOrderEntity activeOrder = activeTableOrderRepository.findByActiveOrderId(activeOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Active order not found: " + activeOrderId));

        activeOrder.setMemberId(member.getId());
        activeTableOrderRepository.save(activeOrder);

        return new BindMemberResultDto(member.getId(), activeOrderId, member.getTierCode());
    }
}
