package com.example.imagehostingservice.user.repository;

import com.example.imagehostingservice.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<User> userRowMapper = (rs, rowNum) ->
            new User(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getString("password_hash"),
                    rs.getTimestamp("created_at").toLocalDateTime()
            );

    public User save(String name, String email, String passwordHash) {
        String sql = """
                INSERT INTO users (name, email, password_hash)
                VALUES (?, ?, ?)
                RETURNING id, name, email, password_hash, created_at
                """;

        return jdbcTemplate.queryForObject(
                sql,
                userRowMapper,
                name,
                email,
                passwordHash
        );
    }

    public Optional<User> findByEmail(String email) {
        String sql = """
                SELECT id, name, email, password_hash, created_at
                FROM users
                WHERE email = ?
                """;

        return jdbcTemplate.query(sql, userRowMapper, email)
                .stream()
                .findFirst();
    }

    public boolean existsByEmail(String email) {
        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM users
                    WHERE email = ?
                )
                """;

        Boolean exists = jdbcTemplate.queryForObject(
                sql,
                Boolean.class,
                email
        );

        return Boolean.TRUE.equals(exists);
    }

    public Optional<User> findById(Long id) {
        String sql = """
                SELECT id, name, email, password_hash, created_at
                FROM users
                WHERE id = ?
                """;

        return jdbcTemplate.query(sql, userRowMapper, id)
                .stream()
                .findFirst();
    }
}