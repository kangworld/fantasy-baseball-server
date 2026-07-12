package com.kangworld.fantasybaseball.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

@MySqlContainerIntegrationTest
class FlywayMySqlIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 실제_MySQL에_Flyway_마이그레이션을_적용한다() {
        // given
        String databaseProductName = jdbcTemplate.execute(
                (ConnectionCallback<String>) connection -> connection.getMetaData().getDatabaseProductName());

        // when
        Integer memberTableCount = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.tables
                where table_schema = database()
                  and table_name = 'member'
                """, Integer.class);
        Integer migrationCount = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where success = 1",
                Integer.class);

        // then
        assertThat(databaseProductName).isEqualTo("MySQL");
        assertThat(memberTableCount).isEqualTo(1);
        assertThat(migrationCount).isEqualTo(9);
    }

}
