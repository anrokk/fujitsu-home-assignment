package ee.calculator.delivery_fee.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class FeeRuleWriteLock {

    private final JdbcTemplate jdbcTemplate;

    /** Serializes pricing changes until the caller's transaction commits or rolls back. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void acquire() {
        jdbcTemplate.queryForObject(
                "SELECT id FROM fee_rule_write_lock WHERE id = 1 FOR UPDATE", Integer.class);
    }
}
