package com.onlineexamsystem.backend.config;

import com.onlineexamsystem.backend.common.util.AppPathResolver;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class StartupInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupInitializer.class);

    private final AppPathResolver appPathResolver;
    private final DataSource dataSource;
    private final Environment environment;

    public StartupInitializer(AppPathResolver appPathResolver, DataSource dataSource, Environment environment) {
        this.appPathResolver = appPathResolver;
        this.dataSource = dataSource;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        appPathResolver.ensureDirectories();
        ensureExamCompatibilityColumns();
        printStartupMessage();
    }

    private void ensureExamCompatibilityColumns() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            ResultSet tableRs = connection.getMetaData().getTables(connection.getCatalog(), null, "exams", null);
            if (!tableRs.next()) {
                return;
            }

            boolean hasAttemptLimit = hasColumn(connection, "exams", "attempt_limit");
            boolean hasShuffleQuestions = hasColumn(connection, "exams", "shuffle_questions");

            if (!hasAttemptLimit) {
                statement.execute("ALTER TABLE exams ADD COLUMN attempt_limit INT NOT NULL DEFAULT 1");
            }
            if (!hasShuffleQuestions) {
                statement.execute("ALTER TABLE exams ADD COLUMN shuffle_questions TINYINT(1) NOT NULL DEFAULT 0");
            }
        }
    }

    private boolean hasColumn(Connection connection, String tableName, String columnName) throws Exception {
        try (ResultSet columns = connection.getMetaData().getColumns(connection.getCatalog(), null, tableName, columnName)) {
            return columns.next();
        }
    }

    private void printStartupMessage() {
        String port = environment.getProperty("local.server.port", environment.getProperty("server.port", "8010"));
        String rootUrl = "http://127.0.0.1:" + port;
        log.info("在线考试系统 Java 后端已启动");
        log.info("本地访问地址: {}/static/user/index.html", rootUrl);
        log.info("健康检查地址: {}/health", rootUrl);
    }
}
