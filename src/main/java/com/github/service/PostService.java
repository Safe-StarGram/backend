package com.github.service;

import com.github.dto.PostCreateRequest;
import com.github.dto.AdminPostUpdateRequest;
import com.github.dto.PostResponse;
import com.github.entity.PostEntity;
import com.github.exception.PostNotFoundException;
import com.github.jwt.JwtTokenProvider;
import com.github.repository.PostJdbcRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.sql.Timestamp;
import com.github.constants.ErrorMessages;
import com.github.constants.PostConstants;
import com.github.constants.FileConstants;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostJdbcRepository postRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${file.upload-dir:./uploads}")
    private String fileUploadDirectory;

    @Value("${api.base-url}")
    private String apiBaseUrl;


        @Transactional
        public PostEntity create(PostCreateRequest req, MultipartFile image) {
            try {
                System.out.println("=== PostService Create Debug ===");
                System.out.println("Request: " + req);
                System.out.println("Reporter Risk Score: " + req.getReporterRisk());
                System.out.println("Image: " + (image != null ? image.getOriginalFilename() : "null"));
                
                LocalDateTime now = LocalDateTime.now();

                PostEntity e = PostEntity.builder()
                        .subAreaId(req.getSubAreaId())
                        .areaId(req.getAreaId())           // areaId 추가
                        .reporterId(req.getUserId())       // 나중에 리팩토링 필요할수도
                        .title(req.getTitle())
                        .content(req.getContent())
                        .reporterRisk(req.getReporterRisk())
                        .isChecked(0)      // 0: 미확인 (초기값)
                        .isActionTaked(0)  // 0: 미조치 (초기값)
                        .createdAt(now)   // 생성 시 현재 시간 설정
                        .updatedAt(null)  // 생성 시 null로 설정
                        .checkedAt(now)   // checked_at 필드 설정
                        .build();

                System.out.println("PostEntity built: " + e);
                
                System.out.println("=== PostRepository.insert 호출 전 ===");
                PostEntity savedPost = postRepository.insert(e);
                System.out.println("=== PostRepository.insert 호출 후 ===");
                System.out.println("PostEntity saved: " + savedPost);
                System.out.println("Saved post ID: " + savedPost.getPostId());
                
                // 이미지가 있으면 저장하고 post_photo_url 업데이트
                if (image != null && !image.isEmpty()) {
                    String imageUrl = savePostImage(req.getUserId(), image);
                    if (imageUrl != null) {
                        // post_photos 테이블에 저장
                        postRepository.insertPostPhoto(savedPost.getPostId(), imageUrl, 0);
                        
                        // post 테이블의 post_photo_url 필드도 업데이트
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("post_photo_url", imageUrl);
                        System.out.println("Updating post_photo_url to: " + imageUrl);
                        postRepository.update(savedPost.getPostId(), updates);
                        System.out.println("Post updated successfully");
                    }
                }
                
                // 업데이트된 최신 데이터를 다시 조회 (area_id 포함)
                PostEntity finalPost = postRepository.findById(savedPost.getPostId());
                if (finalPost != null) {
                    System.out.println("Final post postPhotoUrl: " + finalPost.getPostPhotoUrl());
                    System.out.println("Final post areaId: " + finalPost.getAreaId());
                }
                
                // 알림 생성 로직 제거 - 실제로는 위험보고 목록 조회만 필요
                // Post 생성 완료 후 별도 알림 생성 불필요
                
                return finalPost != null ? finalPost : savedPost; // 최신 데이터 반환 (area_id 포함)
            } catch (Exception ex) {
                System.out.println("Error in PostService.create: " + ex.getMessage());
                ex.printStackTrace();
                throw ex;
            }
    }

    @Transactional
    public List<PostEntity> getAllPosts(int page, int size) {
        List<PostEntity> posts = postRepository.findAll(page, size);
        // 각 게시글의 이미지 URL을 완전한 URL로 변환
        posts.forEach(post -> {
            if (post.getPostPhotoUrl() != null) {
                post.setPostPhotoUrl(convertToFullUrl(post.getPostPhotoUrl()));
            }
        });
        return posts;
    }

    @Transactional
    public PostEntity getPostById(Long postId) {
        PostEntity post = postRepository.findById(postId);
        if (post == null) {
            throw new PostNotFoundException(ErrorMessages.POST_NOT_FOUND);
        }
        // 이미지 URL을 완전한 URL로 변환
        if (post.getPostPhotoUrl() != null) {
            post.setPostPhotoUrl(convertToFullUrl(post.getPostPhotoUrl()));
        }
        return post;
    }

    @Transactional
    public List<PostEntity> getPostsBySubArea(Long subAreaId, int page, int size) {
        System.out.println("=== 지역별 게시글 조회 진행 ===");
        System.out.println("SubAreaId: " + subAreaId + ", Page: " + page + ", Size: " + size);
        
        List<PostEntity> posts = postRepository.findBySubArea(subAreaId, page, size);
        
        // 각 게시글의 이미지 URL을 완전한 URL로 변환
        posts.forEach(post -> {
            if (post.getPostPhotoUrl() != null) {
                post.setPostPhotoUrl(convertToFullUrl(post.getPostPhotoUrl()));
            }
        });
        
        System.out.println("Found " + posts.size() + " posts for subAreaId: " + subAreaId);
        System.out.println("=== 지역별 게시글 조회 완료 ===");
        return posts;
    }

    public int countPostsBySubArea(Long subAreaId) {
        return postRepository.countBySubArea(subAreaId);
    }

    public int countActionTakenPostsBySubArea(Long subAreaId) {
        return postRepository.countActionTakenBySubArea(subAreaId);
    }

    @Transactional
    public PostEntity updatePost(Long postId, Map<String, Object> updates, Long userId) {
        log.info("=== updatePost 시작 ===");
        log.info("postId: {}, userId: {}, updates: {}", postId, userId, updates);
        
        PostEntity existingPost = postRepository.findById(postId);
        if (existingPost == null) {
            throw new PostNotFoundException(ErrorMessages.POST_NOT_FOUND);
        }
        
        // 권한 확인 (작성자 또는 관리자 수정 가능)
        // 조치 상태 업데이트는 관리자도 가능하도록 허용
        boolean isReporter = existingPost.getReporterId().equals(userId);
        boolean isActionTaker = existingPost.getActionTakerId() != null && existingPost.getActionTakerId().equals(userId);
        
        if (!isReporter && !isActionTaker) {
            // 작성자도 관리자도 아닌 경우, 조치 상태 관련 필드만 업데이트 허용
            boolean hasOnlyActionFields = updates.keySet().stream()
                .allMatch(key -> key.equals("is_checked") || 
                               key.equals("is_action_taken") || 
                               key.equals("manager_risk") || 
                               key.equals("action_taker_id") ||
                               key.equals("is_checked_id"));
            
            if (!hasOnlyActionFields) {
                throw new RuntimeException(ErrorMessages.INSUFFICIENT_PERMISSION);
            }
        }
        
        // camelCase를 snake_case로 변환하고 0/1 값을 1/0 문자열로 변환
        Map<String, Object> convertedUpdates = new HashMap<>();
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // camelCase를 snake_case로 변환
            String dbKey = convertCamelToSnakeCase(key);
            
            if ("is_checked".equals(dbKey) || "is_action_taken".equals(dbKey)) {
                // 0/1 값을 1/0 문자열로 변환 (데이터베이스 스키마에 맞춤)
                if (value instanceof Integer) {
                    convertedUpdates.put(dbKey, ((Integer) value) == 1 ? "1" : "0");
                } else if (value instanceof String) {
                    convertedUpdates.put(dbKey, "1".equals(value) ? "1" : "0");
                } else {
                    convertedUpdates.put(dbKey, value);
                }
            } else {
                convertedUpdates.put(dbKey, value);
            }
        }
        
        // is_checked 처리 (0일 때는 null, 1일 때는 한 번만 시간 설정)
        if (updates.containsKey("is_checked")) {
            Object checkedValue = updates.get("is_checked");
            boolean isChecked = false;
            
            log.info("=== is_checked 처리 시작 ===");
            log.info("checkedValue: {}, type: {}", checkedValue, checkedValue != null ? checkedValue.getClass().getSimpleName() : "null");
            
            if (checkedValue instanceof Integer) {
                isChecked = ((Integer) checkedValue) == 1;
            } else if (checkedValue instanceof String) {
                isChecked = "1".equals(checkedValue);
            }
            
            log.info("isChecked: {}", isChecked);
            
            if (isChecked) {
                // 1일 때는 확인한 사람 ID 설정
                convertedUpdates.put("checker_id", userId);
                // 확인할 때마다 최신 시간으로 업데이트
                Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                convertedUpdates.put("checked_at", now);
                log.info("checked_at 설정: {}", now);
            } else {
                // 0일 때는 null로 설정
                convertedUpdates.put("checker_id", null);
                convertedUpdates.put("checked_at", null);
                log.info("checked_at null로 설정");
            }
            log.info("=== is_checked 처리 완료 ===");
        }
        
        // is_action_taken 처리 (0일 때는 null, 1일 때는 한 번만 시간 설정)
        if (updates.containsKey("is_action_taken")) {
            Object actionTakenValue = updates.get("is_action_taken");
            boolean isActionTaken = false;
            
            if (actionTakenValue instanceof Integer) {
                isActionTaken = ((Integer) actionTakenValue) == 1;
            } else if (actionTakenValue instanceof String) {
                isActionTaken = "1".equals(actionTakenValue);
            }
            
            if (isActionTaken) {
                // 1일 때는 조치한 사람 ID 설정
                convertedUpdates.put("action_taker_id", userId);
                // 조치할 때마다 최신 시간으로 업데이트
                convertedUpdates.put("action_taken_at", Timestamp.valueOf(LocalDateTime.now()));
            } else {
                // 0일 때는 null로 설정
                convertedUpdates.put("action_taker_id", null);
                convertedUpdates.put("action_taken_at", null);
            }
        }
        
        // updated_at은 데이터베이스의 ON UPDATE CURRENT_TIMESTAMP가 자동으로 처리
        
        log.info("=== 최종 convertedUpdates ===");
        log.info("convertedUpdates: {}", convertedUpdates);
        
        PostEntity result = postRepository.update(postId, convertedUpdates);
        log.info("=== updatePost 완료 ===");
        return result;
    }

 

    @Transactional
    public void deletePost(Long postId, Long userId) {
        PostEntity existingPost = postRepository.findById(postId);
        if (existingPost == null) {
            throw new PostNotFoundException(ErrorMessages.POST_NOT_FOUND);
        }
        
        // 권한 확인 (작성자만 삭제 가능)
        boolean isReporter = existingPost.getReporterId().equals(userId);
        
        if (!isReporter) {
            throw new RuntimeException(ErrorMessages.INSUFFICIENT_PERMISSION);
        }
        
        postRepository.delete(postId);
    }

    @Transactional
    public void deletePostByAdmin(Long postId) {
        PostEntity existingPost = postRepository.findById(postId);
        if (existingPost == null) {
            throw new PostNotFoundException(ErrorMessages.POST_NOT_FOUND);
        }
        
        // 관리자는 권한 확인 없이 삭제 가능
        postRepository.delete(postId);
    }

    private String savePostImage(Long userId, MultipartFile file) {
        try {
            // 업로드 디렉토리 생성
            Path uploadPath = Paths.get(fileUploadDirectory);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 파일명 생성 (고유한 이름)
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = "post_" + userId + "_" + UUID.randomUUID().toString() + extension;

            // 파일 저장
            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 상대 경로로 반환 (프론트엔드에서 동적 URL 변환)
            return "/uploads/" + filename;
        } catch (IOException e) {
            System.out.println("Failed to save post image: " + e.getMessage());
            return null;
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
            return apiBaseUrl + photoUrl;
        }
        
        // 다른 형태의 경로인 경우 기본 도메인 추가
        return apiBaseUrl + "/uploads/" + photoUrl;
    }

    /**
     * camelCase를 snake_case로 변환
     */
    private String convertCamelToSnakeCase(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }
        
        // 특정 필드 매핑
        switch (camelCase) {
            case "areaId":
                return "area_id";
            case "subAreaId":
                return "sub_area_id";
            case "reporterRisk":
                return "reporter_risk";
            case "managerRisk":
                return "manager_risk";
            case "actionTakerId":
                return "action_taker_id";
            case "isChecked":
                return "is_checked";
            case "isCheckedId":
                return "is_checked_id";
            case "isActionTaken":
                return "is_action_taken";
            case "isCheckedAt":
                return "is_checked_at";
            case "isActionTakenAt":
                return "is_action_taken_at";
            case "postPhotoUrl":
                return "post_photo_url";
            case "reporterId":
                return "reporter_id";
            case "createdAt":
                return "created_at";
            case "updatedAt":
                return "updated_at";
            default:
                // 이미 snake_case이거나 다른 형태면 그대로 반환
                return camelCase;
        }
    }

    /**
     * 관리자용 게시글 수정 (관리자 권한 필요)
     */
    @Transactional
    public PostEntity updatePostByAdmin(Long postId, AdminPostUpdateRequest request, String currentUserToken) {
        // 현재 사용자 권한 확인
        String currentUserRole = jwtTokenProvider.getRole(currentUserToken);
        if (!"ROLE_ADMIN".equals(currentUserRole)) {
            throw new RuntimeException(ErrorMessages.ADMIN_PERMISSION_REQUIRED);
        }
        
        // 게시글 존재 여부 확인
        PostEntity existingPost = postRepository.findById(postId);
        if (existingPost == null) {
            throw new PostNotFoundException(ErrorMessages.POST_NOT_FOUND);
        }
        
        // 현재 사용자 ID 가져오기
        Long currentUserId = jwtTokenProvider.getUserId(currentUserToken);
        
        // 업데이트할 데이터 구성
        Map<String, Object> updates = new HashMap<>();
        
        // isChecked 처리
        if (request.getIsChecked() != null) {
            updates.put("is_checked", request.getIsChecked());
            
            if (request.getIsChecked() == 1) {
                // 확인 완료 시 - 항상 현재 시간으로 업데이트
                updates.put("checker_id", request.getCheckerId() != null ? request.getCheckerId() : currentUserId);
                // 확인할 때마다 최신 시간으로 업데이트 (클라이언트에서 보낸 시간 무시)
                updates.put("checked_at", Timestamp.valueOf(LocalDateTime.now()));
            } else {
                // 미확인 시
                updates.put("checker_id", null);
                updates.put("checked_at", null);
            }
        }
        
        // isActionTaked 처리
        if (request.getIsActionTaked() != null) {
            updates.put("is_action_taken", request.getIsActionTaked());
            
            if (request.getIsActionTaked() == 1) {
                // 조치 완료 시 - 항상 현재 시간으로 업데이트
                updates.put("action_taker_id", request.getActionTakerId() != null ? request.getActionTakerId() : currentUserId);
                // 조치할 때마다 최신 시간으로 업데이트 (클라이언트에서 보낸 시간 무시)
                updates.put("action_taken_at", Timestamp.valueOf(LocalDateTime.now()));
            } else {
                // 미조치 시
                updates.put("action_taker_id", null);
                updates.put("action_taken_at", null);
            }
        }
        
        // managerRisk 처리
        if (request.getManagerRisk() != null) {
            updates.put("manager_risk", request.getManagerRisk());
        }
        
        // 게시글 업데이트
        PostEntity updatedPost = postRepository.update(postId, updates);
        
        System.out.println("관리자용 게시글 수정 완료: postId=" + postId);
        return updatedPost;
    }

    /**
     * PostEntity를 PostResponse로 변환
     */
    public PostResponse convertToPostResponse(PostEntity post) {
        return PostResponse.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .postPhotoUrl(post.getPostPhotoUrl())
                .content(post.getContent())
                .areaId(post.getAreaId())
                .subAreaId(post.getSubAreaId())
                .reporterId(post.getReporterId())
                .isChecked(post.getIsChecked())
                .checkerId(post.getCheckerId())
                .checkedAt(post.getCheckedAt())
                .isActionTaked(post.getIsActionTaked())
                .actionTakerId(post.getActionTakerId())
                .actionTakenAt(post.getActionTakenAt())
                .reporterRiskScore(post.getReporterRiskScore())
                .reporterRiskDescription(post.getReporterRiskDescription())
                .reporterRisk(post.getReporterRisk())
                .managerRisk(post.getManagerRisk())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }


}
