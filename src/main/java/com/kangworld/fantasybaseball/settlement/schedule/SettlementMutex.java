package com.kangworld.fantasybaseball.settlement.schedule;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class SettlementMutex {

    private final JdbcTemplate jdbcTemplate;

    SettlementOutcome run(Supplier<SettlementOutcome> settlement) {
        // Keep the MySQL session open across independently committed settlement steps.
        return jdbcTemplate.execute((ConnectionCallback<SettlementOutcome>) connection -> {
            if (!callLockFunction(connection, "select get_lock(concat(database(), ':fantasy-settlement'), 0)"))
                return SettlementOutcome.ALREADY_RUNNING;

            try {
                return settlement.get();
            } finally {
                callLockFunction(connection, "select release_lock(concat(database(), ':fantasy-settlement'))");
            }
        });
    }

    private boolean callLockFunction(Connection connection, String sql) throws SQLException {
        try (var statement = connection.prepareStatement(sql);
             var result = statement.executeQuery()) {
            if (!result.next())
                throw new SQLException("MySQL settlement lock returned no result");

            int status = result.getInt(1);
            if (result.wasNull())
                throw new SQLException("MySQL could not resolve the settlement lock");

            return status == 1;
        }
    }

}
