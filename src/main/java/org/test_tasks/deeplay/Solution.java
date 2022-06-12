package org.test_tasks.deeplay;

//import java.util.List;
//import java.util.ArrayList;
//import java.util.Map;
//import java.util.HashMap;
//import java.util.Comparator;
//import java.util.PriorityQueue;
//import java.util.Arrays;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class Solution {
    protected static class Node {
        protected int x, y;
        protected int cost;
        protected int distance = 0;
        protected boolean visited = false;
        protected List<Node> neighbors = new ArrayList<>();

        protected Node(int x, int y, int cost) {
            this.x = x;
            this.y = y;
            this.cost = cost;
        }

        public void addNeighbor(Node neighbor) {
            neighbors.add(neighbor);
        }
    }

    protected static int heuristic(Node v, int length) {
        // Manhattan distance
        int goalCoord = length - 1;
        return (goalCoord - v.x) + (goalCoord - v.y);
    }

    private static Map<Character, Integer> getDefaultCosts(String race) {
        List<Integer> prices;
        if (race.equals("Human")) {
            prices = List.of(5, 2, 3, 1);
        } else if (race.equals("Swamper")) {
            prices = List.of(2, 2, 5, 2);
        } else {
            assert race.equals("Woodman");
            prices = List.of(3, 3, 2, 2);
        }
        Map<Character, Integer> costs = new HashMap<>();
        costs.put('S', prices.get(0));
        costs.put('W', prices.get(1));
        costs.put('T', prices.get(2));
        costs.put('P', prices.get(3));
        return costs;
    }

    private static Node createGraph(String field, Map<Character, Integer> costs, int length) {
        List<Node> graph = new ArrayList<>();
        char[] cells = field.toCharArray();
        BiConsumer<Integer, Node> createDependency = (indPrevNode, node) -> {
            Node prevNode = graph.get(indPrevNode);
            node.addNeighbor(prevNode);
            prevNode.addNeighbor(node);
        };
        for (int i = 0; i < length; ++i) {
            for (int j = 0; j < length; ++j) {
                int ind = length * i + j;
                Node node = new Node(j, i, costs.get(cells[ind]));
                if (j > 0) {
                    createDependency.accept(ind - 1, node);
                }
                if (i > 0) {
                    createDependency.accept(ind - length, node);
                }
                graph.add(node);
            }
        }
        return graph.get(0);
    }

    private static int getLength(String field) throws SolutionException {
        int len = (int) Math.floor(Math.sqrt(field.length()));
        if (len * len != field.length()) {
            throw new SolutionException(String.format("Field must be square, current field length: %d", field.length()));
        }
        return len;
    }

    private static int walkGraph(Node start, int length, Comparator<Node> comp) {
        int result = 0;
        PriorityQueue<Node> curCells = new PriorityQueue<>(comp);
        curCells.add(start);
        while (!curCells.isEmpty()) {
            Node cur = curCells.poll();
            if (cur.x == length - 1 && cur.y == length - 1) {
                result = cur.distance;
                break;
            }
            for (Node next : cur.neighbors) {
                int newDistance = cur.distance + next.cost;
                if (!next.visited || newDistance < next.distance) {
                    next.distance = newDistance;
                    next.visited = true;
                    curCells.add(next);
                }
            }
        }
        return result;
    }

    private static int calcAStar(Node start, int length) {
        Comparator<Node> comp = (a, b) -> {
            int valA = a.distance + heuristic(a, length);
            int valB = b.distance + heuristic(b, length);
            return Integer.compare(valA, valB);
        };
        return walkGraph(start, length, comp);
    }

    private static int calcDijkstra(Node start, int length) {
        Comparator<Node> comp = Comparator.comparingInt(a -> a.distance);
        return walkGraph(start, length, comp);
    }

    @FunctionalInterface
    private interface Consumer4 {
        void accept(Integer i, Integer j, Integer k, Node nd);
    }

    private static int calcFordBellman(Node start, int length) throws SolutionException {
        int n = length * length;
        int[] dp = new int[n];
        Arrays.fill(dp, Integer.MAX_VALUE);
        dp[0] = 0;

        Function<Node, Integer> getIndex = node -> node.y * length + node.x;
        Consumer4 relax = (i, j, k, nd) -> {
            int newDist = dp[i] + nd.cost;
            if (k == n - 1 && newDist < dp[j]) {
                throw new RuntimeException();
            }
            dp[j] = Math.min(dp[j], newDist);
        };

        for (int k = 0; k < n; ++k) {
            Queue<Node> nodes = new LinkedList<>();
            nodes.add(start);
            while (!nodes.isEmpty()) {
                Node cur = nodes.poll();
                int indCur = getIndex.apply(cur);
                for (Node next : cur.neighbors) {
                    if (next.x > cur.x || next.y > cur.y) {
                        int indNext = getIndex.apply(next);
                        try {
                            relax.accept(indCur, indNext, k, next);
                            nodes.add(next);
                            relax.accept(indNext, indCur, k, cur);
                        } catch (RuntimeException e) {
                            throw new SolutionException("Field contains negative cycle");
                        }
                    }
                }
            }
        }
        return dp[n - 1];
    }

    private static int calcDynamics(@NotNull String field, Map<Character, Integer> costs, int length) {
        // Optimization through a single array, because we can store only the current row
        int[] dp = new int[length];
        Arrays.fill(dp, Integer.MAX_VALUE);
        dp[0] = 0;
        char[] cells = field.toCharArray();
        for (int i = 0; i < length; ++i) {
            int ind = i * length;
            if (ind != 0) {
                dp[0] = dp[0] + costs.get(cells[ind]);
            }
            ++ind;
            for (int j = 1; j < length; ++j, ++ind) {
                dp[j] = Math.min(dp[j - 1], dp[j]) + costs.get(cells[ind]);
            }
        }
        return dp[length - 1];
    }

    private static void checkFieldCorrectness(String field, Map<Character, Integer> costs) throws SolutionException {
        boolean isCorrect = field.chars().allMatch(c -> costs.containsKey((char) c));
        if (!isCorrect) {
            throw new SolutionException("Field contains incorrect symbols");
        }
    }

    public static int getResult(@NotNull String field, @NotNull String race) throws SolutionException {
        if (!race.equals("Human") && !race.equals("Swamper") && !race.equals("Woodman")) {
            throw new SolutionException("Incorrect race");
        }
        Map<Character, Integer> costs = getDefaultCosts(race);
        checkFieldCorrectness(field, costs);
        int length = getLength(field);
        if (length <= 4) {
            return calcDynamics(field, costs, length);
        } else {
            Node start = createGraph(field, costs, length);
            return calcAStar(start, length);
        }
    }

    public static int getResultFromFile(@NotNull String field, @NotNull String race, @NotNull Path path)
            throws SolutionException {
        // Parse file
        JSONObject specJSON;
        try {
            JSONObject info = new JSONObject(Files.readString(path));
            specJSON = info.getJSONObject(race);
        } catch (IOException e) {
            throw new SolutionException(String.format("Can't read file %s", path), e);
        } catch (org.json.JSONException e) {
            throw new SolutionException(String.format("File don't contains race %s", race), e);
        }

        // Get costs
        boolean allCostsPositive = true;
        boolean hasNegative = false;
        Map<Character, Integer> costs = new HashMap<>();
        for (Iterator<String> it = specJSON.keys(); it.hasNext(); ) {
            String type = it.next();
            if (type.length() > 1) {
                throw new SolutionException("Key types must be single letter");
            }
            int cost = specJSON.getInt(type);
            if (cost <= 0) {
                allCostsPositive = false;
            }
            if (cost < 0) {
                hasNegative = true;
            }
            costs.put(type.charAt(0), specJSON.getInt(type));
        }
        checkFieldCorrectness(field, costs);

        // Calculate
        int length = getLength(field);
        if (length <= 4 && allCostsPositive) {
            return calcDynamics(field, costs, length);
        } else {
            Node start = createGraph(field, costs, length);
            if (allCostsPositive) {
                return calcAStar(start, length);
            } else if (!hasNegative) {
                return calcDijkstra(start, length);
            } else {
                return calcFordBellman(start, length);
            }
        }
    }
}