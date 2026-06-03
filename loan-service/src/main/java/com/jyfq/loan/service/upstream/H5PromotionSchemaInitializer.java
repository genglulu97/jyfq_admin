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
public class H5PromotionSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(H5PromotionSchemaInitializer.class);

    private final DataSource dataSource;

    public H5PromotionSchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            if (hasTable(connection, "h5_promotion_event")) {
                return;
            }
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE `h5_promotion_event` ("
                        + "`id` BIGINT PRIMARY KEY AUTO_INCREMENT,"
                        + "`channel_id` BIGINT NOT NULL COMMENT 'Channel ID',"
                        + "`channel_code` VARCHAR(32) NOT NULL COMMENT 'Channel code snapshot',"
                        + "`event_type` VARCHAR(32) NOT NULL COMMENT 'PV, CLICK, REGISTER, COMPLETE',"
                        + "`visitor_id` VARCHAR(64) DEFAULT NULL COMMENT 'Visitor ID',"
                        + "`session_id` VARCHAR(64) DEFAULT NULL COMMENT 'Session ID',"
                        + "`page_url` VARCHAR(1024) DEFAULT NULL COMMENT 'Page URL',"
                        + "`referer` VARCHAR(1024) DEFAULT NULL COMMENT 'Referer',"
                        + "`user_agent` VARCHAR(512) DEFAULT NULL COMMENT 'User agent',"
                        + "`device_ip` VARCHAR(45) DEFAULT NULL COMMENT 'Client IP',"
                        + "`ext_json` TEXT DEFAULT NULL COMMENT 'Extension JSON',"
                        + "`created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                        + "`create_by` VARCHAR(64) DEFAULT NULL,"
                        + "`updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
                        + "`update_by` VARCHAR(64) DEFAULT NULL,"
                        + "INDEX `idx_h5_event_channel_time` (`channel_id`, `created_at`),"
                        + "INDEX `idx_h5_event_code_time` (`channel_code`, `created_at`),"
                        + "INDEX `idx_h5_event_type_time` (`event_type`, `created_at`)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='H5 promotion event'");
                log.info("[SCHEMA] created h5_promotion_event");
            }
        }
    }

    private boolean hasTable(Connection connection, String tableName) throws Exception {
        String sql = "SELECT COUNT(1) FROM information_schema.TABLES "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }
}
