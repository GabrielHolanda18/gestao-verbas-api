package br.com.gabriel.verbas;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import org.postgresql.ds.PGSimpleDataSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MigrationIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.6-alpine");

    private DataSource dataSource(String schema) {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setURL(POSTGRES.getJdbcUrl());
        ds.setUser(POSTGRES.getUsername());
        ds.setPassword(POSTGRES.getPassword());
        ds.setCurrentSchema(schema);
        return ds;
    }

    @Test
    void migrationsApplyIncrementallyAndPreserveData() throws Exception {
        String url = POSTGRES.getJdbcUrl();
        String user = POSTGRES.getUsername();
        String pass = POSTGRES.getPassword();

        Flyway flyway = Flyway.configure()
                .dataSource(url, user, pass)
                .locations("classpath:db/migration")
                .target("1")
                .load();
        flyway.migrate();

        try (Connection c = DriverManager.getConnection(url, user, pass);
             Statement st = c.createStatement()) {
            st.execute("INSERT INTO departamento (nome, orcamento_total, valor_comprometido, criado_em) " +
                    "VALUES ('Pre-V2', 5000.00, 0.00, CURRENT_TIMESTAMP)");
            st.execute("INSERT INTO solicitacao (departamento_id, descricao, valor, status, criado_em) " +
                    "SELECT id, 'solicitacao pre-v2', 100.00, 'PENDENTE', CURRENT_TIMESTAMP FROM departamento WHERE nome='Pre-V2'");
        }

        Flyway full = Flyway.configure()
                .dataSource(url, user, pass)
                .locations("classpath:db/migration")
                .load();
        full.migrate();
        full.validate();

        assertThat(full.info().current().getVersion().getVersion()).isEqualTo("2");

        try (Connection c = DriverManager.getConnection(url, user, pass);
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM departamento WHERE nome='Pre-V2'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isEqualTo(1);
        }
        try (Connection c = DriverManager.getConnection(url, user, pass);
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM solicitacao WHERE descricao='solicitacao pre-v2'")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isEqualTo(1);
        }

        try (Connection c = DriverManager.getConnection(url, user, pass);
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                    "SELECT indexname FROM pg_indexes WHERE schemaname='public'")) {
            List<String> indexes = new ArrayList<>();
            while (rs.next()) {
                indexes.add(rs.getString(1));
            }
            assertThat(indexes).contains("idx_solicitacao_departamento_status_id", "idx_historico_solicitacao_id");
        }
    }

    @Test
    void freshSchemaMigratesWithExactlyBusinessTables() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource("fresh"))
                .locations("classpath:db/migration")
                .schemas("fresh")
                .defaultSchema("fresh")
                .createSchemas(true)
                .load();
        flyway.migrate();
        flyway.validate();

        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("2");

        try (Connection c = POSTGRES.createConnection("");
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                    "SELECT table_name FROM information_schema.tables " +
                    "WHERE table_schema='fresh' ORDER BY table_name")) {
            List<String> tables = new ArrayList<>();
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
            assertThat(tables).containsExactlyInAnyOrder(
                    "departamento", "solicitacao", "historico_solicitacao", "flyway_schema_history");
        }
    }
}
