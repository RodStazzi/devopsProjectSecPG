package cl.rosta.devopsProject.configuracion;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;


@Configuration
public class DatabaseConfig {
    @Value("${spring.datasource.url}")
    private String jdbcUrl;
    
    @Value("${spring.datasource.username}")
    private String username;
    
    @Value("${spring.datasource.password}")
    private String password;
    
    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        
        // Configuración del pool de conexiones
        config.setMaximumPoolSize(3);
        config.setMinimumIdle(1);
        config.setIdleTimeout(300000); // 5 minutos
        config.setMaxLifetime(600000); // 10 minutos
        config.setConnectionTimeout(20000); // 30 segundos
        config.setLeakDetectionThreshold(60000); // 1 minuto

        return new HikariDataSource(config);
    }
}



