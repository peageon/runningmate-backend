package com.runningmate.backend.posts.repository;

import com.runningmate.backend.member.Member;
import com.runningmate.backend.posts.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findAllByMember(Member member);
    List<Post> findAllByCreatedAt(LocalDate date);
    List<Post> findByMemberInOrderByCreatedAtDesc(List<Member> members, Pageable pageable);
    List<Post> findByMemberInAndIdLessThanOrderByCreatedAtDesc(List<Member> members, Long postId, Pageable pageable);
}
