package ee.calculator.delivery_fee.dto;

import java.math.BigDecimal;

/**
 * Contains the calculated delivery fee in euros.
 * @param totalFee the total delivery fee in euros
 */
public record DeliveryFeeResponse(BigDecimal totalFee) {
}
