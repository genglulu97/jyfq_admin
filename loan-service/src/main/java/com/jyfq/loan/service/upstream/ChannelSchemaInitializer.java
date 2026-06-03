package com.jyfq.loan.service.upstream;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

@Component
public class ChannelSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ChannelSchemaInitializer.class);

    private final DataSource dataSource;

    public ChannelSchemaInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            addColumnIfMissing(connection, "min_price",
                    "ALTER TABLE `channel` ADD COLUMN `min_price` DECIMAL(18,2) DEFAULT NULL COMMENT 'Minimum response price for this channel' AFTER `fee_rate`");
            addColumnIfMissing(connection, "max_price",
                    "ALTER TABLE `channel` ADD COLUMN `max_price` DECIMAL(18,2) DEFAULT NULL COMMENT 'Maximum response price for this channel' AFTER `min_price`");
            addColumnIfMissing(connection, "price_return_mode",
                    "ALTER TABLE `channel` ADD COLUMN `price_return_mode` VARCHAR(32) NOT NULL DEFAULT 'BEFORE_PROFIT' COMMENT 'BEFORE_PROFIT or AFTER_PROFIT' AFTER `max_price`");
        }
    }

    private void addColumnIfMissing(Connection connection, String columnName, String ddl) throws Exception {
        if (hasColumn(connection, columnName)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(ddl);
            log.info("[SCHEMA] added channel.{}", columnName);
        }
    }

    private boolean hasColumn(Connection connection, String columnName) throws Exception {
        String sql = "SELECT COUNT(1) FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'channel' AND COLUMN_NAME = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }
}
