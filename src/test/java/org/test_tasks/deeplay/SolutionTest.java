package org.test_tasks.deeplay;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;

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

    private int exhaustiveSearch(String field, String race, Map<Character, Integer> costs) {
        if (costs == null) {
            List<Integer> prices;
            if (race.equals("Human")) {
                prices = List.of(5, 2, 3, 1);
            } else if (race.equals("Swamper")) {
                prices = List.of(2, 2, 5, 2);
            } else {
                assert race.equals("Woodman");
                prices = List.of(3, 3, 2, 2);
            }
            costs = new HashMap<>();
            costs.put('S', prices.get(0));
            costs.put('W', prices.get(1));
            costs.put('T', prices.get(2));
            costs.put('P', prices.get(3));
        }
        Set<Integer> visited = new HashSet<>();
        visited.add(0);
        return search(0, 0, field.toCharArray(), costs, visited, (int) Math.sqrt(field.length()));
    }

    private void testRandom(int size, char[] alphabet, String[] races,
                            BiFunction<String, String, Integer> getResult,
                            Map<Character, Integer> costs) {
        int n = 1000;
        Random rand = new Random();
        for (int i = 0; i < n; ++i) {
            StringBuilder stringBuilder = new StringBuilder();
            for (int j = 0; j < size; ++j) {
                stringBuilder.append(alphabet[rand.nextInt(alphabet.length)]);
            }
            String field = stringBuilder.toString();
            String spec = races[rand.nextInt(races.length)];
            assertEquals(exhaustiveSearch(field, spec, costs), getResult.apply(field, spec));
        }
    }

    private int getResult(String field, String race) {
        try {
            return Solution.getResult(field, race);
        } catch (SolutionException e) {
            throw new RuntimeException(e);
        }
    }

    private final char[] DEFAULT_ALPHABET = new char[]{'S', 'W', 'T', 'P'};
    private final String[] DEFAULT_RACES = new String[]{"Human", "Swamper", "Woodman"};

    @Test
    public void testSimple() throws SolutionException {
        // One simple test
        String simpleField = "PTWP"
                + "TTPP"
                + "WPTW"
                + "TSPP";
        assertEquals(10, Solution.getResult(simpleField, "Human"));

        // Random tests
        testRandom(16, DEFAULT_ALPHABET, DEFAULT_RACES, this::getResult, null);
    }

    @Test
    public void testAStar() throws SolutionException {
        // Simple test
        String simpleField = "PSSSS"
                + "PSSSS"
                + "PSPPP"
                + "PPPSP"
                + "SSSSP";
        assertEquals(10, Solution.getResult(simpleField, "Human"));

        // Random tests
        testRandom(25, DEFAULT_ALPHABET, DEFAULT_RACES, this::getResult, null);
        // testWithSize(36, DEFAULT_ALPHABET, DEFAULT_RACES, this::getResult, null);     // Long test.
    }

    @Test
    public void testDefaultExceptions() {
        SolutionException e = assertThrows(SolutionException.class, () -> Solution.getResult("PTWPTTPPWPTWTSP", "Human"));
        assertEquals("Field must be square, current field length: 15", e.getMessage());
        e = assertThrows(SolutionException.class, () -> Solution.getResult("PTWPTTPPXPTWTSPP", "Human"));
        assertEquals("Field contains incorrect symbols", e.getMessage());
        e = assertThrows(SolutionException.class, () -> Solution.getResult("PTWPTTPPWPTWTSPP", "Somebody"));
        assertEquals("Incorrect race", e.getMessage());
    }

    private final String workingDir = "./src/test/resources/";

    @Test
    public void testFileWork() throws SolutionException {
        // Simple test
        String simpleField = "PTWP"
                + "TTPP"
                + "WPTW"
                + "TSPP";
        Path path = Path.of(workingDir + "default_info.json");
        assertEquals(10, Solution.getResultFromFile(simpleField, "Human", path));

        // Random test
        BiFunction<String, String, Integer> func = (field, race) -> {
            try {
                return Solution.getResultFromFile(field, race, path);
            } catch (SolutionException e) {
                throw new RuntimeException(e);
            }
        };
        testRandom(16, DEFAULT_ALPHABET, DEFAULT_RACES, func, null);
        testRandom(25, DEFAULT_ALPHABET, DEFAULT_RACES, func, null);
    }

    @Test
    public void testDijkstra() {
        Path path = Path.of(workingDir + "info_for_dijkstra.json");
        BiFunction<String, String, Integer> func = (field, race) -> {
            try {
                return Solution.getResultFromFile(field, race, path);
            } catch (SolutionException e) {
                throw new RuntimeException(e);
            }
        };
        String[] races = new String[]{"Waterman"};
        Map<Character, Integer> costs = new HashMap<>();
        costs.put('S', 1);
        costs.put('W', 0);
        costs.put('T', 4);
        costs.put('P', 5);
        testRandom(16, DEFAULT_ALPHABET, races, func, costs);
        testRandom(25, DEFAULT_ALPHABET, races, func, costs);
    }

    @Test
    public void testNegative() throws SolutionException {
        String field = "ABAAC"
                + "BBBAA"
                + "ABBAA"
                + "AAAAA"
                + "ACAAA";
        Path path = Path.of(workingDir + "info_with_negative_cells.json");
        assertEquals(3, Solution.getResultFromFile(field, "Flash", path));

        String incorrectField = "ABAC"
                + "BBAC"
                + "ABAA"
                + "AAAA";
        SolutionException e = assertThrows(SolutionException.class, () -> Solution.getResultFromFile(incorrectField, "Flash", path));
        assertEquals("Field contains negative cycle", e.getMessage());
    }

    @Test
    public void testFileExceptions() {
        String field = "PTWP"
                + "TTPP"
                + "WPTW"
                + "TSPP";
        String race = "Human";

        Path incorrectPath = Path.of(workingDir + "not_exist.json");
        SolutionException e = assertThrows(SolutionException.class, () -> Solution.getResultFromFile(field, race, incorrectPath));
        assertEquals(String.format("Can't read file %s", incorrectPath), e.getMessage());

        Path pathDefault = Path.of(workingDir + "default_info.json");
        e = assertThrows(SolutionException.class, () -> Solution.getResultFromFile(field, "Nobody", pathDefault));
        assertEquals("File don't contains race Nobody", e.getMessage());

        Path pathToIncorrectLetters = Path.of(workingDir + "info_with_incorrect_symbols.json");
        e = assertThrows(SolutionException.class, () -> Solution.getResultFromFile(field, race, pathToIncorrectLetters));
        assertEquals("Key types must be single letter", e.getMessage());
    }
}
