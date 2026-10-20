import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/** Independent small-graph checks and a million-cell stack-safety check. */
public class AlgorithmTest {
    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    static long cost(Input.Walls w, int[] edges) {
        long sum = 0;
        for (int k : edges) sum += w.cost()[k];
        return sum;
    }

    // Exhaust all subsets on a tiny grid; connectivity is checked without DSU.
    static long bruteMst(Input.Walls w) {
        long best = Long.MAX_VALUE;
        for (int mask = 0; mask < (1 << w.count()); mask++) {
            if (Integer.bitCount(mask) != w.cells() - 1) continue;
            boolean[] reached = new boolean[w.cells()];
            reached[0] = true;
            long total = 0;
            for (int k = 0; k < w.count(); k++) {
                if ((mask & (1 << k)) != 0) total += w.cost()[k];
            }
            for (int pass = 0; pass < w.cells(); pass++) {
                for (int k = 0; k < w.count(); k++) {
                    if ((mask & (1 << k)) == 0) continue;
                    int a = w.a()[k], b = w.b()[k];
                    if (reached[a] || reached[b]) reached[a] = reached[b] = true;
                }
            }
            boolean connected = true;
            for (boolean value : reached) connected &= value;
            if (connected) best = Math.min(best, total);
        }
        return best;
    }

    static int visits;
    static boolean smallDfs(int v, int cols, int[][] g, boolean[] seen,
                            ArrayList<Integer> path) {
        seen[v] = true;
        visits++;
        path.add(v);
        if (v == g.length - 1) return true;
        int[] candidates = {v - cols, v + 1, v + cols, v - 1};
        for (int next : candidates) {
            for (int neighbour : g[v]) {
                if (neighbour == next && !seen[next]
                        && smallDfs(next, cols, g, seen, path)) return true;
            }
        }
        path.remove(path.size() - 1);
        return false;
    }

    static void mazeChecks() {
        for (int seed = 0; seed < 30; seed++) {
            Input.Walls w = Generator.generate(2, 3, seed);
            long expected = bruteMst(w);
            check(cost(w, Maze.cheapestMaze(w)) == expected, "Kruskal vs exhaustive MST");
            check(Maze.primCost(w) == expected, "Prim vs exhaustive MST");
            int[] broken = Maze.breakWalls(w);
            check(broken.length == w.cells() - 1, "tree edge count");
            Escape.Result result = Escape.escape(w, broken);
            int[][] g = Graphs.open(w, broken);
            visits = 0;
            ArrayList<Integer> path = new ArrayList<>();
            check(smallDfs(0, w.cols(), g, new boolean[w.cells()], path), "exit reachable");
            check(Arrays.equals(result.path(), path.stream().mapToInt(i -> i).toArray()),
                    "iterative path vs recursive reference");
            check(result.visited() == visits, "DFS exact visited count");
            int leaves = 0;
            for (int[] neighbours : g) if (neighbours.length == 1) leaves++;
            check(result.deadEnds() == leaves, "dead ends");
        }
        Input.Walls single = new Input.Walls(1, 1, new int[0], new int[0], new int[0]);
        check(Maze.breakWalls(single).length == 0, "one cell: no walls");
        check(Maze.cheapestMaze(single).length == 0 && Maze.primCost(single) == 0,
                "one cell: zero MST cost");
        Escape.Result one = Escape.escape(single, new int[0]);
        check(one.path().length == 1 && one.visited() == 1 && one.deadEnds() == 0,
                "one cell: start equals exit");
    }

    static int longest(int v, long[][] edges) {
        int best = 1;
        for (int next = 0; next < edges.length; next++) {
            if (edges[v][next] < INF) best = Math.max(best, 1 + longest(next, edges));
        }
        return best;
    }

    static final long INF = Long.MAX_VALUE / 4;

    static void zoneChecks() {
        Random random = new Random(252119);
        for (int trial = 0; trial < 200; trial++) {
            int n = 1 + random.nextInt(8);
            ArrayList<Integer> from = new ArrayList<>(), to = new ArrayList<>(), time = new ArrayList<>();
            // Ensure the last cell is reachable; extra edges create cycles and duplicates.
            for (int i = 0; i < n - 1; i++) {
                from.add(i); to.add(i + 1); time.add(1 + random.nextInt(20));
            }
            for (int i = 0; i < n * n; i++) {
                int a = random.nextInt(n), b = random.nextInt(n);
                if (a != b && random.nextBoolean()) {
                    from.add(a); to.add(b); time.add(1 + random.nextInt(20));
                }
            }
            Input.Slides s = new Input.Slides(n, from.stream().mapToInt(i -> i).toArray(),
                    to.stream().mapToInt(i -> i).toArray(), time.stream().mapToInt(i -> i).toArray());
            boolean[][] reach = new boolean[n][n];
            for (int i = 0; i < n; i++) reach[i][i] = true;
            for (int i = 0; i < s.count(); i++) reach[s.from()[i]][s.to()[i]] = true;
            for (int k = 0; k < n; k++)
                for (int a = 0; a < n; a++)
                    for (int b = 0; b < n; b++) reach[a][b] |= reach[a][k] && reach[k][b];
            int[] zones = Zones.kosaraju(s);
            int count = Arrays.stream(zones).max().orElseThrow() + 1;
            for (int a = 0; a < n; a++)
                for (int b = 0; b < n; b++)
                    check((zones[a] == zones[b]) == (reach[a][b] && reach[b][a]),
                            "SCC vs all-pairs reachability");
            long[][] edges = new long[count][count];
            for (long[] row : edges) Arrays.fill(row, INF);
            for (int i = 0; i < s.count(); i++) {
                int a = zones[s.from()[i]], b = zones[s.to()[i]];
                if (a == b) continue;
                check(a < b, "zone IDs are topological");
                edges[a][b] = Math.min(edges[a][b], s.time()[i]);
            }
            int stages = 0;
            for (int v = 0; v < count; v++) stages = Math.max(stages, longest(v, edges));
            long[] actual = Zones.routes(s, zones, count);
            for (int v = 0; v < count; v++) edges[v][v] = 0;
            for (int k = 0; k < count; k++)
                for (int a = 0; a < count; a++)
                    for (int b = 0; b < count; b++)
                        edges[a][b] = Math.min(edges[a][b], edges[a][k] + edges[k][b]);
            check(actual[0] == stages, "longest chain vs exhaustive paths");
            check(actual[1] == edges[zones[0]][zones[n - 1]], "route vs Floyd-Warshall");
        }
    }

    static void deepChecks() {
        int side = 1000, n = side * side;
        int[] a = new int[n - 1], b = new int[n - 1], costs = new int[n - 1];
        int[] broken = new int[n - 1];
        int previous = 0, edge = 0;
        for (int row = 0; row < side; row++) {
            for (int step = 0; step < side; step++) {
                int col = row % 2 == 0 ? step : side - 1 - step;
                int cell = row * side + col;
                if (row == 0 && step == 0) continue;
                a[edge] = previous; b[edge] = cell; costs[edge] = 1;
                broken[edge] = edge;
                previous = cell;
                edge++;
            }
        }
        Escape.Result r = Escape.escape(new Input.Walls(side, side, a, b, costs), broken);
        check(r.path().length == n - side + 1 && r.visited() == n - side + 1,
                "million-cell snake: deep DFS without recursion");
        int chain = 100_000;
        int[] from = new int[chain - 1], to = new int[chain - 1], time = new int[chain - 1];
        for (int i = 0; i < chain - 1; i++) { from[i] = i; to[i] = i + 1; time[i] = 1; }
        Input.Slides slides = new Input.Slides(chain, from, to, time);
        int[] zones = Zones.kosaraju(slides);
        long[] routes = Zones.routes(slides, zones, chain);
        check(routes[0] == chain && routes[1] == chain - 1, "deep slide chain");
    }

    public static void main(String[] args) {
        mazeChecks();
        zoneChecks();
        deepChecks();
        System.out.println("All algorithm checks passed: exhaustive MST, DFS, SCC, routes and deep graphs.");
    }
}
