package com.example.tmta.appointment;

import com.example.tmta.common.dto.MemberInfo;
import com.example.tmta.appointment.dto.AppointmentDetailResponseDto;
import com.example.tmta.appointment.dto.AppointmentListResponseDto;
import com.example.tmta.appointment.dto.AppointmentTimeTableResponseDto;
import com.example.tmta.appointment.entity.Appointment;
import com.example.tmta.appointment.entity.AppointmentDate;
import com.example.tmta.member.entity.Member;
import com.example.tmta.team.entity.TeamMembers;
import com.example.tmta.appointment.entity.type.AppointmentState;
import com.example.tmta.team.entity.type.TeamRole;
import com.example.tmta.appointment.AppointmentSlotCalculator;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class AppointmentQueryAssembler {

    /** 약속 상세 응답 DTO를 조립합니다. */
    public AppointmentDetailResponseDto toAppointmentDetailResponse(Appointment appointment,
                                                                    List<TeamMembers> memberships,
                                                                    Map<Long, Member> memberMap) {
        List<LocalDate> dates = appointment.getAppointmentDateList().stream()
                .map(AppointmentDate::getDate)
                .sorted()
                .toList();
        LocalDate startDate = null;
        LocalDate endDate = null;
        if (!dates.isEmpty()) {
            startDate = dates.get(0);
            endDate = dates.get(dates.size() - 1);
        }
        LocalDate confirmedDate = null;
        if (appointment.getState() == AppointmentState.CONFIRMED || appointment.getState() == AppointmentState.COMPLETE) {
            confirmedDate = appointment.getConfirmedDate();
        }

        return new AppointmentDetailResponseDto(
                appointment.getName(),
                resolveOrganizerName(appointment, memberMap, memberships),
                (long) memberships.size(),
                startDate,
                endDate,
                memberships.stream()
                        .map(membership -> toMemberInfo(membership, memberMap.get(membership.getMemberId())))
                        .toList(),
                appointment.getDescription(),
                appointment.getState(),
                confirmedDate
        );
    }

    /** 약속 후보 시간 응답 DTO를 조립합니다. */
    public AppointmentListResponseDto toCandidateResponse(UUID appointmentId, List<AppointmentSlotCalculator.SlotAggregate> slots) {
        return new AppointmentListResponseDto(
                appointmentId,
                slots.stream().map(this::toAvailableDateTime).toList()
        );
    }

    /** 약속 타임테이블 응답 DTO를 조립합니다. */
    public AppointmentTimeTableResponseDto toTimeTableResponse(UUID appointmentId, List<AppointmentSlotCalculator.SlotAggregate> slots) {
        return new AppointmentTimeTableResponseDto(
                appointmentId,
                slots.stream().map(this::toTimeTableSlot).toList()
        );
    }

    /** 약속 생성자 표시 이름을 계산합니다. */
    private String resolveOrganizerName(Appointment appointment, Map<Long, Member> memberMap, List<TeamMembers> memberships) {
        Member creator = memberMap.get(appointment.getCreatedByMemberId());
        if (creator == null) {
            TeamMembers leader = memberships.stream()
                    .filter(m -> m.getTeamRole() == TeamRole.ADMIN)
                    .findFirst()
                    .orElse(null);
            if (leader == null) {
                return null;
            }
            Member leaderMember = memberMap.get(leader.getMemberId());
            return leaderMember == null ? null : (leaderMember.getNickName() != null ? leaderMember.getNickName() : leaderMember.getName());
        }
        return creator.getNickName() != null ? creator.getNickName() : creator.getName();
    }

    /** 멤버 정보를 응답용 DTO로 변환합니다. */
    private MemberInfo toMemberInfo(TeamMembers membership, Member member) {
        if (member == null) {
            return MemberInfo.of(membership.getMemberId(), null, null);
        }
        String displayName = membership.getTeamNickName() != null ? membership.getTeamNickName() : member.getNickName();
        if (displayName == null) {
            displayName = member.getName();
        }
        String profileBucket = membership.getTeamProfileImageBucket() != null
                ? membership.getTeamProfileImageBucket()
                : member.getProfileImageBucket();
        String profileKey = membership.getTeamProfileImageKey() != null
                ? membership.getTeamProfileImageKey()
                : member.getProfileImageKey();
        return MemberInfo.of(member.getId(), displayName, composeImagePath(profileBucket, profileKey));
    }

    private String composeImagePath(String bucket, String key) {
        if (bucket == null || bucket.isBlank() || key == null || key.isBlank()) {
            return null;
        }
        return bucket + "/" + key;
    }

    /** 후보 슬롯을 목록 응답용 DTO로 변환합니다. */
    private AppointmentListResponseDto.AvailableDateTime toAvailableDateTime(AppointmentSlotCalculator.SlotAggregate aggregate) {
        return new AppointmentListResponseDto.AvailableDateTime(
                aggregate.date(),
                aggregate.startTime(),
                aggregate.endTime(),
                (long) aggregate.memberIds().size(),
                new ArrayList<>(aggregate.memberNames())
        );
    }

    /** 후보 슬롯을 타임테이블 응답용 DTO로 변환합니다. */
    private AppointmentTimeTableResponseDto.AvailableDateTimeSlot toTimeTableSlot(AppointmentSlotCalculator.SlotAggregate aggregate) {
        return new AppointmentTimeTableResponseDto.AvailableDateTimeSlot(aggregate.date(), aggregate.startTime());
    }
}
