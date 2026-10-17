package de.talentmatch.common.persistence;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import de.talentmatch.job.JobPosition;

/**
 * Kleine, idempotente Schema-Anpassungen für bestehende Datenbanken. {@code ddl-auto: update} legt neue Spalten an,
 * vergrößert aber keine vorhandenen – das übernimmt diese Klasse beim Start (H2 und PostgreSQL).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SchemaUpgrade implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(SchemaUpgrade.class);

    private final JdbcTemplate jdbcTemplate;

    public SchemaUpgrade(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        widenVarchar("job_position", "description", JobPosition.MAX_DESCRIPTION_LENGTH);
    }

    /**
     * Vergrößert eine VARCHAR-Spalte, falls sie kürzer als {@code length} ist.
     */
    void widenVarchar(String table, String column, int length) {
        List<Integer> current = jdbcTemplate.queryForList("""
                select character_maximum_length from information_schema.columns
                where lower(table_name) = ? and lower(column_name) = ?
                """, Integer.class, table, column);
        if (current.isEmpty() || current.getFirst() == null || current.getFirst() >= length) {
            return;
        }
        // SET DATA TYPE verstehen sowohl PostgreSQL als auch H2.
        jdbcTemplate.execute("alter table " + table + " alter column " + column
                + " set data type varchar(" + length + ")");
        LOG.info("Spalte {}.{} von {} auf {} Zeichen vergrößert.", table, column, current.getFirst(), length);
    }
}
