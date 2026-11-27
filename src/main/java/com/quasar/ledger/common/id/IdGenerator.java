package com.quasar.ledger.common.id;

/** Provider interface for ID generation so tests can substitute it (NFR-MAIN-03, SE-6). */
public interface IdGenerator {

    String generate(IdKind kind);
}
