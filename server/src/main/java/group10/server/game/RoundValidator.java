package group10.server.game;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Pure logic to validate a submission against a round payload and order.
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

        // Produce canonical sorted list
        List<String> canonical = new ArrayList<>(list);

        if ("ASC".equals(order)) {
            canonical.sort(String::compareTo);
        } else {
            canonical.sort(Comparator.reverseOrder());
        }

        // Normalize submission
        List<String> sub = new ArrayList<>();
        submission.forEach(n -> sub.add(n.asText()));

        boolean correct = canonical.equals(sub);
        return new ValidationResult(correct, canonical);
    }
}
