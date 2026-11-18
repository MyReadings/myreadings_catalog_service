package org.modular.playground.catalog.web.controllers;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.util.Map;
import static io.restassured.RestAssured.given;

// Explicit disposable database only: test schema is recreated.
@QuarkusTest
@TestProfile(ApiValidationTest.DatabaseProfile.class)
@TestSecurity(user = "demo", roles = {"user", "admin"})
@EnabledIfEnvironmentVariable(named = "DEMO_TEST_DB_URL", matches = ".+")
public class ApiValidationTest {
    public static class DatabaseProfile implements QuarkusTestProfile {
        public Map<String, String> getConfigOverrides() {
            return Map.ofEntries(
                Map.entry("quarkus.devservices.enabled", "false"),
                Map.entry("quarkus.oidc.enabled", "false"),
                Map.entry("quarkus.otel.sdk.disabled", "true"),
                Map.entry("quarkus.http.test-port", "0"),
                Map.entry("quarkus.datasource.\"books-db\".username", "postgres"),
                Map.entry("quarkus.datasource.\"books-db\".password", "demo-test-only"),
                Map.entry("quarkus.datasource.\"books-db\".jdbc.url", System.getenv().getOrDefault("DEMO_TEST_DB_URL", "jdbc:postgresql://localhost:1/demo_test")),
                Map.entry("quarkus.hibernate-orm.\"books-db\".database.generation", "drop-and-create"));
        }
    }
    @Test
    void invalidPaginationIsRejectedAndNormalSearchStillWorks() {
        for (String query : new String[] {"page=-1", "page=2147483647", "size=0", "size=-1", "size=101"}) {
            given().get("/api/v1/books/search?query=demo&" + query).then().statusCode(400).contentType("application/json");
        }
        for (int limit : new int[] {0, -1, 101}) {
            given().get("/api/v1/books?limit=" + limit).then().statusCode(400);
        }
        given().get("/api/v1/books/search?query=demo&page=0&size=12").then().statusCode(200);
        given().get("/api/v1/books").then().statusCode(200);
    }
    @Test
    void nullElementsAndOversizedBatchesAreRejected() {
        String oversized = "[" + String.join(",", java.util.Collections.nCopies(501, "\"00000000-0000-0000-0000-000000000001\"")) + "]";
        for (String path : new String[] {"/api/v1/books/batch", "/api/internal/books/batch"}) {
            for (String payload : new String[] {"null", "[null]", oversized}) {
                given().contentType("application/json").body(payload).post(path).then().statusCode(400).contentType("application/json");
            }
        }
    }
    @Test
    void nullCreateBodyIsRejected() {
        given().contentType("application/json").body("null").post("/api/v1/books").then().statusCode(400).contentType("application/json");
    }
}
