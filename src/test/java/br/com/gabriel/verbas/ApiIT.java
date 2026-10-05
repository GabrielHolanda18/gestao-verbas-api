package br.com.gabriel.verbas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ApiIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.6-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
        r.add("server.address", () -> "127.0.0.1");
        r.add("logging.level.org.hibernate.SQL", () -> "WARN");
        r.add("logging.level.org.testcontainers", () -> "INFO");
    }

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    ObjectMapper mapper;

    HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    String base() {
        return "http://127.0.0.1:" + port;
    }

    record Resp(int status, JsonNode body) {
        boolean ok() {
            return status >= 200 && status < 300;
        }
    }

    Resp request(HttpMethod method, String path, String json, String contentType) {
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base() + path))
                    .timeout(Duration.ofSeconds(30));
            if (json != null) {
                b.method(method.name(), HttpRequest.BodyPublishers.ofString(json));
            } else {
                b.method(method.name(), HttpRequest.BodyPublishers.noBody());
            }
            if (contentType != null) {
                b.header(HttpHeaders.CONTENT_TYPE, contentType);
            }
            HttpResponse<String> resp = client.send(b.build(), HttpResponse.BodyHandlers.ofString());
            JsonNode node = resp.body() == null || resp.body().isBlank()
                    ? mapper.nullNode() : mapper.readTree(resp.body());
            return new Resp(resp.statusCode(), node);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    Resp post(String path, Object body) {
        try {
            return request(HttpMethod.POST, path, mapper.writeValueAsString(body), MediaType.APPLICATION_JSON_VALUE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    Resp get(String path) {
        return request(HttpMethod.GET, path, null, null);
    }

    long createDept(String nome, String orcamento) {
        Resp r = post("/departamentos", Map.of("nome", nome, "orcamentoTotal", orcamento));
        assertThat(r.status()).isEqualTo(201);
        return r.body().get("id").asLong();
    }

    long createReq(long deptId, String descricao, String valor) {
        Resp r = post("/departamentos/" + deptId + "/solicitacoes", Map.of("descricao", descricao, "valor", valor));
        assertThat(r.status()).isEqualTo(201);
        return r.body().get("id").asLong();
    }

    String status(long reqId) {
        return jdbc.queryForObject("SELECT status FROM solicitacao WHERE id=?", String.class, reqId);
    }

    BigDecimal committed(long deptId) {
        return jdbc.queryForObject("SELECT valor_comprometido FROM departamento WHERE id=?", BigDecimal.class, deptId);
    }

    int historyCount(long reqId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM historico_solicitacao WHERE solicitacao_id=?", Integer.class, reqId);
    }

    @BeforeEach
    void clean() {
        jdbc.update("TRUNCATE historico_solicitacao, solicitacao, departamento RESTART IDENTITY");
    }

    @Test
    void happyApprovalAndConflicts() {
        long dept = createDept("TI", "10000.00");
        long req = createReq(dept, "notebook", "7000.00");

        Resp approve = post("/solicitacoes/" + req + "/aprovar", Map.of("responsavel", "Maria"));
        assertThat(approve.status()).isEqualTo(200);
        assertThat(approve.body().get("status").asText()).isEqualTo("APROVADA");

        assertThat(committed(dept)).isEqualByComparingTo("7000.00");
        assertThat(status(req)).isEqualTo("APROVADA");
        assertThat(historyCount(req)).isEqualTo(1);
        Map<String, Object> h = jdbc.queryForMap(
                "SELECT status_anterior, status_novo, responsavel, criado_em FROM historico_solicitacao WHERE solicitacao_id=?", req);
        assertThat(h.get("status_anterior")).isEqualTo("PENDENTE");
        assertThat(h.get("status_novo")).isEqualTo("APROVADA");
        assertThat(h.get("responsavel")).isEqualTo("Maria");
        assertThat(h.get("criado_em")).isNotNull();

        assertThat(post("/solicitacoes/" + req + "/aprovar", Map.of("responsavel", "Joao")).status()).isEqualTo(409);
        assertThat(post("/solicitacoes/" + req + "/rejeitar", Map.of("responsavel", "Joao", "justificativa", "x")).status()).isEqualTo(409);

        assertThat(committed(dept)).isEqualByComparingTo("7000.00");
        assertThat(historyCount(req)).isEqualTo(1);
    }

    @Test
    void insufficientBudget() {
        long dept = createDept("RH", "10000.00");
        long first = createReq(dept, "primeira", "7000.00");
        long second = createReq(dept, "segunda", "7000.00");

        assertThat(post("/solicitacoes/" + first + "/aprovar", Map.of("responsavel", "A")).status()).isEqualTo(200);
        Resp refused = post("/solicitacoes/" + second + "/aprovar", Map.of("responsavel", "A"));
        assertThat(refused.status()).isEqualTo(409);

        assertThat(status(second)).isEqualTo("PENDENTE");
        assertThat(historyCount(second)).isEqualTo(0);
        assertThat(committed(dept)).isEqualByComparingTo("7000.00");
    }

    @Test
    void rejectionFlow() {
        long dept = createDept("Marketing", "10000.00");
        long req = createReq(dept, "campanha", "5000.00");

        assertThat(post("/solicitacoes/" + req + "/rejeitar", Map.of("responsavel", "B", "justificativa", "")).status()).isEqualTo(400);
        assertThat(post("/solicitacoes/" + req + "/rejeitar", Map.of("responsavel", "B")).status()).isEqualTo(400);
        assertThat(historyCount(req)).isEqualTo(0);

        Resp reject = post("/solicitacoes/" + req + "/rejeitar", Map.of("responsavel", "B", "justificativa", "fora do orcamento"));
        assertThat(reject.status()).isEqualTo(200);
        assertThat(status(req)).isEqualTo("REJEITADA");
        assertThat(committed(dept)).isEqualByComparingTo("0.00");
        assertThat(historyCount(req)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT justificativa FROM historico_solicitacao WHERE solicitacao_id=?", String.class, req))
                .isEqualTo("fora do orcamento");

        assertThat(post("/solicitacoes/" + req + "/aprovar", Map.of("responsavel", "C")).status()).isEqualTo(409);
        assertThat(committed(dept)).isEqualByComparingTo("0.00");
    }

    @ParameterizedTest
    @CsvSource({"0", "-1", "1.001", "1.000", "1000000000000.00", "abc"})
    void invalidMoneyRejected(String valor) {
        long dept = createDept("Juridico", "10000.00");
        Resp r = post("/departamentos/" + dept + "/solicitacoes", Map.of("descricao", "d", "valor", valor));
        assertThat(r.status()).isEqualTo(400);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM solicitacao", Integer.class)).isEqualTo(0);
    }

    @Test
    void invalidInputsAndMissingResources() {
        assertThat(request(HttpMethod.POST, "/departamentos", "{not json", MediaType.APPLICATION_JSON_VALUE).status()).isEqualTo(400);
        assertThat(post("/departamentos", Map.of("nome", "x")).status()).isEqualTo(400);
        assertThat(post("/departamentos", Map.of("orcamentoTotal", "100.00")).status()).isEqualTo(400);
        assertThat(post("/departamentos", Map.of("nome", "", "orcamentoTotal", "100.00")).status()).isEqualTo(400);
        assertThat(post("/departamentos", Map.of("nome", "x", "orcamentoTotal", "-1.00")).status()).isEqualTo(400);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM departamento", Integer.class)).isEqualTo(0);

        long dept = createDept("Compras", "10000.00");
        assertThat(post("/departamentos/" + dept + "/solicitacoes", Map.of("valor", "10.00")).status()).isEqualTo(400);
        assertThat(post("/departamentos/" + dept + "/solicitacoes", Map.of("descricao", "d")).status()).isEqualTo(400);
        assertThat(post("/departamentos/" + dept + "/solicitacoes", Map.of("descricao", "", "valor", "10.00")).status()).isEqualTo(400);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM solicitacao", Integer.class)).isEqualTo(0);

        long req = createReq(dept, "d", "10.00");
        assertThat(post("/solicitacoes/" + req + "/aprovar", Map.of()).status()).isEqualTo(400);
        assertThat(post("/solicitacoes/" + req + "/rejeitar", Map.of("responsavel", "R", "justificativa", " ")).status()).isEqualTo(400);
        assertThat(historyCount(req)).isEqualTo(0);

        assertThat(get("/departamentos/99999").status()).isEqualTo(404);
        assertThat(get("/solicitacoes/99999").status()).isEqualTo(404);
        assertThat(get("/solicitacoes/99999/historico").status()).isEqualTo(404);
        assertThat(post("/solicitacoes/99999/aprovar", Map.of("responsavel", "R")).status()).isEqualTo(404);
        assertThat(post("/departamentos/99999/solicitacoes", Map.of("descricao", "d", "valor", "1.00")).status()).isEqualTo(404);

        assertThat(get("/solicitacoes?departamentoId=" + dept + "&status=INVALIDA&page=0&").status()).isEqualTo(400);
        assertThat(get("/solicitacoes?departamentoId=" + dept + "&status=PENDENTE&page=-1&size=20").status()).isEqualTo(400);
        assertThat(get("/solicitacoes?departamentoId=" + dept + "&status=PENDENTE&page=0&size=0").status()).isEqualTo(400);
        assertThat(get("/solicitacoes?departamentoId=" + dept + "&status=PENDENTE&page=0&size=101").status()).isEqualTo(400);

        assertThat(request(HttpMethod.PUT, "/solicitacoes/" + req + "/aprovar", "{}", MediaType.APPLICATION_JSON_VALUE).status()).isEqualTo(405);
        assertThat(request(HttpMethod.POST, "/departamentos", "{\"nome\":\"x\"}", MediaType.TEXT_PLAIN_VALUE).status()).isEqualTo(415);
    }

    @Test
    void filtersAndPagination() {
        long d1 = createDept("D1", "10000.00");
        long d2 = createDept("D2", "10000.00");
        long r1 = createReq(d1, "a", "100.00");
        long r2 = createReq(d1, "b", "200.00");
        long r3 = createReq(d1, "c", "300.00");
        long r4 = createReq(d2, "d", "400.00");

        assertThat(post("/solicitacoes/" + r3 + "/rejeitar", Map.of("responsavel", "R", "justificativa", "no")).status()).isEqualTo(200);

        Resp page = get("/solicitacoes?departamentoId=" + d1 + "&status=PENDENTE&page=0&size=2");
        assertThat(page.status()).isEqualTo(200);
        assertThat(page.body().get("size").asInt()).isEqualTo(2);
        assertThat(page.body().get("page").asInt()).isEqualTo(0);
        assertThat(page.body().get("totalElements").asInt()).isEqualTo(2);
        assertThat(page.body().get("totalPages").asInt()).isEqualTo(1);
        assertThat(page.body().get("content")).hasSize(2);
        List<Long> ids = new java.util.ArrayList<>();
        page.body().get("content").forEach(n -> ids.add(n.get("id").asLong()));
        assertThat(ids).containsExactly(r1, r2);

        Resp all = get("/solicitacoes?departamentoId=" + d1 + "&page=0&size=20");
        assertThat(all.body().get("totalElements").asInt()).isEqualTo(3);

        Resp d2Page = get("/solicitacoes?departamentoId=" + d2 + "&page=0&size=20");
        assertThat(d2Page.body().get("totalElements").asInt()).isEqualTo(1);
        assertThat(d2Page.body().get("content").get(0).get("id").asLong()).isEqualTo(r4);

        Resp hist = get("/solicitacoes/" + r3 + "/historico");
        assertThat(hist.status()).isEqualTo(200);
        assertThat(hist.body()).hasSize(1);
        assertThat(hist.body().get(0).get("statusNovo").asText()).isEqualTo("REJEITADA");
    }

    @Test
    void realRollbackViaTestTrigger() {
        long dept = createDept("Financeiro", "10000.00");
        long req = createReq(dept, "servidor", "7000.00");

        jdbc.execute("CREATE OR REPLACE FUNCTION verbas_test_fail_20240601() RETURNS trigger AS $$ " +
                "BEGIN " +
                "IF NOT EXISTS (SELECT 1 FROM solicitacao s JOIN departamento d ON d.id=s.departamento_id " +
                "WHERE s.id=NEW.solicitacao_id AND s.status='APROVADA' AND d.valor_comprometido=7000) THEN " +
                "RETURN NEW; END IF; " +
                "RAISE EXCEPTION 'falha controlada do teste'; " +
                "END $$ LANGUAGE plpgsql");
        jdbc.execute("CREATE TRIGGER verbas_test_trg_20240601 BEFORE INSERT ON historico_solicitacao " +
                "FOR EACH ROW EXECUTE FUNCTION verbas_test_fail_20240601()");
        try {
            Resp r = post("/solicitacoes/" + req + "/aprovar", Map.of("responsavel", "Maria"));
            assertThat(r.ok()).as("approval must not succeed").isFalse();
            assertThat(r.status()).isEqualTo(500);
        } finally {
            jdbc.execute("DROP TRIGGER IF EXISTS verbas_test_trg_20240601 ON historico_solicitacao");
            jdbc.execute("DROP FUNCTION IF EXISTS verbas_test_fail_20240601()");
        }

        // O gatilho só falha se reserva e situação já foram gravadas na mesma transação.
        assertThat(status(req)).isEqualTo("PENDENTE");
        assertThat(committed(dept)).isEqualByComparingTo("0.00");
        assertThat(historyCount(req)).isEqualTo(0);
    }

    @RepeatedTest(3)
    void concurrentApprovalDifferentRequests() throws Exception {
        long dept = createDept("Concorrencia", "10000.00");
        long r1 = createReq(dept, "x", "7000.00");
        long r2 = createReq(dept, "y", "7000.00");

        runConcurrent(
                () -> post("/solicitacoes/" + r1 + "/aprovar", Map.of("responsavel", "A")),
                () -> post("/solicitacoes/" + r2 + "/aprovar", Map.of("responsavel", "B")));

        assertThat(committed(dept)).isEqualByComparingTo("7000.00");
        assertThat(historyCount(r1) + historyCount(r2)).isEqualTo(1);
        assertThat(List.of(status(r1), status(r2))).containsExactlyInAnyOrder("APROVADA", "PENDENTE");
    }

    @RepeatedTest(3)
    void concurrentSameRequestApproveAndReject() throws Exception {
        long dept = createDept("Misto", "10000.00");
        long req = createReq(dept, "z", "7000.00");

        runConcurrent(
                () -> post("/solicitacoes/" + req + "/aprovar", Map.of("responsavel", "A")),
                () -> post("/solicitacoes/" + req + "/rejeitar", Map.of("responsavel", "B", "justificativa", "no")));

        assertThat(historyCount(req)).isEqualTo(1);
        String terminal = status(req);
        assertThat(terminal).isIn("APROVADA", "REJEITADA");
        assertThat(committed(dept)).isEqualByComparingTo("APROVADA".equals(terminal) ? "7000.00" : "0.00");
    }

    @RepeatedTest(3)
    void concurrentSameRequestTwoApprovals() throws Exception {
        long dept = createDept("Duplo", "10000.00");
        long req = createReq(dept, "w", "7000.00");

        runConcurrent(
                () -> post("/solicitacoes/" + req + "/aprovar", Map.of("responsavel", "A")),
                () -> post("/solicitacoes/" + req + "/aprovar", Map.of("responsavel", "B")));

        assertThat(status(req)).isEqualTo("APROVADA");
        assertThat(committed(dept)).isEqualByComparingTo("7000.00");
        assertThat(historyCount(req)).isEqualTo(1);
    }

    private void runConcurrent(java.util.function.Supplier<Resp> a, java.util.function.Supplier<Resp> b) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> f1 = pool.submit(() -> {
                ready.countDown();
                start.await();
                return a.get().status();
            });
            Future<Integer> f2 = pool.submit(() -> {
                ready.countDown();
                start.await();
                return b.get().status();
            });
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            int s1 = f1.get(30, TimeUnit.SECONDS);
            int s2 = f2.get(30, TimeUnit.SECONDS);
            assertThat(List.of(s1, s2)).containsExactlyInAnyOrder(200, 409);
        } finally {
            start.countDown();
            pool.shutdownNow();
            assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void databaseConstraints() {
        jdbc.execute("INSERT INTO departamento (nome, orcamento_total, valor_comprometido, criado_em) " +
                "VALUES ('Valido', 1000.00, 0.00, CURRENT_TIMESTAMP)");
        Long deptId = jdbc.queryForObject("SELECT id FROM departamento WHERE nome='Valido'", Long.class);
        jdbc.execute("INSERT INTO solicitacao (departamento_id, descricao, valor, status, criado_em) " +
                "VALUES (" + deptId + ", 'valida', 100.00, 'PENDENTE', CURRENT_TIMESTAMP)");
        Long reqId = jdbc.queryForObject("SELECT id FROM solicitacao WHERE descricao='valida'", Long.class);

        assertViolation("INSERT INTO departamento (nome, orcamento_total, valor_comprometido, criado_em) " +
                "VALUES ('Neg', -1.00, 0.00, CURRENT_TIMESTAMP)");
        assertViolation("INSERT INTO departamento (nome, orcamento_total, valor_comprometido, criado_em) " +
                "VALUES ('Over', 100.00, 200.00, CURRENT_TIMESTAMP)");
        assertViolation("INSERT INTO departamento (nome, orcamento_total, valor_comprometido, criado_em) " +
                "VALUES ('', 100.00, 0.00, CURRENT_TIMESTAMP)");
        assertViolation("INSERT INTO solicitacao (departamento_id, descricao, valor, status, criado_em) " +
                "VALUES (" + deptId + ", 'bad', 10.00, 'INVALIDA', CURRENT_TIMESTAMP)");
        assertViolation("INSERT INTO solicitacao (departamento_id, descricao, valor, status, criado_em) " +
                "VALUES (99999, 'fk', 10.00, 'PENDENTE', CURRENT_TIMESTAMP)");
        assertViolation("INSERT INTO historico_solicitacao (solicitacao_id, status_anterior, status_novo, responsavel, justificativa, criado_em) " +
                "VALUES (" + reqId + ", 'PENDENTE', 'APROVADA', '', NULL, CURRENT_TIMESTAMP)");
        assertViolation("INSERT INTO historico_solicitacao (solicitacao_id, status_anterior, status_novo, responsavel, justificativa, criado_em) " +
                "VALUES (" + reqId + ", 'PENDENTE', 'REJEITADA', 'R', NULL, CURRENT_TIMESTAMP)");

        jdbc.execute("INSERT INTO historico_solicitacao " +
                "(solicitacao_id, status_anterior, status_novo, responsavel, justificativa, criado_em) " +
                "VALUES (" + reqId + ", 'PENDENTE', 'APROVADA', 'R', 'ok', CURRENT_TIMESTAMP)");
        assertViolation("INSERT INTO historico_solicitacao " +
                "(solicitacao_id, status_anterior, status_novo, responsavel, justificativa, criado_em) " +
                "VALUES (" + reqId + ", 'PENDENTE', 'APROVADA', 'R2', 'dup', CURRENT_TIMESTAMP)");
    }

    private void assertViolation(String sql) {
        // Direct JDBC in autocommit mode: a failed statement does not poison other statements.
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.execute(sql))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
