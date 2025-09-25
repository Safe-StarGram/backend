package com.github.service;

import com.github.dto.AreaResponse;
import com.github.dto.AreaUpdateRequest;
import com.github.dto.SubAreaDto;
import com.github.entity.AreaEntity;
import com.github.repository.AreaJdbcRepository;
import com.github.repository.PostJdbcRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AreaService {

    private final AreaJdbcRepository repo;
    private final PostJdbcRepository postJdbcRepository;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;


    //소구역 포함 저장
    @Transactional
    public Long createArea(com.github.dto.AreaCreateRequest req, MultipartFile image) {
        String imageUrl = null;

        if (req == null || req.getAreaName() == null || req.getAreaName().trim().isEmpty())
            throw new IllegalArgumentException("areaName은 필수");
        var names = req.getSubAreaNames();
        if (names == null || names.isEmpty())
            throw new IllegalArgumentException("subAreaNames는 최소 1개 이상");
        names.removeIf(n -> n == null || n.trim().isEmpty());

        if (image != null && !image.isEmpty()) {
            imageUrl = saveAndMakeUrl(image);
        }

        Long areaId = repo.insertArea(req.getAreaName().trim(), imageUrl);
        repo.insertSubAreas(areaId, names);
        return areaId;
    }


    @Transactional
    public void updateAreaImage(Long areaId, MultipartFile image) {
        if (image == null || image.isEmpty()) return;
        String url = saveAndMakeUrl(image);
        repo.updateImageUrl(areaId, url);
    }

    private String saveAndMakeUrl(MultipartFile file) {
        try {
            Path root = Paths.get(uploadDir);
            if (!Files.exists(root)) Files.createDirectories(root);

            String original = file.getOriginalFilename();
            String ext = (original != null && original.lastIndexOf('.') != -1)
                    ? original.substring(original.lastIndexOf('.')) : "";
            String stored = "area_" + UUID.randomUUID() + ext;

            Path target = root.resolve(stored);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/" + stored;

        } catch (IOException e) {
            throw new RuntimeException("이미지 저장 실패: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public AreaResponse getAreaDetail(Long areaId) {
        // 관리구역 기본 정보 조회
        var area = repo.findArea(areaId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 관리구역입니다."));

        // 소구역 목록 조회
        var subs = repo.findSubArea(areaId);

        // DTO 변환
        List<SubAreaDto> subDtos = subs.stream()
                .map(s -> new SubAreaDto(s.getSubAreaId(), s.getName()))
                .toList();

        return AreaResponse.builder()
                .id(area.getAreaId())
                .areaName(area.getName())
                .imageUrl(area.getImageUrl())
                .subAreas(subDtos)
                .build();
    }

    // 관리구역 일람 - 모든 관리구역 목록 조회 (소구역 정보 포함)
    @Transactional(readOnly = true)
    public List<AreaResponse> getAllArea() {
        List<AreaEntity> areas = repo.findAllArea();
        
        return areas.stream()
                .map(area -> {
                    // 각 관리구역의 소구역 목록 조회
                    var subs = repo.findSubArea(area.getAreaId());
                    List<SubAreaDto> subDtos = subs.stream()
                            .map(s -> new SubAreaDto(s.getSubAreaId(), s.getName()))
                            .toList();
                    
                    return AreaResponse.builder()
                            .id(area.getAreaId())
                            .areaName(area.getName())
                            .imageUrl(convertToFullUrl(area.getImageUrl()))
                            .subAreas(subDtos) // 소구역 정보 포함
                            .build();
                })
                .toList();
    }

    // 이미지 URL을 완전한 URL로 변환
    private String convertToFullUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }
        
        // 이미 완전한 URL인 경우 그대로 반환
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            return imageUrl;
        }
        
        // 상대 경로인 경우 완전한 URL로 변환
        if (imageUrl.startsWith("/uploads/")) {
            return "https://chan23.duckdns.org/safe_api" + imageUrl;
        }
        
        // 다른 형태의 경로인 경우 기본 도메인 추가
        return "https://chan23.duckdns.org/safe_api/uploads/" + imageUrl;
    }

    @Transactional
    public void updateArea(Long areaId, AreaUpdateRequest req, MultipartFile image) throws IOException {
        // 1. 관리구역 이름 변경
        if (req.getAreaName() != null && !req.getAreaName().trim().isEmpty()) {
            repo.updateAreaName(areaId, req.getAreaName().trim());
        }

        // 2. 이미지가 있으면 새로 저장 (기존 이미지 교체)
        if (image != null && !image.isEmpty()) {
            String imageUrl = saveAndMakeUrl(image);
            repo.updateAreaImage(areaId, imageUrl);
        }

        // 3. 소구역 통째로 교체
        repo.deleteSubAreas(areaId);

        if (req.getSubAreaNames() != null && !req.getSubAreaNames().isEmpty()) {
            List<String> names = req.getSubAreaNames().stream()
                    .filter(n -> n != null && !n.trim().isEmpty())
                    .toList();

            if (!names.isEmpty()) {
                repo.insertSubAreas(areaId, names);
            }
        }
    }

    // 구역별 자세히 보기 (상세 정보 + 통계)
    @Transactional(readOnly = true)
    public Map<String, Object> getAreaDetailWithStats(Long areaId, LocalDate from, LocalDate to) {
        // 기본 기간 설정 (최근 5개월)
        if (from == null) {
            from = LocalDate.now().minusMonths(5);
        }
        if (to == null) {
            to = LocalDate.now();
        }

        // 1. 관리구역 기본 정보 조회
        AreaResponse areaDetail = getAreaDetail(areaId);

        // 2. 구역별 통계 조회
        Map<String, Object> stats = new HashMap<>();
        
        // 해당 구역의 보고건수
        List<Map<String, Object>> reportCounts = postJdbcRepository.countReportsByBlock(from, to);
        Map<String, Object> areaReportCount = reportCounts.stream()
                .filter(stat -> areaId.equals(stat.get("blockId")))
                .findFirst()
                .orElse(Map.of("blockId", areaId, "blockName", areaDetail.getAreaName(), "reportCount", 0));
        stats.put("reportCount", areaReportCount.get("reportCount"));

        // 해당 구역의 조치건수
        List<Map<String, Object>> actionCounts = postJdbcRepository.countActionsByBlock(from, to);
        Map<String, Object> areaActionCount = actionCounts.stream()
                .filter(stat -> areaId.equals(stat.get("blockId")))
                .findFirst()
                .orElse(Map.of("blockId", areaId, "blockName", areaDetail.getAreaName(), "actionCount", 0));
        stats.put("actionCount", areaActionCount.get("actionCount"));

        // 해당 구역의 고위험성 조치건수
        List<Map<String, Object>> highRiskActions = postJdbcRepository.countHighRiskActions(from, to);
        Map<String, Object> areaHighRiskCount = highRiskActions.stream()
                .filter(stat -> areaId.equals(stat.get("blockId")))
                .findFirst()
                .orElse(Map.of("blockId", areaId, "blockName", areaDetail.getAreaName(), "actionCount", 0));
        stats.put("highRiskActionCount", areaHighRiskCount.get("actionCount"));

        // 3. 결과 조합
        Map<String, Object> result = new HashMap<>();
        result.put("areaDetail", areaDetail);
        result.put("stats", stats);
        result.put("period", Map.of("from", from, "to", to));

        return result;
    }

    // 구역별 월별 통계 (5개월 단위)
    @Transactional(readOnly = true)
    public Map<String, Object> getAreaMonthlyStats(Long areaId, LocalDate from, LocalDate to) {
        // 1. 해당 구역의 월별 보고건수
        List<Map<String, Object>> monthlyReports = postJdbcRepository.countMonthlyReportsByBlock(from, to);
        List<Map<String, Object>> areaMonthlyReports = monthlyReports.stream()
                .filter(stat -> areaId.equals(stat.get("blockId")))
                .toList();

        // 2. 해당 구역의 월별 조치건수 (기존 메서드 활용)
        List<Map<String, Object>> monthlyActions = getAreaMonthlyActions(areaId, from, to);

        // 3. 해당 구역의 월별 고위험성 조치건수
        List<Map<String, Object>> monthlyHighRiskActions = getAreaMonthlyHighRiskActions(areaId, from, to);

        Map<String, Object> result = new HashMap<>();
        result.put("monthlyReports", areaMonthlyReports);
        result.put("monthlyActions", monthlyActions);
        result.put("monthlyHighRiskActions", monthlyHighRiskActions);
        result.put("period", Map.of("from", from, "to", to));

        return result;
    }

    // 구역별 월별 조치건수
    private List<Map<String, Object>> getAreaMonthlyActions(Long areaId, LocalDate from, LocalDate to) {
        List<Map<String, Object>> monthlyActions = postJdbcRepository.countMonthlyActionsByBlock(from, to);
        return monthlyActions.stream()
                .filter(stat -> areaId.equals(stat.get("blockId")))
                .toList();
    }

    // 구역별 월별 고위험성 조치건수
    private List<Map<String, Object>> getAreaMonthlyHighRiskActions(Long areaId, LocalDate from, LocalDate to) {
        List<Map<String, Object>> monthlyHighRiskActions = postJdbcRepository.countMonthlyHighRiskActionsByBlock(from, to);
        return monthlyHighRiskActions.stream()
                .filter(stat -> areaId.equals(stat.get("blockId")))
                .toList();
    }
}