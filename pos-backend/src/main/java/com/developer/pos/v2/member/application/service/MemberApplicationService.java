package com.developer.pos.v2.member.application.service;

import com.developer.pos.v2.common.application.UseCase;
import com.developer.pos.v2.member.application.dto.BindMemberResultDto;
import com.developer.pos.v2.member.application.dto.CreateMemberDto;
import com.developer.pos.v2.member.application.dto.MemberDetailDto;
import com.developer.pos.v2.member.application.dto.MemberPointsAdjustmentResultDto;
import com.developer.pos.v2.member.application.dto.MemberPointsRecordDto;
import com.developer.pos.v2.member.application.dto.MemberRechargeRecordDto;
import com.developer.pos.v2.member.application.dto.MemberRechargeResultDto;
import com.developer.pos.v2.member.application.dto.MemberSummaryDto;
import com.developer.pos.v2.member.domain.policy.MemberDiscountPolicy;
import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberAccountEntity;
import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberEntity;
import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberPointsLedgerEntity;
import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberRechargeOrderEntity;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberAccountRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberPointsLedgerRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberRechargeOrderRepository;
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
    private final JpaMemberRechargeOrderRepository memberRechargeOrderRepository;
    private final JpaMemberPointsLedgerRepository memberPointsLedgerRepository;
    private final JpaActiveTableOrderRepository activeTableOrderRepository;

    public MemberApplicationService(
            JpaMemberRepository memberRepository,
            JpaMemberAccountRepository memberAccountRepository,
            JpaMemberRechargeOrderRepository memberRechargeOrderRepository,
            JpaMemberPointsLedgerRepository memberPointsLedgerRepository,
            JpaActiveTableOrderRepository activeTableOrderRepository
    ) {
        this.memberRepository = memberRepository;
        this.memberAccountRepository = memberAccountRepository;
        this.memberRechargeOrderRepository = memberRechargeOrderRepository;
        this.memberPointsLedgerRepository = memberPointsLedgerRepository;
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
                            account == null ? 0 : account.getCashBalanceCents(),
                            account == null ? 0 : account.getLifetimeSpendCents(),
                            account == null ? 0 : account.getLifetimeRechargeCents(),
                            member.getMemberStatus()
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

    @Transactional(readOnly = true)
    public MemberSummaryDto getMemberByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }

        MemberEntity member = memberRepository.findByPhone(phone.trim()).orElse(null);
        if (member == null || !"ACTIVE".equalsIgnoreCase(member.getMemberStatus())) {
            return null;
        }

        MemberAccountEntity account = memberAccountRepository.findByMemberId(member.getId()).orElse(null);
        return new MemberSummaryDto(
                member.getId(),
                member.getMemberNo(),
                member.getName(),
                member.getPhone(),
                member.getTierCode(),
                account == null ? 0 : account.getPointsBalance(),
                account == null ? 0 : account.getCashBalanceCents(),
                account == null ? 0 : account.getLifetimeSpendCents(),
                account == null ? 0 : account.getLifetimeRechargeCents(),
                member.getMemberStatus()
        );
    }

    @Transactional
    public CreateMemberDto createMember(Long merchantId, String name, String phone, String tierCode) {
        String normalizedPhone = phone == null ? "" : phone.trim();
        if (normalizedPhone.isBlank()) {
            throw new IllegalArgumentException("Phone must not be blank.");
        }

        MemberEntity existing = memberRepository.findByPhone(normalizedPhone).orElse(null);
        if (existing != null) {
            throw new IllegalStateException("Member phone already exists: " + normalizedPhone);
        }

        MemberEntity member = new MemberEntity();
        member.setMerchantId(merchantId);
        member.setMemberNo("MEM" + System.currentTimeMillis());
        member.setName(name == null ? "" : name.trim());
        member.setPhone(normalizedPhone);
        member.setTierCode(tierCode == null || tierCode.isBlank() ? "STANDARD" : tierCode.trim().toUpperCase());
        member.setMemberStatus("ACTIVE");
        MemberEntity savedMember = memberRepository.save(member);

        MemberAccountEntity account = new MemberAccountEntity();
        account.setMemberId(savedMember.getId());
        account.setPointsBalance(0);
        account.setCashBalanceCents(0);
        account.setLifetimeSpendCents(0);
        account.setLifetimeRechargeCents(0);
        memberAccountRepository.save(account);

        return new CreateMemberDto(
                savedMember.getId(),
                savedMember.getMemberNo(),
                savedMember.getName(),
                savedMember.getPhone(),
                savedMember.getTierCode(),
                savedMember.getMemberStatus()
        );
    }

    @Transactional
    public MemberDetailDto updateMember(Long memberId, String name, String phone, String tierCode, String memberStatus) {
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId));

        String normalizedPhone = phone == null ? "" : phone.trim();
        if (normalizedPhone.isBlank()) {
            throw new IllegalArgumentException("Phone must not be blank.");
        }

        MemberEntity existing = memberRepository.findByPhone(normalizedPhone).orElse(null);
        if (existing != null && !existing.getId().equals(memberId)) {
            throw new IllegalStateException("Member phone already exists: " + normalizedPhone);
        }

        member.setName(name == null ? "" : name.trim());
        member.setPhone(normalizedPhone);
        member.setTierCode(tierCode == null || tierCode.isBlank() ? "STANDARD" : tierCode.trim().toUpperCase());
        member.setMemberStatus(memberStatus == null || memberStatus.isBlank() ? "ACTIVE" : memberStatus.trim().toUpperCase());
        MemberEntity savedMember = memberRepository.save(member);

        MemberAccountEntity account = memberAccountRepository.findByMemberId(memberId).orElse(null);
        return new MemberDetailDto(
                savedMember.getId(),
                savedMember.getMemberNo(),
                savedMember.getName(),
                savedMember.getPhone(),
                savedMember.getTierCode(),
                savedMember.getMemberStatus(),
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
        long memberDiscountCents = MemberDiscountPolicy.calculate(activeOrder.getOriginalAmountCents(), member.getTierCode());
        activeOrder.setMemberDiscountCents(memberDiscountCents);
        long payableAmountCents = Math.max(
                0,
                activeOrder.getOriginalAmountCents() - memberDiscountCents - activeOrder.getPromotionDiscountCents()
        );
        activeOrder.setPayableAmountCents(payableAmountCents);
        activeTableOrderRepository.save(activeOrder);

        return new BindMemberResultDto(
                member.getId(),
                activeOrderId,
                member.getTierCode(),
                memberDiscountCents,
                payableAmountCents
        );
    }

    @Transactional
    public BindMemberResultDto unbindActiveOrder(String activeOrderId) {
        ActiveTableOrderEntity activeOrder = activeTableOrderRepository.findByActiveOrderId(activeOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Active order not found: " + activeOrderId));

        activeOrder.setMemberId(null);
        activeOrder.setMemberDiscountCents(0);
        long payableAmountCents = Math.max(
                0,
                activeOrder.getOriginalAmountCents() - activeOrder.getPromotionDiscountCents()
        );
        activeOrder.setPayableAmountCents(payableAmountCents);
        activeTableOrderRepository.save(activeOrder);

        return new BindMemberResultDto(
                null,
                activeOrderId,
                null,
                0,
                payableAmountCents
        );
    }

    @Transactional(readOnly = true)
    public List<MemberRechargeRecordDto> getRechargeRecords(Long merchantId) {
        java.util.Map<Long, MemberEntity> membersById = memberRepository.findAll().stream()
                .filter(member -> member.getMerchantId().equals(merchantId))
                .collect(java.util.stream.Collectors.toMap(MemberEntity::getId, member -> member));

        return memberRechargeOrderRepository.findAllByMerchantIdOrderByIdDesc(merchantId).stream()
                .map(record -> {
                    MemberEntity member = membersById.get(record.getMemberId());
                    return new MemberRechargeRecordDto(
                            record.getId(),
                            member != null ? member.getName() : "-",
                            member != null ? member.getPhone() : "-",
                            record.getAmountCents(),
                            record.getBonusAmountCents(),
                            record.getFinalStatus(),
                            record.getCreatedAt()
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MemberPointsRecordDto> getPointsRecords(Long merchantId) {
        java.util.Map<Long, MemberEntity> membersById = memberRepository.findAll().stream()
                .filter(member -> member.getMerchantId().equals(merchantId))
                .collect(java.util.stream.Collectors.toMap(MemberEntity::getId, member -> member));

        return memberPointsLedgerRepository.findAllByMerchantIdOrderByIdDesc(merchantId).stream()
                .map(record -> {
                    MemberEntity member = membersById.get(record.getMemberId());
                    return new MemberPointsRecordDto(
                            record.getId(),
                            member != null ? member.getName() : "-",
                            record.getChangeType(),
                            record.getPointsDelta(),
                            record.getSourceType(),
                            record.getCreatedAt()
                    );
                })
                .toList();
    }

    @Transactional
    public MemberRechargeResultDto rechargeMember(Long memberId, long amountCents, long bonusAmountCents, String operatorName) {
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId));
        MemberAccountEntity account = memberAccountRepository.findByMemberId(memberId)
                .orElseThrow(() -> new IllegalStateException("Member account not found: " + memberId));

        long totalTopUp = amountCents + bonusAmountCents;
        account.setCashBalanceCents(account.getCashBalanceCents() + totalTopUp);
        account.setLifetimeRechargeCents(account.getLifetimeRechargeCents() + amountCents);
        memberAccountRepository.save(account);

        MemberRechargeOrderEntity rechargeOrder = new MemberRechargeOrderEntity();
        rechargeOrder.setRechargeNo("RCH" + System.currentTimeMillis());
        rechargeOrder.setMerchantId(member.getMerchantId());
        rechargeOrder.setMemberId(memberId);
        rechargeOrder.setAmountCents(amountCents);
        rechargeOrder.setBonusAmountCents(bonusAmountCents);
        rechargeOrder.setFinalStatus("SUCCESS");
        rechargeOrder.setOperatorName(operatorName);
        memberRechargeOrderRepository.save(rechargeOrder);

        return new MemberRechargeResultDto(
                memberId,
                rechargeOrder.getRechargeNo(),
                amountCents,
                bonusAmountCents,
                account.getCashBalanceCents()
        );
    }

    @Transactional
    public MemberPointsAdjustmentResultDto adjustPoints(Long memberId, long pointsDelta, String changeType, String source, String operatorName) {
        MemberEntity member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId));
        MemberAccountEntity account = memberAccountRepository.findByMemberId(memberId)
                .orElseThrow(() -> new IllegalStateException("Member account not found: " + memberId));

        long balanceAfter = Math.max(0, account.getPointsBalance() + pointsDelta);
        account.setPointsBalance(balanceAfter);
        memberAccountRepository.save(account);

        MemberPointsLedgerEntity ledger = new MemberPointsLedgerEntity();
        ledger.setLedgerNo("PTS" + System.currentTimeMillis());
        ledger.setMerchantId(member.getMerchantId());
        ledger.setMemberId(memberId);
        ledger.setChangeType(changeType == null || changeType.isBlank() ? "ADJUST" : changeType.trim().toUpperCase());
        ledger.setPointsDelta(pointsDelta);
        ledger.setBalanceAfter(balanceAfter);
        ledger.setSourceType(source);
        ledger.setSourceRef(null);
        ledger.setOperatorName(operatorName);
        memberPointsLedgerRepository.save(ledger);

        return new MemberPointsAdjustmentResultDto(
                memberId,
                ledger.getLedgerNo(),
                pointsDelta,
                balanceAfter
        );
    }
}
