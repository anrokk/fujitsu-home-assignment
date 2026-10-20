package ee.calculator.delivery_fee;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Bean;

import java.time.Clock;

@SpringBootApplication
@EnableScheduling
public class DeliveryFeeApplication {

	/**
	 * Starts the delivery fee application.
	 */
	public static void main(String[] args) {
		SpringApplication.run(DeliveryFeeApplication.class, args);
	}

	/**
	 * Provides the clock used when a calculation datetime is omitted. This is used to make the application testable.
	 * @return the system clock in UTC
	 */
	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}

}
