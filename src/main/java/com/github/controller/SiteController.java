package com.github.controller;

import com.github.dto.AreaCreateRequest;
import com.github.dto.AreaResponse;
import com.github.dto.AreaUpdateRequest;
import com.github.service.AreaService;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sites")
public class SiteController {

  private final AreaService service;

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
}
