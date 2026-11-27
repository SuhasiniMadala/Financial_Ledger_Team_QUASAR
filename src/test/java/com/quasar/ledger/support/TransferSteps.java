package com.quasar.ledger.support;

import com.quasar.ledger.support.ApiClient.Response;
import java.util.LinkedHashMap;
import java.util.Map;

/** Reusable API steps for transfers and payments (owned by the Transfers & History module). */
public final class TransferSteps {

    private TransferSteps() {
    }

    public static Response transfer(ApiClient api, String token, String from, String to, Object amountMinor, String key) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sourceWalletId", from);
        body.put("destinationWalletId", to);
        body.put("amountMinor", amountMinor);
        return api.postIdempotent("/api/v1/transfers", body, token, key);
    }

    public static Response pay(ApiClient api, String token, String from, String merchantCode, Object amountMinor, String reference,
                               String key) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sourceWalletId", from);
        body.put("merchantCode", merchantCode);
        body.put("amountMinor", amountMinor);
        if (reference != null) {
            body.put("reference", reference);
        }
        return api.postIdempotent("/api/v1/payments", body, token, key);
    }
}
