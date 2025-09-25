package com.github.token;

import com.github.jwt.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@RequiredArgsConstructor
public class RefreshTokenStore {

    private final JwtProperties props;
    
    // 메모리 기반 토큰 저장소
    private final Map<String, LocalDateTime> tokenStore = new ConcurrentHashMap<>();

    private String key(long userId, String jti) {
        return "refresh:" + userId + ":" + jti;
    }

    public void save(long userId, String jti) {
        String key = key(userId, jti);
        // Duration을 LocalDateTime으로 변환할 때 정확한 계산 필요
        LocalDateTime expiryTime = LocalDateTime.now().plusSeconds(props.getRefreshTtl().toSeconds());
        tokenStore.put(key, expiryTime);
        
        // 만료된 토큰들 정리
        cleanupExpiredTokens();
    }

    public boolean exists(long userId, String jti) {
        String key = key(userId, jti);
        LocalDateTime expiryTime = tokenStore.get(key);
        
        System.out.println("RefreshTokenStore.exists() - key: " + key + ", expiryTime: " + expiryTime);
        
        if (expiryTime == null) {
            System.out.println("RefreshTokenStore.exists() - 토큰이 저장소에 없음");
            return false;
        }
        
        // 만료된 토큰인지 확인
        if (LocalDateTime.now().isAfter(expiryTime)) {
            System.out.println("RefreshTokenStore.exists() - 토큰이 만료됨, 삭제");
            tokenStore.remove(key);
            return false;
        }
        
        System.out.println("RefreshTokenStore.exists() - 토큰이 유효함");
        return true;
    }

    public void delete(long userId, String jti) {
        String key = key(userId, jti);
        tokenStore.remove(key);
    }

    public boolean isValid(long userId, String jti) {
        return exists(userId, jti);
    }
    
    private void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        tokenStore.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
    }
}
