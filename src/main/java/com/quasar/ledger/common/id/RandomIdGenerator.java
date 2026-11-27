package com.quasar.ledger.common.id;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/** Default ID generator: prefix + random characters from an unambiguous upper-case alphabet. */
@Component
public class RandomIdGenerator implements IdGenerator {

    static final String ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ";
    private final SecureRandom random = new SecureRandom();

    @Override
    public String generate(IdKind kind) {
        StringBuilder sb = new StringBuilder(kind.prefix());
        for (int i = 0; i < kind.length(); i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
