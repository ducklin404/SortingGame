package group10.server.game;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import group10.common.util.JsonUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Returns a JSON ObjectNode with fields:
 *  - "items": array of strings
 *  - "order": "ASC" | "DESC"
 */
public class RoundGenerator {
    private final Random rng;

    public RoundGenerator() { this(new Random()); }
    public RoundGenerator(long seed) { this(new Random(seed)); }
    public RoundGenerator(Random rng) { this.rng = rng; }

    // Generate a round containing single uppercase letters
    public ObjectNode generateLetterRound(int count) {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            char c = (char) ('A' + rng.nextInt(26));
            list.add(String.valueOf(c));
        }
        Collections.shuffle(list, rng);
        ArrayNode arr = JsonUtil.MAPPER.createArrayNode();
        for (String s : list) arr.add(s);
        ObjectNode out = JsonUtil.MAPPER.createObjectNode();
        out.set("items", arr);
        out.put("order", randomOrder());
        return out;
    }

    private String randomOrder() {
        int r = rng.nextInt(2);
        if (r == 0) {
            return "ASC";
        }
        else return "DESC";
    }
}
