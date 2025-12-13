package com.example.movie_service.repository;

import com.example.movie_service.model.GenreMap;
import com.example.movie_service.model.Movie;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class MovieRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public MovieRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Movie> MOVIE_MAPPER = (rs, rowNum) -> {
        Movie m = new Movie();
        m.setId(rs.getInt("id"));
        m.setTitle(rs.getString("title"));
        m.setOriginalTitle(rs.getString("original_title"));
        m.setOverview(rs.getString("overview"));
        m.setPopularity(rs.getObject("popularity") == null ? null : rs.getDouble("popularity"));
        m.setVoteCount(rs.getObject("vote_count") == null ? null : rs.getInt("vote_count"));
        m.setVoteAverage(rs.getObject("vote_average") == null ? null : rs.getDouble("vote_average"));
        m.setOriginalLanguage(rs.getString("original_language"));
        m.setBackdropPath(rs.getString("backdrop_path"));
        m.setPosterPath(rs.getString("poster_path"));
        m.setAdult(rs.getObject("adult") == null ? null : rs.getBoolean("adult"));
        m.setVideo(rs.getObject("video") == null ? null : rs.getBoolean("video"));
        m.setReleaseDate(rs.getDate("release_date") != null ? rs.getDate("release_date").toLocalDate() : null);

        // genre_ids comes from array_agg
        java.sql.Array sqlArr = rs.getArray("genre_ids");
        if (sqlArr != null) {
            Object arr = sqlArr.getArray();
            if (arr instanceof Integer[]) {
                m.setGenreIds(Arrays.asList((Integer[]) arr));
            } else {
                Object[] objs = (Object[]) arr;
                List<Integer> ids = new ArrayList<>();
                for (Object o : objs) {
                    ids.add(Integer.parseInt(o.toString()));
                }
                m.setGenreIds(ids);
            }
        } else {
            m.setGenreIds(Collections.emptyList());
        }

        return m;
    };


    /**
     * Fetch paginated movies with optional title search and genre filter.
     *
     * @param search   optional title substring (ILIKE)
     * @param genreId  optional numeric genre id to filter (can be null)
     * @param offset   offset for pagination
     * @param limit    page size
     * @param sortBy   column to sort by (e.g., "popularity", "vote_average", "release_date")
     * @param sortDesc whether descending
     */
    public List<Movie> findAll(
            String search,
            Integer genreId,
            int offset,
            int limit,
            String sortBy,
            boolean sortDesc
    ) {
        // validate/allowlist sortBy to avoid SQL injection
        var allowedSort = Set.of("popularity", "vote_average", "release_date", "title", "id");
        String sort = allowedSort.contains(sortBy) ? sortBy : "id";
        String direction = sortDesc ? "DESC" : "ASC";

        String baseSql = """
        SELECT m.*,
               COALESCE(array_agg(DISTINCT mg.genre_id ORDER BY mg.genre_id), ARRAY[]::integer[]) AS genre_ids
        FROM movies m
        LEFT JOIN movie_genres mg ON mg.movie_id = m.id
        WHERE (:search::text IS NULL OR (m.title ILIKE '%' || :search::text || '%' OR m.original_title ILIKE '%' || :search::text || '%'))
          AND (:genreId::int IS NULL OR EXISTS (
                SELECT 1 FROM movie_genres mg2 WHERE mg2.movie_id = m.id AND mg2.genre_id = :genreId::int
              ))
        GROUP BY m.id
        """;

        // append order/limit/offset safely (sort and direction are allow-listed)
        String sql = baseSql + " ORDER BY " + sort + " " + direction + " LIMIT :limit OFFSET :offset";

        Map<String, Object> params = new HashMap<>();
        params.put("search", (search == null || search.isBlank()) ? null : search);
        params.put("genreId", genreId);
        params.put("limit", limit);
        params.put("offset", offset);

        return jdbc.query(sql, params, MOVIE_MAPPER);
    }

    public List<Movie> findByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        String baseSql = """
        SELECT m.*,
               COALESCE(array_agg(DISTINCT mg.genre_id ORDER BY mg.genre_id), ARRAY[]::integer[]) AS genre_ids
        FROM movies m
        LEFT JOIN movie_genres mg ON mg.movie_id = m.id
        WHERE m.id IN (:ids)
        GROUP BY m.id
        """;

        Map<String, Object> params = new HashMap<>();
        params.put("ids", ids);

        return jdbc.query(baseSql, params, MOVIE_MAPPER);
    }

    public List<GenreMap> getGenreMappings() {
        String sql = "SELECT id, name FROM genres ORDER BY id";
        return jdbc.query(sql, (rs, rowNum) -> new GenreMap(rs.getInt("id"), rs.getString("name")));
    }

}
