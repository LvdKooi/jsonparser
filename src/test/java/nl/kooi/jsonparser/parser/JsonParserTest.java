package nl.kooi.jsonparser.parser;

import nl.kooi.jsonparser.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


class JsonParserTest {

    @Test
    void onlyBraces() {
        assertThat(JsonParser.parse("{}"))
                .isNotNull()
                .isEqualTo(new JsonObject(null));
    }

    @Test
    void oneStringField() {
        var result = JsonParser.parse("{\"name\": \"Laurens\"}");

        assertThat(result).isNotNull();
        assertThat(result.jsonNodes().length).isEqualTo(1);
        assertThat(result.jsonNodes()[0].identifier()).isEqualTo("name");
        assertThat(result.jsonNodes()[0].content()).isEqualTo("Laurens");
    }

    @Test
    void oneStringFieldWithTokenCharacters() {
        var result = JsonParser.parse("{\"name\": \"{[,true false:]}\"}");

        assertThat(result).isNotNull();
        assertThat(result.jsonNodes().length).isEqualTo(1);
        assertThat(result.jsonNodes()[0].identifier()).isEqualTo("name");
        assertThat(result.jsonNodes()[0].content()).isEqualTo("{[,true false:]}");
    }

    @Test
    void oneNumberField() {
        var result = JsonParser.parse("{\"weight\": 79.85}");

        assertThat(result).isNotNull();
        assertThat(result.jsonNodes().length).isEqualTo(1);
        assertThat(result.jsonNodes()[0].identifier()).isEqualTo("weight");
        assertThat(result.jsonNodes()[0].content()).isEqualTo(79.85);
    }

    @Test
    void oneBooleanField() {
        var result = JsonParser.parse("{\"married\": false}");

        assertThat(result).isNotNull();
        assertThat(result.jsonNodes().length).isEqualTo(1);
        assertThat(result.jsonNodes()[0].identifier()).isEqualTo("married");
        assertThat(result.jsonNodes()[0].content()).isEqualTo(false);
    }


    @Test
    void twoStringFields() {
        var result = JsonParser.parse("{\"name\": \"Laurens\",\"sign\": \"Taurus\"}");

        assertThat(result).isNotNull();
        assertThat(result.jsonNodes().length).isEqualTo(2);
        assertThat(result.jsonNodes()[0].identifier()).isEqualTo("name");
        assertThat(result.jsonNodes()[0].content()).isEqualTo("Laurens");
        assertThat(result.jsonNodes()[1].identifier()).isEqualTo("sign");
        assertThat(result.jsonNodes()[1].content()).isEqualTo("Taurus");
    }


    @Test
    void anArrayFieldWithEmptyArray() {
        var result = JsonParser.parse("{\"children\": []}");

        assertThat(result).isNotNull();
        assertThat(result.jsonNodes().length).isEqualTo(1);
        assertThat(result.jsonNodes()[0].identifier()).isEqualTo("children");
        assertThat(result.jsonNodes()[0].content()).isEqualTo(Collections.emptyList());
    }

    @Test
    void anArrayFieldWithEmptyArrays() {
        var result = JsonParser.parse("{\"children\": [[],[],[],[]]}");

        assertThat(result).isNotNull();
        assertThat(result.jsonNodes().length).isEqualTo(1);
        assertThat(result.jsonNodes()[0].identifier()).isEqualTo("children");
        assertThat(result.jsonNodes()[0].content()).isEqualTo(Collections.emptyList());
    }

    @Test
    void aStringArrayField() {
        var result = JsonParser.parse("{\"children\": [\"Anthony\", \"Marvin\"]}");

        assertThat(result).isNotNull();
        assertThat(result.jsonNodes().length).isEqualTo(1);
        assertThat(result.jsonNodes()[0].identifier()).isEqualTo("children");
        assertThat(result.jsonNodes()[0].content()).isEqualTo(List.of("Anthony", "Marvin"));
    }

    @Test
    void aNumberArrayField() {
        var result = JsonParser.parse("{\"ages\": [12, 9]}");

        assertThat(result).isNotNull();
        assertThat(result.jsonNodes().length).isEqualTo(1);
        assertThat(result.jsonNodes()[0].identifier()).isEqualTo("ages");
        assertThat(result.jsonNodes()[0].content()).isEqualTo(new Integer[]{12, 9});
    }

    @Test
    void aBooleanArrayField() {
        var result = JsonParser.parse("{\"bools\": [true, false, true]}");

        assertThat(result).isNotNull();
        assertThat(result.jsonNodes().length).isEqualTo(1);
        assertThat(result.jsonNodes()[0].identifier()).isEqualTo("bools");
        assertThat(result.jsonNodes()[0].content()).isEqualTo(new Boolean[]{true, false, true});
    }

    @Test
    void threeFieldsOfDifferentTypes() {
        var result = JsonParser.parse("{\"name\": \"Laurens\",\"age\": 36, \"married\": true}");

        assertThat(result).isNotNull();
        assertThat(result.jsonNodes().length).isEqualTo(3);
        assertThat(result.jsonNodes()[0].identifier()).isEqualTo("name");
        assertThat(result.jsonNodes()[0].content()).isEqualTo("Laurens");
        assertThat(result.jsonNodes()[1].identifier()).isEqualTo("age");
        assertThat(result.jsonNodes()[1].content()).isEqualTo(36);
        assertThat(result.jsonNodes()[2].identifier()).isEqualTo("married");
        assertThat(result.jsonNodes()[2].content()).isEqualTo(true);
    }

}