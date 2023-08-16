package nl.kooi.jsonparser.parser;

import nl.kooi.jsonparser.json.JsonNode;
import nl.kooi.jsonparser.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class JsonArrayParserTest {
    @Test
    void anEmptyArray() {
        var result = JsonArrayParser.parse("""
                 []
                """);

        assertThat(result).isNotNull().isEqualTo(Collections.emptyList());
    }

    @Test
    void anObjectArray() {
        var result = JsonArrayParser.parse("""
                [{
                  "name": "Laurens",
                  "sign": "Taurus"
                },   
                {
                  "name": "Andreas",
                  "sign": "Scorpius"
                }]
                """);

        assertThat(result).isNotNull().hasSize(2);
        assertThat(((JsonObject) result.get(0)).jsonNodes()).containsAll(List.of(new JsonNode("name", "Laurens"), new JsonNode("sign", "Taurus")));
        assertThat(((JsonObject) result.get(1)).jsonNodes()).containsAll(List.of(new JsonNode("name", "Andreas"), new JsonNode("sign", "Scorpius")));
    }

    @Test
    void aMixedArray() {
        var result = JsonArrayParser.parse("""
                [1, true, "hello", -2, -3.86, false, "world", null,   
                  {
                    "name": "Andreas",
                    "sign": "Scorpius"
                  }]
                  """);


        assertThat(result).hasSize(9);
        assertThat(result.get(0)).isEqualTo(1);
        assertThat(result.get(1)).isEqualTo(true);
        assertThat(result.get(2)).isEqualTo("hello");
        assertThat(result.get(3)).isEqualTo(-2);
        assertThat(result.get(4)).isEqualTo(-3.86);
        assertThat(result.get(5)).isEqualTo(false);
        assertThat(result.get(6)).isEqualTo("world");
        assertThat(result.get(7)).isNull();
        assertThat(result.get(8)).isInstanceOf(JsonObject.class);

        var jsonObject = (JsonObject) result.get(8);

        assertThat(jsonObject.jsonNodes()).hasSize(2);
        assertThat(jsonObject.jsonNodes()[0].identifier()).isEqualTo("name");
        assertThat(jsonObject.jsonNodes()[0].content()).isEqualTo("Andreas");
        assertThat(jsonObject.jsonNodes()[1].identifier()).isEqualTo("sign");
        assertThat(jsonObject.jsonNodes()[1].content()).isEqualTo("Scorpius");
    }

    @Test
    void anArrayOfEmptyArrays() {
        var result = JsonArrayParser.parse("""
                [[],[],[]]
                        """);

        assertThat(result).isNotNull().hasSize(3).isEqualTo(
                List.of(
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList()));
    }

    @Test
    void aStringArray() {
        var result = JsonArrayParser.parse("""
                      ["Anthony", "Marvin"]
                """);

        assertThat(result).isNotNull().hasSize(2).isEqualTo(List.of("Anthony", "Marvin"));
    }

    @Test
    void aNumberArrayWithIntegers() {
        var result = JsonArrayParser.parse("""
                [12, 9]
                """);

        assertThat(result).isNotNull().hasSize(2).isEqualTo(List.of(12, 9));
    }

    @Test
    void anArrayWithNulls() {
        var result = JsonArrayParser.parse("""
                [null, null]
                """);

        assertThat(result).isNotNull().hasSize(2).isEqualTo(Arrays.asList(null, null));
    }

    @Test
    void aNumberArrayWithDoubles() {
        var result = JsonArrayParser.parse("""
                [12.99, 9.87]
                      """);

        assertThat(result).isNotNull().hasSize(2).isEqualTo(List.of(12.99, 9.87));
    }

    @Test
    void aBooleanArray() {
        var result = JsonArrayParser.parse("""
                [true, false, true]
                """);

        assertThat(result).isNotNull().hasSize(3).isEqualTo(List.of(true, false, true));
    }
}
