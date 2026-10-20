import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * Missions 1 and 2. Building the maze out of the walls.
 * A wall k joins the cells w.a()[k] and w.b()[k] and costs w.cost()[k]. Indexes start from 0,
 * the line number in walls.txt is the index + 1. The cells are connected by DisjointSet.
 */
public class Maze {

    /**
     * Mission 1. Take the walls in the order of the file. Break a wall when its two cells are in different sets.
     * Returns the indexes of the broken walls, in the order of breaking.
     */
    public static int[] breakWalls(Input.Walls w) {
        DisjointSet sets = new DisjointSet(w.cells());
        int[] broken = new int[w.cells() - 1];
        int count = 0;
        for (int k = 0; k < w.count(); k++) {
            if (sets.union(w.a()[k], w.b()[k])) {
                broken[count++] = k;
            }
        }
        return java.util.Arrays.copyOf(broken, count);
    }

    /**
     * Mission 2. Kruskal: the same, but take the walls from the cheapest to the most expensive.
     * Returns the indexes of the broken walls.
     */
    public static int[] cheapestMaze(Input.Walls w) {
        Integer[] order = new Integer[w.count()];
        for (int k = 0; k < order.length; k++) order[k] = k;
        Arrays.sort(order, Comparator.comparingInt(k -> w.cost()[k]));
        DisjointSet sets = new DisjointSet(w.cells());
        int[] broken = new int[w.cells() - 1];
        int count = 0;
        for (int k : order) {
            if (sets.union(w.a()[k], w.b()[k])) {
                broken[count++] = k;
                if (count == broken.length) break;
            }
        }
        return Arrays.copyOf(broken, count);
    }

    /**
     * Mission 2. Prim with a priority queue. Returns the total cost of the cheapest maze.
     * It must be equal to the total cost of cheapestMaze.
     */
    public static long primCost(Input.Walls w) {
        int n = w.cells();
        int[] degree = new int[n];
        for (int k = 0; k < w.count(); k++) {
            degree[w.a()[k]]++;
            degree[w.b()[k]]++;
        }
        int[][] edges = new int[n][];
        for (int v = 0; v < n; v++) edges[v] = new int[degree[v]];
        int[] filled = new int[n];
        for (int k = 0; k < w.count(); k++) {
            edges[w.a()[k]][filled[w.a()[k]]++] = k;
            edges[w.b()[k]][filled[w.b()[k]]++] = k;
        }

        boolean[] visited = new boolean[n];
        PriorityQueue<Integer> queue =
                new PriorityQueue<>(Comparator.comparingInt(k -> w.cost()[k]));
        long cost = 0;
        int current = 0;
        for (int taken = 0; taken < n; taken++) {
            visited[current] = true;
            for (int k : edges[current]) {
                int other = w.a()[k] == current ? w.b()[k] : w.a()[k];
                if (!visited[other]) queue.add(k);
            }
            if (taken == n - 1) break;
            // Discard old edges whose two endpoints are already in the tree.
            while (!queue.isEmpty()) {
                int k = queue.peek();
                if (!visited[w.a()[k]] || !visited[w.b()[k]]) break;
                queue.remove();
            }
            if (queue.isEmpty()) throw new IllegalArgumentException("Disconnected maze");
            int k = queue.remove();
            cost += w.cost()[k];
            current = visited[w.a()[k]] ? w.b()[k] : w.a()[k];
        }
        return cost;
    }
}
