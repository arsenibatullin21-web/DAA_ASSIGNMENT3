# Own tests

## Commands

```powershell
python tools/run.py --input tests/own_cases.jsonl
javac --release 17 -encoding UTF-8 -d .build/Java src/Java/*.java tests/ManualAlgorithmTests.java
java -cp .build/Java ManualAlgorithmTests
```

`own_cases.jsonl` is a mixed JSON-Lines batch. It checks that every request starts with fresh state. The actual output captured from this command is stored in `own_actual_output.jsonl` in the same request order.

## Expected results and justification

| Case | Expected property |
|---|---|
| DSU | States are `[[6,1],[5,2],[4,2],[3,2],[2,4],[2,4]]`; self/repeated unions do not change a set. |
| Kruskal / Prim | A valid minimum forest has edge IDs `{2,3,6,4}`, cost `1000000008`, one component. Negative loops are ignored; the sum needs `long`. |
| SCC | The groups are `{0,1}`, `{2,3}`, `{4,5}`, `{6}` and condensation contains exactly the chain between these groups. Labels can differ. |
| Acyclic topo | Every listed edge goes from an earlier to a later vertex; isolated vertices are also present. |
| Cyclic topo | The disconnected cycle `3 -> 4 -> 5 -> 3` makes `acyclic` false. |
| DAG paths | Valid distances are `[0,5,5,3,4,null]`. Either tied parent edge into vertex 3 is valid, and the parallel edge to vertex 1 must not change the cost. |
| DAG outside-source cycle | The cycle `4 <-> 5` makes the entire graph cyclic even though it is unreachable from source 0. |

## Route helper

`ManualAlgorithmTests.java` calls `reconstructRoute` directly. It verifies:

- reachable target 4: `[0,2,3,4]`, using edge IDs `1,3,4`, total weight `5 + (-2) + 1 = 4`;
- source target 0: `[0]`;
- unreachable target 5: `[]`;
- tied paths and the parallel `0 -> 1` edges are represented in the same test graph.

The helper checks that each parent edge ends at the current vertex, then follows it backwards and reverses the list. Therefore the returned route follows the directed edge orientation.
