package cl.rosta.devopsProject.configuracion;

import java.lang.reflect.Field;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import com.zaxxer.hikari.pool.HikariPool;

@Component
public class DatabaseConnectionManager {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DatabaseConnectionManager(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public <T> T executeQuery(String sql, RowMapper<T> rowMapper, Object... params) {
        try {
            return jdbcTemplate.queryForObject(sql, rowMapper, params);
        } finally {
            // JdbcTemplate maneja internamente el cierre de conexiones
            // pero podemos forzar la limpieza del pool si es necesario
            cleanupConnectionPool();
        }
    }

    public <T> List<T> executeQueryForList(String sql, RowMapper<T> rowMapper, Object... params) {
        try {
            return jdbcTemplate.query(sql, rowMapper, params);
        } finally {
            cleanupConnectionPool();
        }
    }

    public boolean executeUpdate(String sql, Object... params) {
        try {
            int result = jdbcTemplate.update(sql, params);
            return result > 0;
        } finally {
            cleanupConnectionPool();
        }
    }

    private void cleanupConnectionPool() {
        // Si estás usando HikariCP como pool de conexiones (recomendado)
        try {
            Field poolField = jdbcTemplate.getDataSource().getClass().getDeclaredField("pool");
            poolField.setAccessible(true);
            HikariPool pool = (HikariPool) poolField.get(jdbcTemplate.getDataSource());
            pool.softEvictConnections();
        } catch (Exception e) {
            // Log the exception but don't throw it
            System.err.println("Error cleaning up connection pool: " + e.getMessage());
        }
    }
}
