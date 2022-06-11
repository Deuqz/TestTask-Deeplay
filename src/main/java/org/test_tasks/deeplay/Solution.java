package org.test_tasks.deeplay;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Arrays;
import java.util.function.BiConsumer;

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

    private static Map<Character, Integer> getCosts(String species) {
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
            throw new SolutionException(String.format("Incorrect field length, current length: %d", field.length()));
        }
        return len;
    }

    private static int calcAStar(String field, Map<Character, Integer> costs, int length) {
        Node start = createGraph(field, costs, length);
        int result = 0;
        Comparator<Node> comparator = (a, b) -> {
            int valA = a.distance + heuristic(a, length);
            int valB = b.distance + heuristic(b, length);
            return Integer.compare(valA, valB);
        };
        PriorityQueue<Node> curCells = new PriorityQueue<>(comparator);
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
        assert result != 0;
        return result;
    }

    // Двумерная динамика по полю
    private static int calcDynamics(String field, Map<Character, Integer> costs, int length) {
        // Оптимизация через один массив, т.к. достаточно хранить только текущую строку
        int[] dp = new int[length];
        Arrays.fill(dp, Integer.MAX_VALUE);
        dp[0] = 0;
        char[] cells = field.toCharArray();
        for (int i = 0; i < length; ++i) {
            int ind = i * length;
            // Мы не должны обновлять стартовую клетку
            if (ind != 0) {
                dp[0] = dp[0] + costs.get(cells[ind++]);
            }
            for (int j = 1; j < length; ++j, ++ind) {
                dp[j] = Math.min(dp[j - 1], dp[j]) + costs.get(cells[ind]);
            }
        }
        return dp[length - 1];
    }

    public static int getResult(String field, String species) throws SolutionException {
        int length = getLength(field);
        Map<Character, Integer> costs = getCosts(species);
        if (length <= 4) {
            return calcDynamics(field, costs, length);
        } else {
            return calcAStar(field, costs, length);
        }
    }
}