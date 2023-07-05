package nl.kooi.jsonparser.parser;

import nl.kooi.jsonparser.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;


class JsonParserTest {

    @Test
    void onlyBraces() {
        assertThat(JsonParser.parse("{}"))
                .isNotNull()
                .isEqualTo(new JsonObject(null));
    }
}