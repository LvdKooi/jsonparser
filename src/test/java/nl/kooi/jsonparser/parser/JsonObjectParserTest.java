package nl.kooi.jsonparser.parser;

import nl.kooi.jsonparser.json.JsonNode;
import nl.kooi.jsonparser.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


class JsonObjectParserTest {

    @Test
    void onlyBraces() {
        assertThat(JsonObjectParser.parse("{}"))
                .isNotNull()
                .isEqualTo(new JsonObject(null));
    }

    @Test
    void oneStringField() {
        var result = JsonObjectParser.parse("""
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
        var result = JsonObjectParser.parse("""
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
        var result = JsonObjectParser.parse("""
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
        var result = JsonObjectParser.parse("""
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
        var result = JsonObjectParser.parse("""
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
    void fourFieldsOfDifferentTypes() {
        var result = JsonObjectParser.parse("""
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
        var result = JsonObjectParser.parse("""
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
        var result = JsonObjectParser.parse("""
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

    @Test
    void jsonWith1NestedObjectContaining1NestedObject() {
        var result = JsonObjectParser.parse("""
                {
                  "person1": {
                    "name": "Laurens",
                    "age": 36,
                    "person2": {
                        "name": "Andreas",
                        "age": 32
                    }
                  }
                }""");

        assertThat(result).isNotNull();
        assertThat(result.jsonNodes().length).isEqualTo(1);
        assertThat(result.jsonNodes()[0].identifier()).isEqualTo("person1");
        assertThat(result.jsonNodes()[0].content()).isInstanceOf(JsonObject.class);

        var nestedObject1 = (JsonObject) result.jsonNodes()[0].content();
        assertThat(nestedObject1.jsonNodes().length).isEqualTo(3);
        assertThat(nestedObject1.jsonNodes()[0].identifier()).isEqualTo("name");
        assertThat(nestedObject1.jsonNodes()[0].content()).isEqualTo("Laurens");
        assertThat(nestedObject1.jsonNodes()[1].identifier()).isEqualTo("age");
        assertThat(nestedObject1.jsonNodes()[1].content()).isEqualTo(36);
        assertThat(nestedObject1.jsonNodes()[2].identifier()).isEqualTo("person2");

        var nestedObject2 = (JsonObject) nestedObject1.jsonNodes()[2].content();
        assertThat(nestedObject2.jsonNodes()[0].identifier()).isEqualTo("name");
        assertThat(nestedObject2.jsonNodes()[0].content()).isEqualTo("Andreas");
        assertThat(nestedObject2.jsonNodes()[1].identifier()).isEqualTo("age");
        assertThat(nestedObject2.jsonNodes()[1].content()).isEqualTo(32);
    }

    @Test
    void anArrayFieldWithEmptyArray() {
        var result = JsonObjectParser.parse("""
                {
                  "children": []
                }""");

        assertThat(result).isNotNull();
        assertThat(result.jsonNodes().length).isEqualTo(1);
        assertThat(result.jsonNodes()[0].identifier()).isEqualTo("children");
        assertThat(result.jsonNodes()[0].content()).isEqualTo(Collections.emptyList());
    }

    @Test
    void anArrayFieldWithObjectArray() {
        var result = JsonObjectParser.parse("""
                {
                  "children": [       {
                  "name": "Laurens",
                  "sign": "Taurus"
                },   
                {
                  "name": "Andreas",
                  "sign": "Scorpius"
                }]
                }""");

        assertThat(result).isNotNull();
        assertThat(result.jsonNodes().length).isEqualTo(1);
        assertThat(result.jsonNodes()[0].identifier()).isEqualTo("children");
        assertThat(result.jsonNodes()[0].content()).isInstanceOf(List.class);
        assertThat(((JsonObject) ((List) result.jsonNodes()[0].content()).get(0)).jsonNodes()).hasSize(2);
        assertThat(((JsonObject) ((List) result.jsonNodes()[0].content()).get(0)).jsonNodes()).containsAll(List.of(new JsonNode("name", "Laurens"), new JsonNode("sign", "Taurus")));
        assertThat(((JsonObject) ((List) result.jsonNodes()[0].content()).get(1)).jsonNodes()).hasSize(2);
        assertThat(((JsonObject) ((List) result.jsonNodes()[0].content()).get(1)).jsonNodes()).containsAll(List.of(new JsonNode("name", "Andreas"), new JsonNode("sign", "Scorpius")));
    }

    @Test
    void anArrayFieldWithAMixedArray() {
        var result = JsonObjectParser.parse("""
                {
                  "children": [1, true, "hello", -2, -3.86, false, "world",   
                {
                  "name": "Andreas",
                  "sign": "Scorpius"
                }]
                }""");

        assertThat(result).isNotNull();
        assertThat(result.jsonNodes().length).isEqualTo(1);
        assertThat(result.jsonNodes()[0].identifier()).isEqualTo("children");
        assertThat(result.jsonNodes()[0].content()).isInstanceOf(ArrayList.class);

        var content = (ArrayList) result.jsonNodes()[0].content();

        assertThat(content).hasSize(8);
        assertThat(content.get(0)).isEqualTo(1);
        assertThat(content.get(1)).isEqualTo(true);
        assertThat(content.get(2)).isEqualTo("hello");
        assertThat(content.get(3)).isEqualTo(-2);
        assertThat(content.get(4)).isEqualTo(-3.86);
        assertThat(content.get(5)).isEqualTo(false);
        assertThat(content.get(6)).isEqualTo("world");
        assertThat(content.get(7)).isInstanceOf(JsonObject.class);

        var jsonObject = (JsonObject) content.get(7);

        assertThat(jsonObject.jsonNodes()).hasSize(2);
        assertThat(jsonObject.jsonNodes()[0].identifier()).isEqualTo("name");
        assertThat(jsonObject.jsonNodes()[0].content()).isEqualTo("Andreas");
        assertThat(jsonObject.jsonNodes()[1].identifier()).isEqualTo("sign");
        assertThat(jsonObject.jsonNodes()[1].content()).isEqualTo("Scorpius");
    }

    @Test
    void anArrayFieldWithEmptyArrays() {
        var result = JsonObjectParser.parse("""
                {
                  "children": [[],[],[]]
                }""");

        assertThat(result).isNotNull();
        assertThat(result.jsonNodes().length).isEqualTo(1);
        assertThat(result.jsonNodes()[0].identifier()).isEqualTo("children");
        assertThat(result.jsonNodes()[0].content()).isEqualTo(
                List.of(
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList()));
    }

    @Test
    void aStringArrayField() {
        var result = JsonObjectParser.parse("""
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
        var result = JsonObjectParser.parse("""
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
        var result = JsonObjectParser.parse("""
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
        var result = JsonObjectParser.parse("""
                {
                  "bools": [true, false, true]
                }""");

        assertThat(result).isNotNull();
        assertThat(result.jsonNodes().length).isEqualTo(1);
        assertThat(result.jsonNodes()[0].identifier()).isEqualTo("bools");
        assertThat(result.jsonNodes()[0].content()).isEqualTo(List.of(true, false, true));
    }
}