package com.github.controller;

import com.github.dto.AreaCreateRequest;
import com.github.dto.AreaResponse;
import com.github.dto.AreaUpdateRequest;
import com.github.jwt.JwtTokenProvider;
import com.github.service.AreaService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sites")
public class SiteController {

  private final AreaService service;
  private final JwtTokenProvider jwtTokenProvider;

  // 관리구역 일람 - 모든 관리구역 목록 조회
  @GetMapping
  public ResponseEntity<List<AreaResponse>> getAllArea() {
    List<AreaResponse> areas = service.getAllArea();
    return ResponseEntity.ok(areas);
  }

  // 생성: name(텍스트) + image(파일 1장)
  @PostMapping(consumes = {"multipart/form-data"})
  public ResponseEntity<Long> create(
      @ModelAttribute AreaCreateRequest data,
      @RequestPart(value = "image", required = false) MultipartFile image) {
    Long id = service.createArea(data, image);
    return ResponseEntity.ok(id);
  }

  // 관리구역 단건 조회
  @GetMapping("/{areaId}")
  public ResponseEntity<AreaResponse> getDetail(@PathVariable Long areaId) {
    AreaResponse body = service.getAreaDetail(areaId);
    return ResponseEntity.ok(body);
  }

  // 관리구역 전체 수정 (이름 + 이미지)
  @PutMapping(
      path = "/{areaId}",
      consumes = {"multipart/form-data"})
  public ResponseEntity<String> updateArea(
      @PathVariable Long areaId,
      @ModelAttribute AreaUpdateRequest req,
      @RequestPart(value = "image", required = false) MultipartFile image // 이미지 (옵션)
      ) throws IOException {
    service.updateArea(areaId, req, image);
    return ResponseEntity.ok("관리구역이 수정되었습니다.");
  }

  // 관리구역 삭제 (관리자 권한 필요)
  @DeleteMapping("/{areaId}")
  public ResponseEntity<Map<String, Object>> deleteArea(
      @PathVariable Long areaId,
      HttpServletRequest request
  ) {
    try {
      // JWT 토큰에서 사용자 정보 추출
      String token = extractTokenFromRequest(request);
      String userRole = jwtTokenProvider.getRole(token);
      
      // 관리자 권한 확인
      if (!"ROLE_ADMIN".equals(userRole)) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "관리자 권한이 필요합니다.");
        return ResponseEntity.status(403).body(errorResponse);
      }
      
      // 삭제 전 참조 데이터 정보 조회
      var refInfo = service.getAreaReferenceInfo(areaId);
      
      // Area 삭제 (참조 데이터도 함께 삭제)
      service.deleteArea(areaId);
      
      Map<String, Object> response = new HashMap<>();
      response.put("message", "관리구역이 삭제되었습니다.");
      response.put("deletedData", Map.of(
        "posts", refInfo.getPostCount(),
        "subAreas", refInfo.getSubAreaCount()
      ));
      
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("error", e.getMessage());
      return ResponseEntity.badRequest().body(errorResponse);
    }
  }

  // 관리구역 삭제 전 참조 데이터 확인
  @GetMapping("/{areaId}/references")
  public ResponseEntity<Map<String, Object>> getAreaReferences(@PathVariable Long areaId) {
    try {
      var refInfo = service.getAreaReferenceInfo(areaId);
      
      Map<String, Object> response = new HashMap<>();
      response.put("areaId", areaId);
      response.put("references", Map.of(
        "posts", refInfo.getPostCount(),
        "subAreas", refInfo.getSubAreaCount(),
        "hasReferences", refInfo.hasReferences()
      ));
      
      return ResponseEntity.ok(response);
    } catch (Exception e) {
      Map<String, Object> errorResponse = new HashMap<>();
      errorResponse.put("error", e.getMessage());
      return ResponseEntity.badRequest().body(errorResponse);
    }
  }

  /**
   * JWT 토큰을 요청에서 추출합니다
   */
  private String extractTokenFromRequest(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
      String token = bearerToken.substring(7);
      // 토큰 유효성 검증
      if (!jwtTokenProvider.validate(token)) {
        throw new RuntimeException("유효하지 않은 토큰입니다.");
      }
      return token;
    }
    throw new RuntimeException("인증 토큰이 필요합니다.");
  }
}
