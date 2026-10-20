package ee.calculator.delivery_fee.weather;

import ee.calculator.delivery_fee.domain.PhenomenonCategory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class PhenomenonClassifierTest {
    private final PhenomenonClassifier classifier = new PhenomenonClassifier();

    @ParameterizedTest
    @CsvSource({
            "Light snow shower, SNOW",
            "Heavy snowfall, SNOW",
            "Light sleet, SLEET",
            "Moderate sleet, SLEET",
            "Light shower, RAIN",
            "Heavy shower, RAIN",
            "Moderate rain, RAIN",
            "Glaze, GLAZE",
            "Hail, HAIL",
            "Thunder, THUNDER",
            "Thunderstorm with rain, THUNDER",
            "Fog, OTHER",
            "Overcast, OTHER",
            "Unknown condition, OTHER",
            "'  LIGHT SNOW SHOWER  ', SNOW"
    })
    void classifiesDescription(
            String description,
            PhenomenonCategory expectedCategory
    ) {
        assertThat(classifier.classify(description))
                .isEqualTo(expectedCategory);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void classifiesMissingDescriptionAsOther(String description) {
        assertThat(classifier.classify(description))
                .isEqualTo(PhenomenonCategory.OTHER);
    }
}
