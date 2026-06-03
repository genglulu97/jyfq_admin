package com.jyfq.loan.service.upstream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

@Component
public class InstitutionSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InstitutionSchemaInitializer.class);

    private final DataSource dataSource;

    public InstitutionSchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            addColumnIfMissing(connection, "channel_type",
                    "ALTER TABLE `institution` ADD COLUMN `channel_type` VARCHAR(64) NOT NULL DEFAULT '全流程API-CPS' COMMENT 'Channel type for matching' AFTER `merchant_type`");
        }
    }

    private void addColumnIfMissing(Connection connection, String columnName, String ddl) throws Exception {
        if (hasColumn(connection, columnName)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(ddl);
            log.info("[SCHEMA] added institution.{}", columnName);
        }
    }

    private boolean hasColumn(Connection connection, String columnName) throws Exception {
        String sql = "SELECT COUNT(1) FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'institution' AND COLUMN_NAME = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }
}
