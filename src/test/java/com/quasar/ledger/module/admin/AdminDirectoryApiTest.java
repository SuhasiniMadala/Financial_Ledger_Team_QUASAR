package com.quasar.ledger.module.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.quasar.ledger.support.ApiClient.Response;
import com.quasar.ledger.support.ApiTestBase;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** SRS 4.8: admin directory and wallet status (FR-ADM-01, 02, 04, 05, 06; BR-08). */
class AdminDirectoryApiTest extends ApiTestBase {

    @Test
    @DisplayName("FR-ADM-01: admin lists users and merchants (merchants with merchant code)")
    void listsUsersAndMerchants() {
        String admin = adminToken();
        TestUser asha = registerUser("Asha Rao");
        TestMerchant cafe = registerMerchant("Campus Cafe");
        JsonNode users = allItems("/api/v1/admin/users", admin);
        JsonNode merchants = allItems("/api/v1/admin/merchants", admin);
        assertThat(users.findValuesAsText("accountId")).contains(asha.accountId()).doesNotContain(cafe.accountId());
        JsonNode cafeRow = find(merchants, cafe.accountId());
        assertThat(cafeRow.get("merchantCode").asText()).isEqualTo(cafe.merchantCode());
        assertThat(cafeRow.get("name").asText()).isEqualTo("Campus Cafe");
        assertThat(cafeRow.get("email").asText()).isEqualTo(cafe.email());
        assertThat(cafeRow.get("registeredAt").asText()).endsWith("Z");
    }

    @Test
    @DisplayName("FR-ADM-02 / ACC-01: admin lists all accounts including one System Funding Account per currency")
    void listsAccountsIncludingSystemFunding() {
        JsonNode accounts = allItems("/api/v1/admin/accounts", adminToken());
        JsonNode inr = find(accounts, "SYS-FUND-INR");
        assertThat(inr.get("type").asText()).isEqualTo("SYSTEM_FUNDING");
        assertThat(inr.get("ownerName").asText()).isEqualTo("System Funding");
        assertThat(inr.has("ownerId")).isFalse();
        assertThat(find(accounts, "SYS-FUND-USD")).isNotNull();
    }

    @Test
    void pagingIsValidated() {
        String admin = adminToken();
        assertThat(api.get("/api/v1/admin/users?size=100", admin).status()).isEqualTo(200);
        assertThat(api.get("/api/v1/admin/users?size=101", admin).status()).isEqualTo(400);
        assertThat(api.get("/api/v1/admin/users?size=0", admin).status()).isEqualTo(400);
        assertThat(api.get("/api/v1/admin/users?page=-1", admin).status()).isEqualTo(400);
        assertThat(api.get("/api/v1/admin/users?page=abc", admin).status()).isEqualTo(400);
        Response r = api.get("/api/v1/admin/users", admin);
        assertThat(r.body().get("size").asInt()).isEqualTo(20);
    }

    @Test
    @DisplayName("FR-ADM-04/05: freeze, unfreeze, deactivate; invalid transitions are 409; every change is recorded")
    void walletStatusTransitions() {
        String admin = adminToken();
        TestUser ravi = registerUser("Ravi Kumar");
        String wallet = createWallet(ravi.token(), "INR");

        assertThat(status(admin, wallet, "unfreeze").status()).isEqualTo(409);
        Response frozen = status(admin, wallet, "freeze");
        assertThat(frozen.status()).isEqualTo(200);
        assertThat(frozen.text("status")).isEqualTo("FROZEN");
        assertThat(status(admin, wallet, "freeze").code()).isEqualTo("INVALID_STATE_TRANSITION");
        assertThat(status(admin, wallet, "unfreeze").text("status")).isEqualTo("ACTIVE");
        assertThat(status(admin, wallet, "deactivate").text("status")).isEqualTo("DEACTIVATED");
        Response afterFinal = status(admin, wallet, "unfreeze");
        assertThat(afterFinal.status()).isEqualTo(409);
        assertThat(afterFinal.code()).isEqualTo("INVALID_STATE_TRANSITION");
        assertThat(status(admin, wallet, "freeze").status()).isEqualTo(409);

        List<Map<String, Object>> log = jdbc.queryForList(
                "SELECT old_status, new_status, changed_by, reason FROM wallet_status_change WHERE account_id = ? ORDER BY rowid", wallet);
        // Three successful changes (freeze, unfreeze, deactivate); rejected transitions record nothing.
        assertThat(log).hasSize(3);
        assertThat(log.get(0)).containsEntry("old_status", "ACTIVE").containsEntry("new_status", "FROZEN")
                .containsEntry("reason", "reported lost device");
        assertThat(log.get(0).get("changed_by")).isEqualTo(jdbc.queryForObject(
                "SELECT id FROM user_account WHERE role = 'ADMIN'", String.class));
    }

    @Test
    @DisplayName("FR-ADM-05/06: a reason is required; status changes never change the balance")
    void reasonRequiredAndBalanceUntouched() {
        String admin = adminToken();
        TestUser ravi = registerUser("Ravi Kumar");
        String wallet = createWallet(ravi.token(), "INR");
        Response noReason = api.post("/api/v1/admin/wallets/" + wallet + "/freeze", Map.of("reason", " "), admin);
        assertThat(noReason.status()).isEqualTo(400);
        assertThat(api.post("/api/v1/admin/wallets/" + wallet + "/freeze", Map.of(), admin).status()).isEqualTo(400);
        long before = storedBalance(wallet);
        status(admin, wallet, "freeze");
        assertThat(storedBalance(wallet)).isEqualTo(before);
        assertThat(count("SELECT COUNT(*) FROM ledger_entry WHERE account_id = ?", wallet)).isZero();
    }

    @Test
    void systemAccountsAndUnknownWalletsCannotChangeStatus() {
        String admin = adminToken();
        assertThat(status(admin, "W-0000000000", "freeze").status()).isEqualTo(404);
        assertThat(api.post("/api/v1/admin/wallets/SYS-FUND-INR/freeze", Map.of("reason", "test"), admin).status())
                .isEqualTo(400);
    }

    @Test
    @DisplayName("BR-08 / FR-AUTH-08: only ADMIN can change wallet status")
    void nonAdminCannotChangeStatus() {
        TestUser asha = registerUser("Asha Rao");
        String wallet = createWallet(asha.token(), "INR");
        Response r = api.post("/api/v1/admin/wallets/" + wallet + "/freeze", Map.of("reason", "self freeze"), asha.token());
        assertThat(r.status()).isEqualTo(403);
    }

    private Response status(String admin, String wallet, String action) {
        return api.post("/api/v1/admin/wallets/" + wallet + "/" + action, Map.of("reason", "reported lost device"), admin);
    }

    private JsonNode allItems(String path, String token) {
        Response r = api.get(path + "?size=100", token);
        assertThat(r.status()).as("%s", r).isEqualTo(200);
        int pages = r.body().get("totalPages").asInt();
        var all = objectMapper.createArrayNode();
        for (int p = 0; p < pages; p++) {
            api.get(path + "?size=100&page=" + p, token).body().get("items").forEach(all::add);
        }
        return all;
    }

    private static JsonNode find(JsonNode items, String accountId) {
        for (JsonNode n : items) {
            if (accountId.equals(n.get("accountId").asText())) {
                return n;
            }
        }
        throw new AssertionError("Not listed: " + accountId);
    }
}
