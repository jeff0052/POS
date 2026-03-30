package com.developer.pos.v2.member.application.service;

import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberAccountEntity;
import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberPointsLedgerEntity;
import com.developer.pos.v2.member.infrastructure.persistence.entity.PointsBatchEntity;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberAccountRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberPointsLedgerRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaPointsBatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PointsExpiryService {

    private static final Logger log = LoggerFactory.getLogger(PointsExpiryService.class);

    private final JpaPointsBatchRepository pointsBatchRepo;
    private final JpaMemberAccountRepository memberAccountRepo;
    private final JpaMemberPointsLedgerRepository memberPointsLedgerRepo;
    private final JpaMemberRepository memberRepo;

    public PointsExpiryService(
            JpaPointsBatchRepository pointsBatchRepo,
            JpaMemberAccountRepository memberAccountRepo,
            JpaMemberPointsLedgerRepository memberPointsLedgerRepo,
            JpaMemberRepository memberRepo
    ) {
        this.pointsBatchRepo = pointsBatchRepo;
        this.memberAccountRepo = memberAccountRepo;
        this.memberPointsLedgerRepo = memberPointsLedgerRepo;
        this.memberRepo = memberRepo;
    }

    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void expirePointsBatches() {
        LocalDateTime now = LocalDateTime.now();
        List<PointsBatchEntity> expiredBatches = pointsBatchRepo
                .findByBatchStatusAndExpiresAtBefore("ACTIVE", now);

        if (expiredBatches.isEmpty()) return;

        Map<Long, Long> expiredByMember = new HashMap<>();
        for (PointsBatchEntity batch : expiredBatches) {
            if (batch.getRemainingPoints() > 0) {
                expiredByMember.merge(batch.getMemberId(), batch.getRemainingPoints(), Long::sum);
            }
            batch.setExpiredPoints(batch.getRemainingPoints());
            batch.setRemainingPoints(0);
            batch.setBatchStatus("EXPIRED");
            batch.setExpiredAt(now);
            pointsBatchRepo.save(batch);
        }

        for (Map.Entry<Long, Long> entry : expiredByMember.entrySet()) {
            Long memberId = entry.getKey();
            long toExpire = entry.getValue();

            MemberAccountEntity account = memberAccountRepo.findByMemberId(memberId).orElse(null);
            if (account == null) continue;

            long newBalance = Math.max(0, account.getPointsBalance() - toExpire);
            long newAvailable = Math.max(0, account.getAvailablePoints() - toExpire);
            account.setPointsBalance(newBalance);
            account.setAvailablePoints(newAvailable);
            memberAccountRepo.save(account);

            Long merchantId = memberRepo.findById(memberId)
                    .map(m -> m.getMerchantId()).orElse(null);

            if (merchantId == null) {
                log.warn("Skipping expiry ledger for deleted member {}", memberId);
                continue;
            }

            MemberPointsLedgerEntity ledger = new MemberPointsLedgerEntity();
            ledger.setLedgerNo("EXP" + System.currentTimeMillis() + "_" + memberId);
            ledger.setMerchantId(merchantId);
            ledger.setMemberId(memberId);
            ledger.setChangeType("EXPIRE");
            ledger.setPointsDelta(-toExpire);
            ledger.setBalanceAfter(newBalance);
            ledger.setSourceType("EXPIRY");
            memberPointsLedgerRepo.save(ledger);
        }

        long totalExpired = expiredByMember.values().stream().mapToLong(Long::longValue).sum();
        log.info("Expired {} points batches, {} total points", expiredBatches.size(), totalExpired);
    }
}
