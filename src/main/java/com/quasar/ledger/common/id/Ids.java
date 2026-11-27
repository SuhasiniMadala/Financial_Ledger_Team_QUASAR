package com.quasar.ledger.common.id;

import com.quasar.ledger.common.api.ErrorCode;
import com.quasar.ledger.common.exception.ApiException;

/** Regular expressions for identifiers accepted from clients; malformed IDs get HTTP 400 (SRS Table 3-2). */
public final class Ids {

    public static final String WALLET_ID = "^W-[0-9A-Z]{10}$";
    public static final String ACCOUNT_ID = "^(W-[0-9A-Z]{10}|SYS-FUND-[A-Z]{3})$";
    public static final String TRANSACTION_ID = "^T-[0-9A-Z]{12}$";
    public static final String MERCHANT_CODE = "^MER-[0-9A-Z]{6}$";

    private Ids() {
    }

    public static String require(String value, String regex, String field) {
        if (value == null || !value.matches(regex)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, field + " is malformed");
        }
        return value;
    }

    public static String systemFundingId(String currency) {
        return "SYS-FUND-" + currency;
    }
}
