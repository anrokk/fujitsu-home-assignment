package ee.calculator.delivery_fee.weather;


import ee.calculator.delivery_fee.domain.PhenomenonCategory;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class PhenomenonClassifier {

    public PhenomenonCategory classify(String phenomenon) {
        if (phenomenon == null) {
            return PhenomenonCategory.OTHER;
        }

        String value = phenomenon.strip().toLowerCase(Locale.ROOT);

        if (value.contains("thunder")) {
            return PhenomenonCategory.THUNDER;
        }

        if (value.contains("hail")) {
            return PhenomenonCategory.HAIL;
        }

        if (value.contains("glaze")) {
            return PhenomenonCategory.GLAZE;
        }

        if (value.contains("snow")) {
            return PhenomenonCategory.SNOW;
        }

        if (value.contains("sleet")) {
            return PhenomenonCategory.SLEET;
        }

        if (value.contains("rain") || value.contains("shower")) {
            return PhenomenonCategory.RAIN;
        }

        return PhenomenonCategory.OTHER;
    }
}
