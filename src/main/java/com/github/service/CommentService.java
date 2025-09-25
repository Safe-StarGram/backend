package com.github.service;

import com.github.dto.CommentCreateRequest;
import com.github.entity.CommentEntity;
import com.github.entity.UserEntity;
import com.github.exception.CommentNotFoundException;
import com.github.repository.CommentJdbcRepository;
import com.github.repository.UserJdbcRepository;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentJdbcRepository commentRepository;
    private final UserJdbcRepository userRepository;

    @Value("${api.base-url}")
    private String baseUrl;

    @Transactional
    public CommentEntity createComment(CommentCreateRequest request) {
        System.out.println("=== 댓글 작성 중간 진행 ===");
        System.out.println("Request: " + request);

        // 사용자 정보 조회
        UserEntity user = userRepository.findById(request.getUserId().intValue());
        if (user == null) {
            throw new RuntimeException("사용자 정보를 찾을 수 없습니다.");
        }

        CommentEntity comment = CommentEntity.builder()
                .postId(request.getPostId())
                .userId(request.getUserId())
                .userName(user.getName())        // 사용자 정보도 한 번에 포함
                .positionId(convertToInteger(user.getPosition()))
                .departmentId(convertToInteger(user.getDepartment()))
                .profilePhotoUrl(convertToFullUrl(user.getProfilePhotoUrl())) // 프로필 이미지 URL 포함
                .message(request.getMessage())
                .createdAt(LocalDateTime.now())  // 생성 시 현재 시간 설정
                .updatedAt(null)                 // 생성 시 null로 설정
                .build();

        CommentEntity savedComment = commentRepository.insert(comment);

        System.out.println("Comment created: " + savedComment);
        System.out.println("=== 댓글 작성 중간 완료 ===");

        return savedComment;
    }


    @Transactional(readOnly = true)
    public List<CommentEntity> getCommentsByPostId(Long postId) {
        System.out.println("=== 게시글별 댓글 조회 진행 ===");
        System.out.println("PostId: " + postId);

        List<CommentEntity> comments = commentRepository.findByPostId(postId);
        
        // 각 댓글에 사용자 정보 추가
        for (CommentEntity comment : comments) {
            UserEntity user = userRepository.findById(comment.getUserId().intValue());
            if (user != null) {
                comment.setUserName(user.getName());
                comment.setPositionId(convertToInteger(user.getPosition()));
                comment.setDepartmentId(convertToInteger(user.getDepartment()));
                // 프로필 이미지 URL 설정
                comment.setProfilePhotoUrl(convertToFullUrl(user.getProfilePhotoUrl()));
            }
        }

        System.out.println("Found " + comments.size() + " comments for postId: " + postId);
        System.out.println("=== 게시글별 댓글 조회 완료 ===");

        return comments;
    }

    @Transactional
    public CommentEntity updateComment(Long commentId, String message) {
        System.out.println("=== 댓글 수정 중간 진행 ===");
        System.out.println("CommentId: " + commentId + ", Message: " + message);

        CommentEntity existingComment = commentRepository.findById(commentId);
        if (existingComment == null) {
            throw new CommentNotFoundException("댓글을 찾을 수 없습니다.");
        }

        commentRepository.update(commentId, message);
        CommentEntity updatedComment = commentRepository.findById(commentId);

        // 사용자 정보 조회
        UserEntity user = userRepository.findById(updatedComment.getUserId().intValue());
        if (user == null) {
            throw new RuntimeException("사용자 정보를 찾을 수 없습니다.");
        }

        // 사용자 정보를 updatedComment에 추가
        updatedComment.setUserName(user.getName());
        updatedComment.setPositionId(convertToInteger(user.getPosition()));
        updatedComment.setDepartmentId(convertToInteger(user.getDepartment()));
        // 프로필 이미지 URL 설정
        updatedComment.setProfilePhotoUrl(convertToFullUrl(user.getProfilePhotoUrl()));

        System.out.println("Comment updated: " + updatedComment);
        System.out.println("=== 댓글 수정 중간 완료 ===");

        return updatedComment;
    }

    @Transactional
    public void deleteComment(Long commentId) {
        System.out.println("=== 댓글 삭제 중간 진행 ===");
        System.out.println("CommentId: " + commentId);

        CommentEntity existingComment = commentRepository.findById(commentId);
        if (existingComment == null) {
            throw new CommentNotFoundException("댓글을 찾을 수 없습니다.");
        }

        commentRepository.delete(commentId);

        System.out.println("Comment deleted: " + commentId);
        System.out.println("=== 댓글 삭제 중간 완료 ===");
    }

    // String을 Integer로 변환하는 헬퍼 메서드
    private Integer convertToInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null; // 변환 실패시 null 반환
        }
    }

    /**
     * 상대 경로를 완전한 URL로 변환
     */
    private String convertToFullUrl(String photoUrl) {
        if (photoUrl == null || photoUrl.isEmpty()) {
            return null;
        }
        
        // 이미 완전한 URL인 경우 그대로 반환
        if (photoUrl.startsWith("http://") || photoUrl.startsWith("https://")) {
            return photoUrl;
        }
        
        // 상대 경로인 경우 완전한 URL로 변환
        if (photoUrl.startsWith("/uploads/")) {
            return baseUrl + photoUrl;
        }
        
        // 다른 형태의 경로인 경우 기본 도메인 추가
        return baseUrl + "/uploads/" + photoUrl;
    }
}

