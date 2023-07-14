package nl.kooi.jsonparser.parser;

import nl.kooi.jsonparser.json.JsonObject;
import org.junit.jupiter.api.Disabled;
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
        var result = JsonParser.parse("""
                {
                  "name": "Laurens"
                }""");

        assertThat(result).isNotNull();
        assertThat(result.jsonNodes().length).isEqualTo(1);
        assertThat(result.jsonNodes()[0].identifier()).isEqualTo("name");
        assertThat(result.jsonNodes()[0].content()).isEqualTo("Laurens");
    }

    @Test
    void oneStringFieldWithTokenCharacters() {
        var result = JsonParser.parse("""
                {
                   "name": "{[,true false:]}"
                }""");

        assertThat(result).isNotNull();
        assertThat(result.jsonNodes().length).isEqualTo(1);
        assertThat(result.jsonNodes()[0].identifier()).isEqualTo("name");
        assertThat(result.jsonNodes()[0].content()).isEqualTo("{[,true false:]}");
    }

    @Test
    void oneNumberField() {
        var result = JsonParser.parse("""
                {
                  "weight": 79.85
                }""");

        assertThat(result).isNotNull();
        assertThat(result.jsonNodes().length).isEqualTo(1);
        assertThat(result.jsonNodes()[0].identifier()).isEqualTo("weight");
        assertThat(result.jsonNodes()[0].content()).isEqualTo(79.85);
    }

    @Test
    void oneBooleanField() {
        var result = JsonParser.parse("""
                {
                  "married": false
                }""");

        assertThat(result).isNotNull();
        assertThat(result.jsonNodes().length).isEqualTo(1);
        assertThat(result.jsonNodes()[0].identifier()).isEqualTo("married");
        assertThat(result.jsonNodes()[0].content()).isEqualTo(false);
    }

    @Test
    void twoStringFields() {
        var result = JsonParser.parse("""
                {
                  "name": "Laurens",
                  "sign": "Taurus"
                }""");

        assertThat(result).isNotNull();
        assertThat(result.jsonNodes().length).isEqualTo(2);
        assertThat(result.jsonNodes()[0].identifier()).isEqualTo("name");
        assertThat(result.jsonNodes()[0].content()).isEqualTo("Laurens");
        assertThat(result.jsonNodes()[1].identifier()).isEqualTo("sign");
        assertThat(result.jsonNodes()[1].content()).isEqualTo("Taurus");
    }

    @Test
    void anArrayFieldWithEmptyArray() {
        var result = JsonParser.parse("""
                {
                  "children": []
                }""");

        assertThat(result).isNotNull();
        assertThat(result.jsonNodes().length).isEqualTo(1);
        assertThat(result.jsonNodes()[0].identifier()).isEqualTo("children");
        assertThat(result.jsonNodes()[0].content()).isEqualTo(Collections.emptyList());
    }

    @Test
    void anArrayFieldWithAMixedArray() {
        var result = JsonParser.parse("""
                {
                  "children": [1, true, "hello", -2, -3.86, false, "world"]
                }""");

        assertThat(result).isNotNull();
        assertThat(result.jsonNodes().length).isEqualTo(1);
        assertThat(result.jsonNodes()[0].identifier()).isEqualTo("children");
        assertThat(result.jsonNodes()[0].content()).isEqualTo(List.of(1, true, "hello", -2, -3.86, false, "world"));
    }


    @Test
    @Disabled
    void anArrayFieldWithEmptyArrays() {
        var result = JsonParser.parse("""
                {
                  "children": [[],[],[]]
                }""");

        assertThat(result).isNotNull();
        assertThat(result.jsonNodes().length).isEqualTo(1);
        assertThat(result.jsonNodes()[0].identifier()).isEqualTo("children");
        assertThat(result.jsonNodes()[0].content()).isEqualTo(Collections.emptyList());
    }

    @Test
    void aStringArrayField() {
        var result = JsonParser.parse("""
                {
                  "children": ["Anthony", "Marvin"]
                }""");

        assertThat(result).isNotNull();
        assertThat(result.jsonNodes().length).isEqualTo(1);
        assertThat(result.jsonNodes()[0].identifier()).isEqualTo("children");
        assertThat(result.jsonNodes()[0].content()).isEqualTo(List.of("Anthony", "Marvin"));
    }

    @Test
    void aNumberArrayFieldOfIntegers() {
        var result = JsonParser.parse("""
                {
                  "ages": [12, 9]
                }""");


        assertThat(result).isNotNull();
        assertThat(result.jsonNodes().length).isEqualTo(1);
        assertThat(result.jsonNodes()[0].identifier()).isEqualTo("ages");
        assertThat(result.jsonNodes()[0].content()).isEqualTo(List.of(12, 9));
    }

    @Test
    void aNumberArrayFieldOfDoubles() {
        var result = JsonParser.parse("""
                {
                  "weights": [12.99, 9.87]
                }""");

        assertThat(result).isNotNull();
        assertThat(result.jsonNodes().length).isEqualTo(1);
        assertThat(result.jsonNodes()[0].identifier()).isEqualTo("weights");
        assertThat(result.jsonNodes()[0].content()).isEqualTo(List.of(12.99, 9.87));
    }

    @Test
    void aBooleanArrayField() {
        var result = JsonParser.parse("""
                {
                  "bools": [true, false, true]
                }""");


        assertThat(result).isNotNull();
        assertThat(result.jsonNodes().length).isEqualTo(1);
        assertThat(result.jsonNodes()[0].identifier()).isEqualTo("bools");
        assertThat(result.jsonNodes()[0].content()).isEqualTo(List.of(true, false, true));
    }

    @Test
    void fourFieldsOfDifferentTypes() {
        var result = JsonParser.parse("""
                {
                  "name": "Laurens",
                  "age": 36,
                  "children": ["Anthony", "Marvin"],
                  "married": true
                }""");
        assertThat(result).isNotNull();
        assertThat(result.jsonNodes().length).isEqualTo(4);
        assertThat(result.jsonNodes()[0].identifier()).isEqualTo("name");
        assertThat(result.jsonNodes()[0].content()).isEqualTo("Laurens");
        assertThat(result.jsonNodes()[1].identifier()).isEqualTo("age");
        assertThat(result.jsonNodes()[1].content()).isEqualTo(36);
        assertThat(result.jsonNodes()[2].identifier()).isEqualTo("children");
        assertThat(result.jsonNodes()[2].content()).isEqualTo(List.of("Anthony", "Marvin"));
        assertThat(result.jsonNodes()[3].identifier()).isEqualTo("married");
        assertThat(result.jsonNodes()[3].content()).isEqualTo(true);
    }

    @Test
    void simpleJsonWith1NestedObject() {
        var result = JsonParser.parse("""
                {
                  "person": {
                    "name": "Laurens",
                    "age": 36
                  }
                }""");

        assertThat(result).isNotNull();
        assertThat(result.jsonNodes().length).isEqualTo(1);
        assertThat(result.jsonNodes()[0].identifier()).isEqualTo("person");
        assertThat(result.jsonNodes()[0].content()).isInstanceOf(JsonObject.class);

        var nestedObject = (JsonObject) result.jsonNodes()[0].content();
        assertThat(nestedObject.jsonNodes()[0].identifier()).isEqualTo("name");
        assertThat(nestedObject.jsonNodes()[0].content()).isEqualTo("Laurens");
        assertThat(nestedObject.jsonNodes()[1].identifier()).isEqualTo("age");
        assertThat(nestedObject.jsonNodes()[1].content()).isEqualTo(36);
    }

    @Test
    void simpleJsonWith2NestedObject() {
        var result = JsonParser.parse("""
                {
                  "person1": {
                    "name": "Laurens",
                    "age": 36
                  },
                  "person2": {
                    "name": "Andreas",
                    "age": 32
                  }
                }""");

        assertThat(result).isNotNull();
        assertThat(result.jsonNodes().length).isEqualTo(2);
        assertThat(result.jsonNodes()[0].identifier()).isEqualTo("person1");
        assertThat(result.jsonNodes()[0].content()).isInstanceOf(JsonObject.class);
        assertThat(result.jsonNodes()[1].identifier()).isEqualTo("person2");
        assertThat(result.jsonNodes()[1].content()).isInstanceOf(JsonObject.class);

        var nestedObject1 = (JsonObject) result.jsonNodes()[0].content();
        assertThat(nestedObject1.jsonNodes()[0].identifier()).isEqualTo("name");
        assertThat(nestedObject1.jsonNodes()[0].content()).isEqualTo("Laurens");
        assertThat(nestedObject1.jsonNodes()[1].identifier()).isEqualTo("age");
        assertThat(nestedObject1.jsonNodes()[1].content()).isEqualTo(36);
        var nestedObject2 = (JsonObject) result.jsonNodes()[1].content();
        assertThat(nestedObject2.jsonNodes()[0].identifier()).isEqualTo("name");
        assertThat(nestedObject2.jsonNodes()[0].content()).isEqualTo("Andreas");
        assertThat(nestedObject2.jsonNodes()[1].identifier()).isEqualTo("age");
        assertThat(nestedObject2.jsonNodes()[1].content()).isEqualTo(32);
    }
}