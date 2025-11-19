package group10.server.game;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * logic to validate a submission against a round payload and order.
 * Returns ValidationResult (correct, normalized canonical array).
 */
public class RoundValidator {

    public record ValidationResult(boolean correct, List<String> canonical) {
    }

    /**
     * payload: ObjectNode with "items" (array) and "order" ("ASC" / "DESC")
     * submission: ArrayNode with attempted ordering
     */
    public ValidationResult validate(ObjectNode payload, ArrayNode submission) {
        ArrayNode items = (ArrayNode) payload.get("items");
        String order = payload.get("order").asText();

        // Normalize items to strings
        List<String> list = new ArrayList<>();
        items.forEach(n -> list.add(n.asText()));

        // Detect whether everything is numeric (integers). We'll treat as numeric if all items parse as integers.
        boolean allNumeric = true;
        for (String s : list) {
            if (!s.matches("^-?\\d+$")) {
                allNumeric = false;
                break;
            }
        }

        // Produce canonical sorted list
        List<String> canonical = new ArrayList<>(list);

        if (allNumeric) {
            // numeric sort by integer value
            canonical.sort(Comparator.comparingLong(s -> Long.parseLong(s)));
        } else {
            // TEXT sort: pure lexicographic (dictionary-style)
            canonical.sort((a, b) -> {
                int min = Math.min(a.length(), b.length());
                for (int i = 0; i < min; i++) {
                    char ca = Character.toLowerCase(a.charAt(i));
                    char cb = Character.toLowerCase(b.charAt(i));
                    if (ca != cb) {
                        return Character.compare(ca, cb);
                    }
                }
                // all equal up to min length; shorter wins
                return Integer.compare(a.length(), b.length());
            });

        }

        // If order is DESC, reverse
        if (!"ASC".equals(order)) {
            // DESC branch
            List<String> reversed = new ArrayList<>();
            for (int i = canonical.size() - 1; i >= 0; i--) reversed.add(canonical.get(i));
            canonical = reversed;
        }

        // Normalize submission
        List<String> sub = new ArrayList<>();
        submission.forEach(n -> sub.add(n.asText()));

        boolean correct = canonical.equals(sub);
        return new ValidationResult(correct, canonical);
    }
}
