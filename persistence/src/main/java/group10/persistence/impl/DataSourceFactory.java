package group10.persistence.impl;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;

import javax.sql.DataSource;
import java.util.Objects;


public final class DataSourceFactory {
    private DataSourceFactory() {}

    public static DataSource createFromEnv(Dotenv dotenv) {
        HikariConfig cfg = new HikariConfig();

        // read from env
        String JDBC_URL = dotenv.get("JDBC_URL", "jdbc:postgresql://localhost:5432/sorting_game");
        String JDBC_PASS = dotenv.get("JDBC_PASS", "postgres");
        String JDBC_USER = dotenv.get("JDBC_USER", "postgres");
        String DB_POOL_MAX = dotenv.get("DB_POOL_MAX", "10");

        //set hikaru cfg
        cfg.setJdbcUrl(JDBC_URL);
        cfg.setUsername(JDBC_USER);
        cfg.setPassword(JDBC_PASS);
        cfg.setMaximumPoolSize(Integer.parseInt(DB_POOL_MAX));
        cfg.setMinimumIdle(2);
        cfg.setPoolName("game-hikari-pool");


        // optional tuning:
        cfg.setConnectionTimeout(10_000);
        cfg.setIdleTimeout(600_000);
        cfg.setMaxLifetime(1_800_000);
        return new HikariDataSource(cfg);
    }
}
