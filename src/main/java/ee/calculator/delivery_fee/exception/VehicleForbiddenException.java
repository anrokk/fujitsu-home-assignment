package ee.calculator.delivery_fee.exception;

public class VehicleForbiddenException extends RuntimeException {
    public VehicleForbiddenException() {
        super("Usage of selected vehicle type is forbidden");
    }
}
