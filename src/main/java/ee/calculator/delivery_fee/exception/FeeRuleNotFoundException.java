package ee.calculator.delivery_fee.exception;

public class FeeRuleNotFoundException extends RuntimeException {
    public FeeRuleNotFoundException(Long id) {
        super("Fee rule not found: " + id);
    }
}
