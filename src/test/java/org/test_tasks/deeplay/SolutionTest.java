package org.test_tasks.deeplay;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Handler;

import static org.junit.jupiter.api.Assertions.*;

public class SolutionTest {

    private int search(int ind, int curDist, char[] cells, Map<Character, Integer> costs, Set<Integer> visited, int length) {
        if (ind == cells.length - 1) {
            return curDist;
        }
        int[] result = new int[]{Integer.MAX_VALUE};
        Consumer<Integer> calculate = (nextInd) -> {
            if (!visited.contains(nextInd)) {
                visited.add(nextInd);
                int nextCost = costs.get(cells[nextInd]);
                result[0] = Math.min(result[0], search(nextInd, curDist + nextCost, cells, costs, visited, length));
                visited.remove(nextInd);
            }
        };
        // Go left
        if (ind % length != 0) {
            calculate.accept(ind - 1);
        }
        // Go up
        if (ind >= length) {
            calculate.accept(ind - length);
        }
        // Go right
        if ((ind + 1) % length != 0) {
            calculate.accept(ind + 1);
        }
        // Go down
        if (ind + length < cells.length) {
            calculate.accept(ind + length);
        }
        return result[0];
    }

    private int exhaustiveSearch(String field, String species) {
        Set<Integer> visited = new HashSet<>();
        visited.add(0);
        List<Integer> prices;
        if (species.equals("Human")) {
            prices = List.of(5, 2, 3, 1);
        } else if (species.equals("Swamper")) {
            prices = List.of(2, 2, 5, 2);
        } else {
            assert species.equals("Woodman");
            prices = List.of(3, 3, 2, 2);
        }
        Map<Character, Integer> costs = new HashMap<>();
        costs.put('S', prices.get(0));
        costs.put('W', prices.get(1));
        costs.put('T', prices.get(2));
        costs.put('P', prices.get(3));
        return search(0, 0, field.toCharArray(), costs, visited, (int) Math.sqrt(field.length()));
    }

    private void testWithSize(int size) throws SolutionException {
        int n = 1000;
        char[] alphabet = new char[]{'S', 'W', 'T', 'P'};
        String[] species = new String[]{"Human", "Swamper", "Woodman"};
        Random rand = new Random();
        for (int i = 0; i < n; ++i) {
            StringBuilder stringBuilder = new StringBuilder();
            for (int j = 0; j < size; ++j) {
                stringBuilder.append(alphabet[rand.nextInt(4)]);
            }
            String field = stringBuilder.toString();
            String spec = species[rand.nextInt(3)];
            assertEquals(exhaustiveSearch(field, spec), Solution.getResult(field, spec));
        }
    }

    @Test
    public void testSimple() throws SolutionException {
        // One simple test
        String field = "PTWP"
                + "TTPP"
                + "WPTW"
                + "TSPP";
        assertEquals(10, Solution.getResult(field, "Human"));

        // Random tests
        testWithSize(16);
    }

    @Test
    public void testAStar() throws SolutionException {
        // Simple test
        String field = "PSSSS"
                + "PSSSS"
                + "PSPPP"
                + "PPPSP"
                + "SSSSP";
        assertEquals(10, Solution.getResult(field, "Human"));

        // Random tests
        testWithSize(25);
//        testWithSize(36);     // Long test.
    }

    @Test
    public void testExceptions() {
        SolutionException e = assertThrows(SolutionException.class, () -> Solution.getResult("PTWPTTPPWPTWTSP", "Human"));
        assertEquals("Field must be square, current field length: 15", e.getMessage());
        e = assertThrows(SolutionException.class, () -> Solution.getResult("PTWPTTPPXPTWTSPP", "Human"));
        assertEquals("Field contains incorrect symbols", e.getMessage());
        e = assertThrows(SolutionException.class, () -> Solution.getResult("PTWPTTPPWPTWTSPP", "Somebody"));
        assertEquals("Incorrect species", e.getMessage());
    }
}
