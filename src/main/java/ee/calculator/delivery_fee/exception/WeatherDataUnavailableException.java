package ee.calculator.delivery_fee.exception;

public class WeatherDataUnavailableException extends RuntimeException {
    public WeatherDataUnavailableException(String message) {
        super(message);
    }
}
