import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/** Reproducible in-memory benchmark; graph generation and JSON I/O are excluded from timings. */
public final class Benchmark {
    private static final int[] SIZES = {1_000, 10_000, 50_000};
    private static final int RUNS = 3;

    public static void main(String[] args) {
        System.out.println("n,m,kruskal_ms_runs,kruskal_median_ms,prim_ms_runs,prim_median_ms,cost");
        for (int n : SIZES) {
            int[][] edges = generateGraph(n, 8 * n, 254866L + n);
            Map<String, Object> input = new HashMap<>();
            input.put("n", n);
            input.put("edges", jsonEdges(edges));

            // Warm-up runs are intentionally not timed.
            Map<String, Object> kruskalAnswer = Algorithms.kruskal(input);
            Map<String, Object> primAnswer = Algorithms.prim(input);
            long expectedCost = ((Number) kruskalAnswer.get("cost")).longValue();
            verifyForest(kruskalAnswer, n, edges, expectedCost);
            verifyForest(primAnswer, n, edges, expectedCost);

            long[] kruskalTimes = measure(input, true);
            long[] primTimes = measure(input, false);
            System.out.printf(Locale.ROOT, "%d,%d,%s,%.3f,%s,%.3f,%d%n", n, edges.length,
                    formatMs(kruskalTimes), medianMs(kruskalTimes),
                    formatMs(primTimes), medianMs(primTimes), expectedCost);
        }
    }

    private static long[] measure(Map<String, Object> input, boolean kruskal) {
        long[] times = new long[RUNS];
        for (int run = 0; run < RUNS; run++) {
            long started = System.nanoTime();
            if (kruskal) Algorithms.kruskal(input); else Algorithms.prim(input);
            times[run] = System.nanoTime() - started;
        }
        return times;
    }

    private static int[][] generateGraph(int n, int m, long seed) {
        int[][] edges = new int[m][3];
        Random random = new Random(seed);
        for (int vertex = 1; vertex < n; vertex++) {
            edges[vertex - 1] = new int[] {vertex - 1, vertex, random.nextInt(2_000_001) - 1_000_000};
        }
        for (int id = n - 1; id < m; id++) {
            edges[id] = new int[] {random.nextInt(n), random.nextInt(n), random.nextInt(2_000_001) - 1_000_000};
        }
        return edges;
    }

    private static List<List<Long>> jsonEdges(int[][] edges) {
        List<List<Long>> rows = new ArrayList<>(edges.length);
        for (int[] edge : edges) {
            rows.add(List.of((long) edge[0], (long) edge[1], (long) edge[2]));
        }
        return rows;
    }

    @SuppressWarnings("unchecked")
    private static void verifyForest(Map<String, Object> answer, int n, int[][] edges, long expectedCost) {
        long cost = ((Number) answer.get("cost")).longValue();
        if (cost != expectedCost) throw new AssertionError("cost mismatch");
        int[] ids = ((java.util.List<Integer>) answer.get("edge_ids")).stream().mapToInt(Integer::intValue).toArray();
        int[] parent = new int[n];
        for (int v = 0; v < n; v++) parent[v] = v;
        long sum = 0;
        for (int id : ids) {
            int a = root(parent, edges[id][0]);
            int b = root(parent, edges[id][1]);
            if (a == b) throw new AssertionError("cycle or loop in forest");
            parent[b] = a;
            sum += edges[id][2];
        }
        if (sum != cost || ((Number) answer.get("components")).intValue() != 1) {
            throw new AssertionError("invalid spanning forest");
        }
    }

    private static int root(int[] parent, int vertex) {
        while (parent[vertex] != vertex) {
            parent[vertex] = parent[parent[vertex]];
            vertex = parent[vertex];
        }
        return vertex;
    }

    private static String formatMs(long[] times) {
        StringBuilder text = new StringBuilder();
        for (int index = 0; index < times.length; index++) {
            if (index > 0) text.append('/');
            text.append(String.format(Locale.ROOT, "%.3f", times[index] / 1_000_000.0));
        }
        return text.toString();
    }

    private static double medianMs(long[] times) {
        long[] sorted = times.clone();
        Arrays.sort(sorted);
        return sorted[sorted.length / 2] / 1_000_000.0;
    }
}
