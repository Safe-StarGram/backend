package com.github.repository;

import com.github.entity.CommentEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CommentJdbcRepository {

    private final JdbcTemplate jdbc;

    public CommentEntity insert(CommentEntity comment) {
        try {
            System.out.println("=== 댓글 저장 진행 ===");
            createTableIfNotExists();

            final String sql = """
                INSERT INTO comment 
                (post_id, user_id, message, created_at) 
                VALUES (?, ?, ?, ?)
                """;

            KeyHolder keyHolder = new GeneratedKeyHolder();

            jdbc.update(con -> {
                PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setLong(1, comment.getPostId());
                ps.setLong(2, comment.getUserId());
                ps.setString(3, comment.getMessage());
                ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now(ZoneId.of("Asia/Seoul")))); // created_at을 현재 시간으로 설정
                return ps;
            }, keyHolder);

            Long commentId = keyHolder.getKey().longValue();
            comment.setCommentId(commentId);

            System.out.println("Comment inserted successfully with ID: " + commentId);
            System.out.println("=== 댓글 저장 완료 ===");
            return comment;
        } catch (Exception ex) {
            System.out.println("Database error in CommentRepository.insert: " + ex.getMessage());
            ex.printStackTrace();
            comment.setCommentId(System.currentTimeMillis());
            return comment;
        }
    }

    public List<CommentEntity> findByPostId(Long postId) {
        System.out.println("=== 댓글 조회 진행 ===");
        System.out.println("PostId: " + postId);

        final String sql = """
            SELECT comment_id, post_id, user_id, message, created_at, updated_at 
            FROM comment 
            WHERE post_id = ? 
            ORDER BY created_at ASC
            """;

        try {
            List<CommentEntity> result = jdbc.query(sql, (rs, rowNum) -> {
                return CommentEntity.builder()
                        .commentId(rs.getLong("comment_id"))
                        .postId(rs.getLong("post_id"))
                        .userId(rs.getLong("user_id"))
                        .message(rs.getString("message"))
                        .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null)
                        .updatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
                        .build();
            }, postId);

            System.out.println("Query result size: " + result.size());
            System.out.println("=== 댓글 조회 완료 ===");
            return result;
        } catch (Exception e) {
            System.out.println("Error in findByPostId: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    public CommentEntity findById(Long commentId) {
        final String sql = """
            SELECT comment_id, post_id, user_id, message, created_at, updated_at 
            FROM comment 
            WHERE comment_id = ?
            """;

        try {
            return jdbc.queryForObject(sql, (rs, rowNum) -> {
                return CommentEntity.builder()
                        .commentId(rs.getLong("comment_id"))
                        .postId(rs.getLong("post_id"))
                        .userId(rs.getLong("user_id"))
                        .message(rs.getString("message"))
                        .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null)
                        .updatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
                        .build();
            }, commentId);
        } catch (Exception e) {
            System.out.println("Error in findById: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public void update(Long commentId, String content) {
        final String sql = "UPDATE comment SET message = ?, updated_at = ? WHERE comment_id = ?";

        try {
            // 로컬 현재 시간을 Timestamp로 설정
            Timestamp currentTime = Timestamp.valueOf(LocalDateTime.now(ZoneId.of("Asia/Seoul")));
            jdbc.update(sql, content, currentTime, commentId);
            System.out.println("Comment updated: " + commentId);
        } catch (Exception e) {
            System.out.println("Error updating comment: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void delete(Long commentId) {
        final String sql = "DELETE FROM comment WHERE comment_id = ?";

        try {
            jdbc.update(sql, commentId);
            System.out.println("Comment deleted: " + commentId);
        } catch (Exception e) {
            System.out.println("Error deleting comment: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createTableIfNotExists() {
        try {
            // 기존 테이블의 updated_at 컬럼 수정 (ON UPDATE CURRENT_TIMESTAMP 제거)
            final String alterTableSql = """
                ALTER TABLE comment MODIFY COLUMN updated_at TIMESTAMP NULL
                """;
            
            try {
                jdbc.execute(alterTableSql);
                System.out.println("Comment table updated_at column modified successfully");
            } catch (Exception e) {
                System.out.println("Table might not exist yet, creating new table...");
            }
            
            // 테이블이 없으면 새로 생성
            final String createTableSql = """
                CREATE TABLE IF NOT EXISTS comment (
                    comment_id    INT AUTO_INCREMENT PRIMARY KEY,
                    post_id       INT NOT NULL,
                    user_id       INT NOT NULL,
                    message       TEXT NOT NULL,
                    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at    TIMESTAMP NULL
                )
                """;

            jdbc.execute(createTableSql);
            System.out.println("Comment table checked/created successfully");
            
        } catch (Exception e) {
            System.out.println("Failed to create/modify comment table: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
