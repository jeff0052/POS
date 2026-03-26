package com.developer.pos.v2.member.application.service;

import com.developer.pos.v2.member.application.dto.CreateMemberDto;
import com.developer.pos.v2.member.application.dto.MemberDetailDto;
import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberAccountEntity;
import com.developer.pos.v2.member.infrastructure.persistence.entity.MemberEntity;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberAccountRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberPointsLedgerRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberRechargeOrderRepository;
import com.developer.pos.v2.member.infrastructure.persistence.repository.JpaMemberRepository;
import com.developer.pos.v2.order.infrastructure.persistence.repository.JpaActiveTableOrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberApplicationServiceTest {

    @Mock private JpaMemberRepository memberRepository;
    @Mock private JpaMemberAccountRepository memberAccountRepository;
    @Mock private JpaMemberRechargeOrderRepository memberRechargeOrderRepository;
    @Mock private JpaMemberPointsLedgerRepository memberPointsLedgerRepository;
    @Mock private JpaActiveTableOrderRepository activeTableOrderRepository;

    @InjectMocks
    private MemberApplicationService service;

    @Nested
    @DisplayName("createMember")
    class CreateMember {

        @Test
        @DisplayName("creates member and account for new phone number")
        void createsNewMember() {
            when(memberRepository.findByPhone("91234567"))
                    .thenReturn(Optional.empty());
            when(memberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(memberAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CreateMemberDto result = service.createMember(1L, "Test User", "91234567", null);

            assertNotNull(result);
            verify(memberRepository).save(any());
            verify(memberAccountRepository).save(any());
        }

        @Test
        @DisplayName("throws when phone number already registered")
        void throwsOnDuplicate() {
            MemberEntity existing = new MemberEntity();
            ReflectionTestUtils.setField(existing, "id", 1L);
            existing.setPhone("91234567");
            when(memberRepository.findByPhone("91234567"))
                    .thenReturn(Optional.of(existing));

            assertThrows(IllegalStateException.class,
                    () -> service.createMember(1L, "Test User", "91234567", null));
        }
    }

    @Nested
    @DisplayName("getMember")
    class GetMember {

        @Test
        @DisplayName("returns member with account when found")
        void returnsMember() {
            MemberEntity member = new MemberEntity();
            ReflectionTestUtils.setField(member, "id", 1L);
            member.setMerchantId(1L);
            member.setMemberNo("MEM001");
            member.setPhone("91234567");
            member.setName("Test User");
            member.setTierCode("STANDARD");
            member.setMemberStatus("ACTIVE");
            when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

            MemberAccountEntity account = new MemberAccountEntity();
            ReflectionTestUtils.setField(account, "id", 1L);
            account.setMemberId(1L);
            account.setPointsBalance(100L);
            account.setCashBalanceCents(5000L);
            account.setLifetimeSpendCents(50000L);
            account.setLifetimeRechargeCents(0L);
            when(memberAccountRepository.findByMemberId(1L))
                    .thenReturn(Optional.of(account));

            MemberDetailDto result = service.getMember(1L);

            assertNotNull(result);
            assertEquals("91234567", result.phone());
            assertEquals(100L, result.pointsBalance());
            assertEquals(5000L, result.cashBalanceCents());
        }

        @Test
        @DisplayName("throws when member not found")
        void throwsWhenNotFound() {
            when(memberRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> service.getMember(999L));
        }
    }
}
