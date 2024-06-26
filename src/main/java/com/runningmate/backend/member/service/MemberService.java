package com.runningmate.backend.member.service;

import com.runningmate.backend.exception.ExistsConflictException;
import com.runningmate.backend.exception.ResourceNotFoundException;
import com.runningmate.backend.member.Follow;
import com.runningmate.backend.member.Member;
import com.runningmate.backend.member.dto.MemberDto;
import com.runningmate.backend.member.dto.MemberListResponseDto;
import com.runningmate.backend.member.dto.MemberSignupRequest;
import com.runningmate.backend.member.Role;
import com.runningmate.backend.exception.FieldExistsException;
import com.runningmate.backend.member.repository.FollowRepository;
import com.runningmate.backend.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final FollowRepository followRepository;


    public MemberDto signup(MemberSignupRequest memberSignupRequest) throws FieldExistsException {
        List<String> exists = checkExistingFields(memberSignupRequest);

        if (!exists.isEmpty()) {
            throw new FieldExistsException(exists);
        }

        Member member = createMember(memberSignupRequest);

        Member savedMember = memberRepository.save(member);
        return MemberDto.fromEntity(savedMember);
    }

    public Member getMemberByUsername(String username) {
        return memberRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Member with User Name: " + username + " not found."));
    }

    public Member getMemberById(Long memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new ResourceNotFoundException("Member with member_id: " + memberId + " not found."));
    }

    public void followUser(Member follower, Member following) {
        if (followRepository.existsByFollowerAndFollowing(follower, following)) {
            throw new ExistsConflictException(follower.getUsername() + " is already following " + following.getUsername());
        }
        Follow newFollow = createFollow(follower, following);
        followRepository.save(newFollow);
    }

    public void unfollowUser(Member follower, Member following) {
        Follow follow = followRepository.findByFollowerAndFollowing(follower, following)
                .orElseThrow(() -> new ResourceNotFoundException(follower.getUsername() + " is not following " + following.getUsername()));
        followRepository.delete(follow);
    }

    public MemberListResponseDto getFollowerList(Long memberId) {
        Member member = getMemberById(memberId);
        List<Follow> follows = followRepository.findByFollowing(member);
        return createMemberListResponseDto("followers", follows);
    }

    public MemberListResponseDto getFollowingList(Long memberId) {
        Member member = getMemberById(memberId);
        List<Follow> follows = followRepository.findByFollower(member);
        return createMemberListResponseDto("followings", follows);
    }

    public Member updateMemberProfilePicture(Member member, String url) {
        member.updateProfilePicture(url != null ? url : Member.DEFAULT_PROFILE_PIC);
        return memberRepository.save(member);
    }

    private MemberListResponseDto createMemberListResponseDto(String type, List<Follow> follows) {
        List<MemberDto> memberList = new ArrayList<>();
        for (Follow follow: follows) {
            memberList.add(MemberDto.fromEntity(follow.getFollower()));
        }
        MemberListResponseDto memberListResponseDto = new MemberListResponseDto(type, memberList);
        return memberListResponseDto;
    }

    private Follow createFollow(Member follower, Member following) {
        return Follow.builder()
                .follower(follower)
                .following(following)
                .build();
    }

    private List<String> checkExistingFields(MemberSignupRequest request) {
        List<String> exists = new ArrayList<>();

        if (memberRepository.existsByEmail(request.getEmail())) {
            exists.add("email");
        }
        if (memberRepository.existsByUsername(request.getUsername())) {
            exists.add("username");
        }

        return exists;
    }

    private Member createMember(MemberSignupRequest request) {
        return Member.builder()
                .email(request.getEmail())
                .name(request.getName())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();
    }

}
