package ee.calculator.delivery_fee.exception;

public class FeeRuleConflictException extends RuntimeException {
    public FeeRuleConflictException(String message) {
        super(message);
    }
}
