package group10.common.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public final class JsonUtil {
    public static final ObjectMapper MAPPER = create();
    private static ObjectMapper create() {
        ObjectMapper m = new ObjectMapper();
        m.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        m.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return m;
    }

    private JsonUtil() {}
}
