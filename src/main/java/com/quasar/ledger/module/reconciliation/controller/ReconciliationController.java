package com.quasar.ledger.module.reconciliation.controller;

import com.quasar.ledger.module.reconciliation.dto.ReconciliationReport;
import com.quasar.ledger.module.reconciliation.service.ReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Admin: reconciliation")
public class ReconciliationController {

    private final ReconciliationService reconciliation;

    public ReconciliationController(ReconciliationService reconciliation) {
        this.reconciliation = reconciliation;
    }

    @PostMapping("/api/v1/admin/reconciliations")
    @Operation(summary = "Run a read-only reconciliation (C1 currency totals, C2 transaction balance, C3 stored vs derived "
            + "balances) and return the report (FR-REC-01..08)")
    public ReconciliationReport run() {
        return reconciliation.run();
    }
}
