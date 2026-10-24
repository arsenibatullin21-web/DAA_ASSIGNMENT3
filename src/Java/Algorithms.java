import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Implement the six methods and add your route-reconstruction helper.
 * Input extraction is provided; all algorithm state belongs to the current request.
 * See ../../README.md for contracts and Java guidance.
 */
public final class Algorithms {
    /** DSU with path compression and union by size/rank; return states. */
    static Map<String, Object> dsu(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] operations = Input.operations(input);
        DisjointSet set = new DisjointSet(n);
        List<List<Integer>> states = new ArrayList<>();
        for (int[] operation : operations) {
            set.union(operation[0], operation[1]);
            states.add(List.of(set.components, set.largest));
        }
        return result("states", states);
    }

    /** Separate Kruskal implementation; return cost, edge_ids, components. */
    static Map<String, Object> kruskal(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] edges = Input.edges(input);

        Edge[] all = edgeArray(edges);
        Arrays.sort(all, Comparator.comparingInt(edge -> edge.weight));
        DisjointSet set = new DisjointSet(n);
        long cost = 0;
        List<Integer> chosen = new ArrayList<>();
        for (Edge edge : all) {
            if (set.union(edge.from, edge.to)) {
                cost += edge.weight;
                chosen.add(edge.id);
            }
        }
        Map<String, Object> answer = new LinkedHashMap<>();
        answer.put("cost", cost);
        answer.put("edge_ids", chosen);
        answer.put("components", set.components);
        return answer;
    }

    /** Separate Prim implementation; restart for every unvisited component. */
    static Map<String, Object> prim(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] edges = Input.edges(input);

        @SuppressWarnings("unchecked")
        ArrayList<Edge>[] graph = new ArrayList[n];
        for (int vertex = 0; vertex < n; vertex++) graph[vertex] = new ArrayList<>();
        for (int id = 0; id < edges.length; id++) {
            Edge edge = new Edge(edges[id][0], edges[id][1], edges[id][2], id);
            graph[edge.from].add(edge);
            graph[edge.to].add(edge);
        }

        boolean[] used = new boolean[n];
        PriorityQueue<Edge> frontier = new PriorityQueue<>(Comparator
                .comparingInt((Edge edge) -> edge.weight)
                .thenComparingInt(edge -> edge.id));
        long cost = 0;
        int components = 0;
        List<Integer> chosen = new ArrayList<>();

        for (int start = 0; start < n; start++) {
            if (used[start]) continue;
            components++;
            used[start] = true;
            frontier.addAll(graph[start]);
            while (!frontier.isEmpty()) {
                Edge edge = frontier.poll();
                int next;
                if (used[edge.from] && !used[edge.to]) next = edge.to;
                else if (used[edge.to] && !used[edge.from]) next = edge.from;
                else continue;

                used[next] = true;
                cost += edge.weight;
                chosen.add(edge.id);
                frontier.addAll(graph[next]);
            }
        }
        Map<String, Object> answer = new LinkedHashMap<>();
        answer.put("cost", cost);
        answer.put("edge_ids", chosen);
        answer.put("components", components);
        return answer;
    }

    /** Kosaraju's two DFS passes; return component and exact condensation. */
    static Map<String, Object> scc(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] edges = Input.edges(input);

        ArrayList<Integer>[] graph = directedGraph(n, edges, false);
        ArrayList<Integer>[] transpose = directedGraph(n, edges, true);
        boolean[] visited = new boolean[n];
        List<Integer> finish = new ArrayList<>(n);

        for (int start = 0; start < n; start++) {
            if (!visited[start]) finishDfs(start, graph, visited, finish);
        }

        Arrays.fill(visited, false);
        int[] component = new int[n];
        int label = 0;
        for (int index = finish.size() - 1; index >= 0; index--) {
            int start = finish.get(index);
            if (visited[start]) continue;
            ArrayDeque<Integer> stack = new ArrayDeque<>();
            stack.push(start);
            visited[start] = true;
            while (!stack.isEmpty()) {
                int vertex = stack.pop();
                component[vertex] = label;
                for (int next : transpose[vertex]) {
                    if (!visited[next]) {
                        visited[next] = true;
                        stack.push(next);
                    }
                }
            }
            label++;
        }

        HashSet<Long> seen = new HashSet<>();
        List<List<Integer>> condensation = new ArrayList<>();
        for (int[] edge : edges) {
            int from = component[edge[0]];
            int to = component[edge[1]];
            if (from != to) {
                long code = ((long) from << 32) ^ (to & 0xffffffffL);
                if (seen.add(code)) condensation.add(List.of(from, to));
            }
        }
        Map<String, Object> answer = new LinkedHashMap<>();
        answer.put("component", component);
        answer.put("condensation", condensation);
        return answer;
    }

    /** Three-state DFS and reverse finish order; return acyclic and order. */
    static Map<String, Object> topo(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int[][] edges = Input.edges(input);

        ArrayList<Integer>[] graph = directedGraph(n, edges, false);
        List<Integer> order = topologicalOrder(graph, n);
        Map<String, Object> answer = new LinkedHashMap<>();
        if (order == null) {
            answer.put("acyclic", false);
            answer.put("order", new ArrayList<Integer>());
        } else {
            answer.put("acyclic", true);
            answer.put("order", order);
        }
        return answer;
    }

    /** Whole-graph cycle check and DAG relaxation; return acyclic, distance, parent_edge. */
    static Map<String, Object> dag(Map<String, Object> input) {
        int n = Input.vertexCount(input);
        int source = Input.source(input);
        int[][] edges = Input.edges(input);

        @SuppressWarnings("unchecked")
        ArrayList<Edge>[] graph = new ArrayList[n];
        for (int vertex = 0; vertex < n; vertex++) graph[vertex] = new ArrayList<>();
        for (int id = 0; id < edges.length; id++) {
            graph[edges[id][0]].add(new Edge(edges[id][0], edges[id][1], edges[id][2], id));
        }
        List<Integer> order = topologicalOrderWeighted(graph, n);
        Map<String, Object> answer = new LinkedHashMap<>();
        if (order == null) {
            answer.put("acyclic", false);
            answer.put("distance", new ArrayList<Long>());
            answer.put("parent_edge", new int[0]);
            return answer;
        }

        Long[] distance = new Long[n];
        int[] parentEdge = new int[n];
        Arrays.fill(parentEdge, -1);
        distance[source] = 0L;
        for (int vertex : order) {
            if (distance[vertex] == null) continue;
            for (Edge edge : graph[vertex]) {
                long candidate = distance[vertex] + (long) edge.weight;
                if (distance[edge.to] == null || candidate < distance[edge.to]) {
                    distance[edge.to] = candidate;
                    parentEdge[edge.to] = edge.id;
                }
            }
        }
        answer.put("acyclic", true);
        answer.put("distance", distance);
        answer.put("parent_edge", parentEdge);
        return answer;
    }

    /** Reconstructs a chosen source-to-target route, or returns an empty list if no route exists. */
    static List<Integer> reconstructRoute(int source, int target, int[][] edges, int[] parentEdge) {
        List<Integer> reversed = new ArrayList<>();
        int current = target;
        while (true) {
            reversed.add(current);
            if (current == source) break;
            int edgeId = parentEdge[current];
            if (edgeId < 0 || edgeId >= edges.length || edges[edgeId][1] != current) {
                return new ArrayList<>();
            }
            current = edges[edgeId][0];
            if (reversed.size() > parentEdge.length + 1) return new ArrayList<>();
        }
        Collections.reverse(reversed);
        return reversed;
    }

    private static Map<String, Object> result(String key, Object value) {
        Map<String, Object> answer = new LinkedHashMap<>();
        answer.put(key, value);
        return answer;
    }

    private static Edge[] edgeArray(int[][] edges) {
        Edge[] result = new Edge[edges.length];
        for (int id = 0; id < edges.length; id++) {
            result[id] = new Edge(edges[id][0], edges[id][1], edges[id][2], id);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static ArrayList<Integer>[] directedGraph(int n, int[][] edges, boolean reversed) {
        ArrayList<Integer>[] graph = new ArrayList[n];
        for (int vertex = 0; vertex < n; vertex++) graph[vertex] = new ArrayList<>();
        for (int[] edge : edges) {
            graph[reversed ? edge[1] : edge[0]].add(reversed ? edge[0] : edge[1]);
        }
        return graph;
    }

    private static void finishDfs(int start, ArrayList<Integer>[] graph, boolean[] visited, List<Integer> finish) {
        ArrayDeque<Frame> stack = new ArrayDeque<>();
        stack.push(new Frame(start));
        visited[start] = true;
        while (!stack.isEmpty()) {
            Frame frame = stack.peek();
            if (frame.nextIndex < graph[frame.vertex].size()) {
                int next = graph[frame.vertex].get(frame.nextIndex++);
                if (!visited[next]) {
                    visited[next] = true;
                    stack.push(new Frame(next));
                }
            } else {
                finish.add(frame.vertex);
                stack.pop();
            }
        }
    }

    private static List<Integer> topologicalOrder(ArrayList<Integer>[] graph, int n) {
        int[] state = new int[n];
        List<Integer> finish = new ArrayList<>(n);
        for (int start = 0; start < n; start++) {
            if (state[start] != 0) continue;
            ArrayDeque<Frame> stack = new ArrayDeque<>();
            stack.push(new Frame(start));
            state[start] = 1;
            while (!stack.isEmpty()) {
                Frame frame = stack.peek();
                if (frame.nextIndex < graph[frame.vertex].size()) {
                    int next = graph[frame.vertex].get(frame.nextIndex++);
                    if (state[next] == 1) return null;
                    if (state[next] == 0) {
                        state[next] = 1;
                        stack.push(new Frame(next));
                    }
                } else {
                    state[frame.vertex] = 2;
                    finish.add(frame.vertex);
                    stack.pop();
                }
            }
        }
        Collections.reverse(finish);
        return finish;
    }

    private static List<Integer> topologicalOrderWeighted(ArrayList<Edge>[] graph, int n) {
        int[] state = new int[n];
        List<Integer> finish = new ArrayList<>(n);
        for (int start = 0; start < n; start++) {
            if (state[start] != 0) continue;
            ArrayDeque<Frame> stack = new ArrayDeque<>();
            stack.push(new Frame(start));
            state[start] = 1;
            while (!stack.isEmpty()) {
                Frame frame = stack.peek();
                if (frame.nextIndex < graph[frame.vertex].size()) {
                    int next = graph[frame.vertex].get(frame.nextIndex++).to;
                    if (state[next] == 1) return null;
                    if (state[next] == 0) {
                        state[next] = 1;
                        stack.push(new Frame(next));
                    }
                } else {
                    state[frame.vertex] = 2;
                    finish.add(frame.vertex);
                    stack.pop();
                }
            }
        }
        Collections.reverse(finish);
        return finish;
    }

    private static final class Edge {
        final int from, to, weight, id;
        Edge(int from, int to, int weight, int id) {
            this.from = from;
            this.to = to;
            this.weight = weight;
            this.id = id;
        }
    }

    private static final class Frame {
        final int vertex;
        int nextIndex;
        Frame(int vertex) { this.vertex = vertex; }
    }

    private static final class DisjointSet {
        final int[] parent;
        final int[] size;
        int components;
        int largest;

        DisjointSet(int n) {
            parent = new int[n];
            size = new int[n];
            components = n;
            largest = n == 0 ? 0 : 1;
            for (int vertex = 0; vertex < n; vertex++) {
                parent[vertex] = vertex;
                size[vertex] = 1;
            }
        }

        int find(int vertex) {
            int root = vertex;
            while (root != parent[root]) root = parent[root];
            while (vertex != root) {
                int next = parent[vertex];
                parent[vertex] = root;
                vertex = next;
            }
            return root;
        }

        boolean union(int first, int second) {
            int rootFirst = find(first);
            int rootSecond = find(second);
            if (rootFirst == rootSecond) return false;
            if (size[rootFirst] < size[rootSecond]) {
                int saved = rootFirst;
                rootFirst = rootSecond;
                rootSecond = saved;
            }
            parent[rootSecond] = rootFirst;
            size[rootFirst] += size[rootSecond];
            components--;
            if (size[rootFirst] > largest) largest = size[rootFirst];
            return true;
        }
    }
}
