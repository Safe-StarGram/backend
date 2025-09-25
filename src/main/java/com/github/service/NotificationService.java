package com.github.service;

import com.github.entity.PostEntity;
import com.github.repository.PostJdbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final PostJdbcRepository postRepository;

    // 위험보고 목록 조회 (Post 목록 직접 반환)
    @Transactional(readOnly = true)
    public List<PostEntity> getRecentRiskReports(int page, int size) {
        System.out.println("=== NotificationService.getRecentRiskReports() 시작 ===");
        System.out.println("Page: " + page + ", Size: " + size);
        System.out.println("PostRepository: " + postRepository);

        try {
            List<PostEntity> posts = postRepository.findAll(page, size);
            System.out.println("Found " + posts.size() + " recent risk reports");
            System.out.println("=== NotificationService.getRecentRiskReports() 완료 ===");
            return posts; // PostEntity 직접 반환
        } catch (Exception e) {
            System.out.println("Error in NotificationService: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }
}

