package ee.calculator.delivery_fee.weather;


import ee.calculator.delivery_fee.domain.City;
import ee.calculator.delivery_fee.entity.WeatherObservation;
import org.springframework.stereotype.Component;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.w3c.dom.Element;
import javax.swing.text.html.parser.Parser;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public class WeatherXmlParser {
    /**
     * Extracts observations for Tallinn-Harku, Tartu-Tõravere, and Pärnu.
     * Missing numeric measurements are stored as null.
     *
     * @throws IllegalArgumentException if the XML or a numeric value is invalid
     */
    public List<WeatherObservation> parse(String xml) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

            Element root = dbf.newDocumentBuilder()
                    .parse(new InputSource(new StringReader(xml)))
                    .getDocumentElement();

            Instant observedAt = Instant.ofEpochSecond(
                    Long.parseLong(root.getAttribute("timestamp"))
            );

            NodeList stations = root.getElementsByTagName("station");
            List<WeatherObservation> observations = new ArrayList<>();

            for (int i = 0; i < stations.getLength(); i++) {
                Element station = (Element) stations.item(i);
                String stationName = readText(station, "name");

                City city = switch (stationName) {
                    case "Tallinn-Harku" -> City.TALLINN;
                    case "Tartu-Tõravere" -> City.TARTU;
                    case "Pärnu" -> City.PARNU;
                    default -> null;
                };

                if (city == null) {
                    continue;
                }

                WeatherObservation observation = new WeatherObservation();
                observation.setCity(city);
                observation.setStationName(stationName);
                observation.setWmoCode(readText(station, "wmocode"));
                observation.setAirTemperature(
                        readDouble(station, "airtemperature")
                );
                observation.setWindSpeed(
                        readDouble(station, "windspeed")
                );
                observation.setPhenomenon(readText(station, "phenomenon"));
                observation.setObservedAt(observedAt);

                observations.add(observation);
            }

            return observations;

        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalArgumentException("Could not parse weather XML", e);
        }
    }

    private String readText(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);

        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent().trim();
    }

    private Double readDouble(Element element, String tagName) {
        String value = readText(element, tagName);
        return value.isEmpty() ? null : Double.parseDouble(value);
    }
}
