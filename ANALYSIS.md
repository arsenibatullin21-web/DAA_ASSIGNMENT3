# Assignment 3: Graph Algorithms Analysis

**Language:** Java 17 source compatibility (compiled with JDK 25).  
**Student:** Arsen Ibatullin, 254866.

Each JSON-lines request is independent. `Main` parses one request, invokes exactly one method, writes one JSON object, and retains no algorithm state between requests. Every method allocates its own arrays, graph lists, queues and DSU.

## 1. Disjoint-set union (DSU)

`DisjointSet` starts with one singleton set per vertex. `find` first walks to the root and then makes every vertex on that path point directly to the root (path compression). `union` joins the smaller root below the larger root, decreases `components`, and updates `largest` only when a merge happens.

**Invariant:** each root represents one connected component; its `size` equals the number of vertices in that component. Therefore `components` and `largest` after every operation describe the partition exactly. A repeated union or a self-union finds the same root and does not change the invariant.

**Correctness:** initially the invariant is true for singleton vertices. A successful union combines exactly two disjoint components, so both counters are updated correctly. Path compression changes only parent pointers inside one component, not membership. By induction, every returned state is correct.

**Boundary cases:** `n = 0`, no operations, self-unions and repeated unions are supported.  
**Complexity:** amortized `O(alpha(n))` per operation and `O(n)` memory.

## 2. Kruskal minimum spanning forest

Edges are assigned their input positions as IDs, sorted by nondecreasing weight, and scanned once. An edge is selected exactly if its endpoints are currently in different DSU sets. Costs are accumulated in `long`.

**Invariant:** after each scanned prefix, the selected edges are acyclic and have minimum possible weight among forests obtainable from that prefix with the same component partition. The cut property says that a lightest safe edge joining two current components can be selected. DSU rejects loops and any edge that would make a cycle. Thus, after all edges, the result is a minimum spanning forest (MSF) in every connected component.

**Boundary cases:** empty graph, isolated vertices, negative weights, loops, parallel edges and totals above 32-bit range.  
**Complexity:** sorting dominates: `O(m log m)` time and `O(n + m)` memory.

## 3. Prim minimum spanning forest

For every unvisited vertex, Prim starts a new component. A priority queue stores incident candidate edges ordered by `(weight, edge ID)`. It repeatedly selects the lightest edge with exactly one endpoint already in the current tree, then adds the outside endpoint. Restarting makes the result a forest rather than assuming the graph is connected.

**Invariant:** the selected edges of a restart form a tree, and the queue contains every encountered edge crossing from that tree to unvisited vertices (plus harmless stale edges). The minimum crossing edge is safe by the cut property. Consequently each restart builds an MST of its connected component, and all restarts form an MSF.

**Boundary cases:** `n = 0`, isolates, disconnected graphs, self-loops, negative and parallel/tied edges.  
**Complexity:** `O(m log m)` time with this binary priority queue and `O(n + m)` memory.

## 4. Strongly connected components (Kosaraju)

The program constructs the directed graph and its transpose. The first iterative DFS records vertices at finish time. The second iterative DFS processes that list backwards on the transpose; every traversal receives one component label. Finally, a set deduplicates original edges that go between different labels to build the condensation edges.

**Invariant:** after the first pass, vertices with later finish times are processed first in the transpose. Such a traversal cannot leave its SCC into an unprocessed SCC. Therefore each second-pass traversal marks exactly one SCC. The final scan adds an ordered condensation pair iff an original cross-component edge exists.

**Correctness:** the finish-time property of the SCC condensation DAG makes the second-pass root a source in the remaining transpose DAG. It reaches all and only its own SCC. Repeating covers every vertex exactly once; deduplication makes the condensation exact.

**Boundary cases:** empty/disconnected graphs, isolates, duplicate edges and self-loops. Both DFS passes are explicit stacks, so a 100,000-vertex chain does not overflow the Java call stack.  
**Complexity:** `O(n + m)` time and `O(n + m)` memory.

## 5. Topological sorting

The iterative DFS uses three states: `0` unvisited, `1` active, `2` finished. Seeing an edge to an active vertex is a back edge, so the graph is cyclic. Otherwise, append a vertex when it finishes and reverse the finish list.

**Invariant:** an active stack describes the current DFS path. Thus an edge to state `1` closes a directed cycle. If no such edge is found, for every edge `u -> v`, `v` finishes before `u`; reversing finish order puts `u` before `v`.

**Correctness:** a back edge proves a cycle. In an acyclic graph DFS has no back edges, and the reverse finish order satisfies every precedence edge, so it is a valid topological order.

**Boundary cases:** empty graph, isolated vertices, duplicate edges, self-loops and cycles in a disconnected component.  
**Complexity:** `O(n + m)` time and `O(n + m)` memory.

## 6. DAG shortest paths and route reconstruction

The program first topologically checks the **whole** directed graph. If it has a cycle, it returns `acyclic: false` and empty arrays, even when the cycle is unreachable from the source. Otherwise `distance[source]` is `0`, unreachable distances remain `null`, and edges are relaxed in topological order. `parent_edge[v]` stores the original ID of the final edge that improved `v`.

**Invariant:** when a reachable vertex is processed, its distance is already the shortest source distance because all paths into it originate earlier in topological order. Relaxation therefore propagates correct shortest distances; negative weights are safe because the graph is acyclic. A parent edge always ends at its child and lies on a shortest route.

**Correctness:** induction over the topological order proves that the first time a reachable vertex is processed, every possible predecessor has already been considered. Taking the minimum relaxation yields the shortest distance. The preceding full DFS establishes that this reasoning is used only for a DAG.

`reconstructRoute(source, target, edges, parentEdge)` follows parent edge IDs backwards, verifies each edge direction, then reverses the list. It returns `[]` for an unreachable/invalid target.

Direct helper checks in `tests/ManualAlgorithmTests.java` produced:

| Target | Result | Selected edge IDs | Weight |
|---|---|---|---|
| reachable 4 | `[0, 2, 3, 4]` | `1, 3, 4` | `5 + (-2) + 1 = 4` |
| source 0 | `[0]` | none | `0` |
| unreachable 5 | `[]` | none | not defined |

The graph also contains tied paths and a parallel `0 -> 1` edge. The route direction is verified by checking that every selected parent edge ends at the current vertex.

**Boundary cases:** source alone, unreachable vertices, `-1` as a valid distance, negative weights, parallel/tied edges, self-loops and cycles outside source reachability.  
**Complexity:** `O(n + m)` time and `O(n + m)` memory; reconstructing a route with `L` vertices is `O(L)`.

## Own tests and test execution

`tests/own_cases.jsonl` contains mixed JSON requests for self/repeated unions, loops, parallel and tied edges, negative weights, a 64-bit total, SCC condensation, disconnected topological cycles, unreachable DAG vertices, and a cycle outside source reachability. `tests/OWN_TESTS.md` gives independently justified expected properties and the recorded outputs. `tests/ManualAlgorithmTests.java` calls the route helper directly.

Commands used:

```powershell
python tools/run.py --check
python tools/run.py --input tests/own_cases.jsonl
javac --release 17 -encoding UTF-8 -d .build/Java src/Java/*.java tests/ManualAlgorithmTests.java
java -cp .build/Java ManualAlgorithmTests
```

The supplied public checker passed all 36 cases, including 2,000-vertex deep SCC/topological/DAG chains, and passed two complete batches in one process. The own batch and the direct route-helper test also passed.

## Kruskal vs Prim benchmark

`tests/Benchmark.java` is reproducible. It uses seed `254866 + n`, creates a connected graph with `m = 8n` (a chain first, then random edges), and uses exactly the same graph for both methods. Before timings it verifies matching MSF costs, acyclicity and one component. One untimed warm-up run per algorithm is executed. Each displayed result is three timed runs; the median is reported.

Timer: `System.nanoTime()`. Runtime: `java -Xmx384m` with JDK 25, compiled with `javac --release 17`, on Microsoft Windows 11 Pro with a 13th Gen Intel Core i7-13650HX processor and 24 GB RAM. Graph generation, compilation, JSON serialization and console output are excluded. Conversion from the already-created JSON-like request rows into the algorithm’s adjacency/edge arrays is included equally for both methods.

Command:

```powershell
javac --release 17 -encoding UTF-8 -d .build/Java src/Java/*.java tests/Benchmark.java
java -Xmx384m -cp .build/Java Benchmark
```

| n | m | Kruskal runs, ms | median, ms | Prim runs, ms | median, ms | verified MSF cost |
|---:|---:|---|---:|---|---:|---:|
| 1,000 | 8,000 | 4.931 / 3.696 / 3.567 | 3.696 | 6.996 / 6.745 / 5.291 | 6.745 | -846,475,593 |
| 10,000 | 80,000 | 42.563 / 34.457 / 40.913 | 40.913 | 61.124 / 58.571 / 59.436 | 59.436 | -8,500,065,204 |
| 50,000 | 400,000 | 148.291 / 145.488 / 216.367 | 148.291 | 503.664 / 514.533 / 516.356 | 514.533 | -42,480,418,573 |

Both methods use a binary heap/sort and have an `O(m log m)` upper bound here. Kruskal was faster in these particular dense random inputs because it sorts one flat edge array and its DSU scan is simple; Prim builds adjacency lists, performs many priority-queue operations and skips stale candidates. These measurements are not a proof of asymptotic superiority: JVM warm-up, garbage collection, hardware, graph density and input representation can change the constants.

## External tools and verification

I used the supplied assignment template, its public checker and an AI coding assistant to understand the contract and draft code. I independently ran compilation, all 36 public checks, the repeated-batch state-reset check, custom JSON cases, direct route reconstruction assertions and the recorded benchmark. I am responsible for reviewing, explaining and submitting the result.
