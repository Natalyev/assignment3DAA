/**
 * Mission 3. Escape: DFS from cell 0 to the last cell (rows * cols - 1) in the maze of Mission 1.
 * Two neighbouring cells are connected when the wall between them is broken.
 * The neighbours are tried in this order: up, right, down, left (cell - cols, cell + 1, cell + cols, cell - 1).
 * The ready method Graphs.open(w, broken) gives you the open neighbours of every cell, in no particular order:
 * to visit them in the required order, go through the four cells above one by one and take those that are in the list.
 * The program must not overflow the stack on a maze of 1000 by 1000 cells.
 */
public class Escape {

    /**
     * path: the cells of the way out, from 0 to the last cell.
     * visited: how many cells are marked as visited at the moment the exit is marked (the start and the exit are counted).
     * deadEnds: how many cells of the maze have exactly one open neighbour.
     */
    public record Result(int[] path, int visited, int deadEnds) {}

    public static Result escape(Input.Walls w, int[] broken) {
        int[][] graph = Graphs.open(w, broken);
        int n = w.cells();
        int deadEnds = 0;
        for (int[] neighbours : graph) {
            if (neighbours.length == 1) deadEnds++;
        }
        boolean[] visited = new boolean[n];
        int[] stack = new int[n];
        int[] direction = new int[n];
        int top = 0;
        int count = 1;
        stack[0] = 0;
        visited[0] = true;

        while (top >= 0) {
            int cell = stack[top];
            if (cell == n - 1) {
                return new Result(java.util.Arrays.copyOf(stack, top + 1), count, deadEnds);
            }
            if (direction[top] == 4) {
                top--;
                continue;
            }
            int next = neighbour(cell, direction[top]++, w.rows(), w.cols());
            if (next < 0 || visited[next]) continue;
            boolean open = false;
            for (int v : graph[cell]) {
                if (v == next) open = true;
            }
            if (!open) continue;
            visited[next] = true;
            count++;
            top++;
            stack[top] = next;
            direction[top] = 0;
        }
        return new Result(new int[0], count, deadEnds);
    }

    private static int neighbour(int cell, int direction, int rows, int cols) {
        int row = cell / cols;
        int col = cell % cols;
        return switch (direction) {
            case 0 -> row > 0 ? cell - cols : -1;
            case 1 -> col + 1 < cols ? cell + 1 : -1;
            case 2 -> row + 1 < rows ? cell + cols : -1;
            default -> col > 0 ? cell - 1 : -1;
        };
    }
}
