package com.github.service;

import com.github.dto.PostCreateRequest;
import com.github.entity.PostEntity;
import com.github.exception.PostNotFoundException;
import com.github.repository.PostJdbcRepository;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostJdbcRepository postRepository;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;


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
                        .isActionTaken(0)  // 0: 미조치 (초기값)
                        .createdAt(now)   // 생성 시 현재 시간 설정
                        .updatedAt(null)  // 생성 시 null로 설정
                        .isCheckedAt(now)  // is_checked_at 필드 설정
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
            throw new PostNotFoundException("게시물을 찾을 수 없습니다.");
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

    public int countBySubArea(Long subAreaId) {
        return postRepository.countBySubArea(subAreaId);
    }

    public int countActionTakenBySubArea(Long subAreaId) {
        return postRepository.countActionTakenBySubArea(subAreaId);
    }

    @Transactional
    public PostEntity updatePost(Long postId, Map<String, Object> updates, Long userId) {
        PostEntity existingPost = postRepository.findById(postId);
        if (existingPost == null) {
            throw new PostNotFoundException("게시물을 찾을 수 없습니다.");
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
                throw new RuntimeException("게시물을 수정할 권한이 없습니다.");
            }
        }
        
        // camelCase를 snake_case로 변환하고 0/1 값을 Y/N으로 변환
        Map<String, Object> convertedUpdates = new HashMap<>();
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // camelCase를 snake_case로 변환
            String dbKey = convertCamelToSnakeCase(key);
            
            if ("is_checked".equals(dbKey) || "is_action_taken".equals(dbKey)) {
                // 0/1 값을 Y/N으로 변환
                if (value instanceof Integer) {
                    convertedUpdates.put(dbKey, ((Integer) value) == 1 ? "Y" : "N");
                } else if (value instanceof String) {
                    convertedUpdates.put(dbKey, "1".equals(value) ? "Y" : "N");
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
            
            if (checkedValue instanceof Integer) {
                isChecked = ((Integer) checkedValue) == 1;
            } else if (checkedValue instanceof String) {
                isChecked = "1".equals(checkedValue);
            }
            
            if (isChecked) {
                // 1일 때는 확인한 사람 ID 설정
                convertedUpdates.put("is_checked_id", userId);
                // 이미 시간이 설정되어 있으면 시간 업데이트 안함
                if (existingPost.getIsCheckedAt() == null) {
                    convertedUpdates.put("is_checked_at", LocalDateTime.now());
                }
            } else {
                // 0일 때는 null로 설정
                convertedUpdates.put("is_checked_id", null);
                convertedUpdates.put("is_checked_at", null);
            }
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
                // 이미 시간이 설정되어 있으면 시간 업데이트 안함
                if (existingPost.getIsActionTakenAt() == null) {
                    convertedUpdates.put("is_action_taken_at", LocalDateTime.now());
                }
            } else {
                // 0일 때는 null로 설정
                convertedUpdates.put("action_taker_id", null);
                convertedUpdates.put("is_action_taken_at", null);
            }
        }
        
        // updated_at은 데이터베이스의 ON UPDATE CURRENT_TIMESTAMP가 자동으로 처리
        
        return postRepository.update(postId, convertedUpdates);
    }

 

    @Transactional
    public void deletePost(Long postId, Long userId) {
        PostEntity existingPost = postRepository.findById(postId);
        if (existingPost == null) {
            throw new PostNotFoundException("게시물을 찾을 수 없습니다.");
        }
        
        // 권한 확인 (작성자만 삭제 가능)
        if (!existingPost.getReporterId().equals(userId)) {
            throw new RuntimeException("게시물을 삭제할 권한이 없습니다.");
        }
        
        postRepository.delete(postId);
    }

    private String savePostImage(Long userId, MultipartFile file) {
        try {
            // 업로드 디렉토리 생성
            Path uploadPath = Paths.get(uploadDir);
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
            return "https://chan23.duckdns.org/safe_api" + photoUrl;
        }
        
        // 다른 형태의 경로인 경우 기본 도메인 추가
        return "https://chan23.duckdns.org/safe_api/uploads/" + photoUrl;
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


}
