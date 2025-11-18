package group10.server.game;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import group10.common.util.JsonUtil;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class RoundGenerator {
    private final Random rng;
    private final List<String> dictionaryWords = new ArrayList<>();
    private final List<String> dictionaryNumbers = new ArrayList<>();

    public RoundGenerator() {
        this(new Random());
    }

    public RoundGenerator(long seed) {
        this(new Random(seed));
    }

    public RoundGenerator(Random rng) {
        this.rng = rng;
        loadWordsFromResources();
    }

    private void loadWordsFromResources() {
        InputStream is = getClass().getResourceAsStream("/words.txt");
        if (is == null) {
            System.out.println("No words.txt");
            return;
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.strip();
                if (line.isEmpty()) continue;

                dictionaryWords.add(line);
                if (line.matches("^-?\\d+$")) {
                    dictionaryNumbers.add(line);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private String randomOrder() {
        return rng.nextBoolean() ? "ASC" : "DESC";
    }

    // TEXT ROUND

    public ObjectNode generateTextRound(int count) {
        List<String> list = new ArrayList<>(count);

        if (!dictionaryWords.isEmpty()) {
            for (int i = 0; i < count; i++) {
                list.add(dictionaryWords.get(rng.nextInt(dictionaryWords.size())));
            }
        } else {
            // fallback: random fake words
            for (int i = 0; i < count; i++) {
                int len = 1 + rng.nextInt(12);
                StringBuilder sb = new StringBuilder(len);
                for (int j = 0; j < len; j++) {
                    char c = (char) ('a' + rng.nextInt(26));
                    if (rng.nextBoolean()) c = Character.toUpperCase(c);
                    sb.append(c);
                }
                list.add(sb.toString());
            }
        }

        Collections.shuffle(list, rng);

        ArrayNode arr = JsonUtil.MAPPER.createArrayNode();
        list.forEach(arr::add);

        ObjectNode out = JsonUtil.MAPPER.createObjectNode();
        out.set("items", arr);
        out.put("order", randomOrder());

        return out;
    }

    // NUMBER ROUND

    public ObjectNode generateNumberRound(int count, int min, int max) {
        List<String> list = new ArrayList<>(count);

        if (!dictionaryNumbers.isEmpty()) {
            for (int i = 0; i < count; i++) {
                list.add(dictionaryNumbers.get(rng.nextInt(dictionaryNumbers.size())));
            }
        } else {
            if (min > max) {
                int t = min;
                min = max;
                max = t;
            }
            int range = max - min + 1;
            for (int i = 0; i < count; i++) {
                int v = min + rng.nextInt(range);
                list.add(String.valueOf(v));
            }
        }

        Collections.shuffle(list, rng);

        ArrayNode arr = JsonUtil.MAPPER.createArrayNode();
        list.forEach(arr::add);

        ObjectNode out = JsonUtil.MAPPER.createObjectNode();
        out.set("items", arr);
        out.put("order", randomOrder());

        return out;
    }

    // CENTRAL RANDOM ROUND

    public ObjectNode generateRandomRound(int count) {
        // 0 = text, 1 = number
        int type = rng.nextInt(2);

        if (type == 0) {
            return generateTextRound(count);
        } else {
            int min = -100 + rng.nextInt(200);
            int max = min + 10 + rng.nextInt(400);
            return generateNumberRound(count, min, max);
        }
    }
}
