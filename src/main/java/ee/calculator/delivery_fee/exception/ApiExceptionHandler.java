package ee.calculator.delivery_fee.exception;


import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Reports that weather conditions prohibit the selected vehicle.
     */
    @ExceptionHandler(VehicleForbiddenException.class)
    public ProblemDetail handleVehicleForbidden(
            VehicleForbiddenException exception
    ) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_CONTENT,
                exception.getMessage()
        );
    }

    /**
     * Reports that a base fee for the selected city and vehicle type was not found.
     */
    @ExceptionHandler(RegionalBaseFeeNotFoundException.class)
    public ProblemDetail handleBaseFeeNotFound(
            RegionalBaseFeeNotFoundException exception
    ) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );
    }

    /**
     * Reports that the weather data needed for calculation is unavailable.
     */
    @ExceptionHandler(WeatherDataUnavailableException.class)
    public ProblemDetail handleWeatherDataUnavailable(
            WeatherDataUnavailableException exception
    ) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                exception.getMessage()
        );
    }

    /**
     * Reports that the requested fee-rule version does not exist.
     */
    @ExceptionHandler(FeeRuleNotFoundException.class)
    public ProblemDetail handleFeeRuleNotFound(
            FeeRuleNotFoundException exception) {

        return ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );
    }

    /**
     * Reports a duplicate active rule or an invalid version change.
     */
    @ExceptionHandler(FeeRuleConflictException.class)
    public ProblemDetail handleFeeRuleConflict(
            FeeRuleConflictException exception) {

        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );
    }
}
