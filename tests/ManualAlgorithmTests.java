import java.util.List;

/** Direct checks for the route helper, which is intentionally not a JSON task. */
public final class ManualAlgorithmTests {
    public static void main(String[] args) {
        int[][] edges = {
            {0, 1, 5}, {0, 2, 5}, {1, 3, -2}, {2, 3, -2}, {3, 4, 1}, {0, 1, 5}
        };
        int[] parentEdge = {-1, 0, 1, 3, 4, -1};
        check(Algorithms.reconstructRoute(0, 4, edges, parentEdge), List.of(0, 2, 3, 4));
        check(Algorithms.reconstructRoute(0, 0, edges, parentEdge), List.of(0));
        check(Algorithms.reconstructRoute(0, 5, edges, parentEdge), List.of());
        System.out.println("PASS route helper: reachable, source, unreachable, tied/parallel");
    }

    private static void check(List<Integer> actual, List<Integer> expected) {
        if (!actual.equals(expected)) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
    }
}
