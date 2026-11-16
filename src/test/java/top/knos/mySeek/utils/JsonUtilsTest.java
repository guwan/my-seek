package top.knos.mySeek.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonUtilsTest {

    record Sample(String name, int age) {
    }

    @Test
    @DisplayName("Object to JSON and back")
    void objectRoundTrip() {
        String json = JsonUtils.objectToJson(new Sample("knos", 18));

        assertTrue(json.contains("\"name\""));

        Sample back = JsonUtils.jsonToPojo(json, Sample.class);
        assertEquals("knos", back.name());
        assertEquals(18, back.age());
    }

    @Test
    @DisplayName("JSON array to list")
    void jsonToList() {
        List<Sample> list = JsonUtils.jsonToList("[{\"name\":\"a\",\"age\":1}]", Sample.class);

        assertEquals(1, list.size());
        assertEquals("a", list.getFirst().name());
    }

    @Test
    @DisplayName("Invalid JSON returns null instead of throwing")
    void invalidJsonReturnsNull() {
        assertNull(JsonUtils.jsonToPojo("{not-json", Sample.class));
    }

    @Test
    @DisplayName("String is parsed to JsonNode")
    void stringToJsonNode() {
        JsonNode node = JsonUtils.strToJson("{\"a\":1}");

        assertEquals(1, node.get("a").asInt());
    }
}
