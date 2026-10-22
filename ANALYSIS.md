# Assignment 3: Graph Algorithms

Student: Nurzhan Zholaman  
Student/group identifier supplied: SE-2515  
Language: Java 17-compatible source, no external dependencies

## Build, run and reproduce

Run these commands from the repository root with JDK 17+ and Python 3.12+ available:

```sh
javac --release 17 -encoding UTF-8 -d .build/Java src/Java/*.java
python tools/run.py --setup
python tools/run.py --check
python tools/run.py --input tests/small_input.jsonl
python tests/verify.py
javac --release 17 -encoding UTF-8 -cp .build/Java -d .build/Java tests/GraphChecks.java
java -Xmx384m -cp .build/Java GraphChecks
java -Xmx384m -cp .build/Java GraphChecks benchmark
```

On Windows, `Run.ps1` compiles and runs the JSON Lines program using the installed JDK; `Run.ps1 -Check` runs the supplied public checker. The script sets PATH only for its own process. In IntelliJ, open this directory, choose JDK 17 or newer, mark `src/Java` as Sources Root, and run Main. For standalone testing without PATH, set the JAVA environment variable to the absolute java executable before running tests/verify.py.

Measurements and output files were obtained on 22 October 2026. The installed OpenJDK is 25.0.1; compilation used `--release 17`. Public checks were executed through tools/run.py with absolute paths substituted for java/javac by a temporary launcher, because those commands were absent from the execution environment PATH. The supplied cases and validators were unchanged. Compilation was performed outside the restricted sandbox after its JDK resource-close error; compilation succeeded. No compiler error was ignored.

## Interface, representation and reset

The provided Main, Json and Input files are unchanged. Each line is one independent request and produces one JSON result, including ping. All mutable graph, DSU, heap, DFS and result state is local to a method invocation. Algorithms contains no mutable static fields. Repeated mixed requests therefore cannot inherit previous components, visited flags or distances.

Let n denote vertices, m edges, q union operations, and L the number of vertices in a reconstructed route. Adjacency uses head/to/next/id integer arrays. Every adjacency arc records the original edge ID. Undirected graphs contain two arcs per input edge. Input conversion and graph construction cost O(n+m) time and memory; returned output is included in the bounds below.

## DSU

Steps: initialise one singleton per vertex; find both roots, attach the smaller tree to the larger root, update its size and decrease the component count. Iterative find first locates the root, then compresses every visited parent directly to it. After each union, append components and largest to states.

Invariant: parent links form rooted trees representing disjoint sets; size at a root is exactly the number of vertices in its set. components counts roots, and largest is their maximum size (zero for an empty graph). Only union of distinct roots changes the partition. Attaching one complete tree preserves set membership and updates sizes exactly, establishing correctness by induction over operations. Compression changes paths but not their root or membership.

Boundaries: self and repeated unions leave counts unchanged. Empty input returns an empty states array, and singleton size is one.

Time: O(n+q alpha(n)) amortised with union by size and path compression. Space: O(n+q), including returned states. find is iterative.

## Kruskal

Steps: sort original edge IDs by weight, then by ID for deterministic ties. Start with singleton DSU sets. Add an edge exactly when its endpoints are in different sets; accumulate cost in long. Return selected original IDs and the remaining component count.

Invariant: accepted edges form a forest, DSU sets are its connected components, and that forest can be extended to a minimum spanning forest. A selected edge crosses a current cut and has minimum available weight across that cut. The cut exchange argument permits including it in an optimum extension. Edges rejected within a set would create a cycle. After scanning all edges, every original connected component is spanned, so the resulting forest is optimal.

Boundaries: loops are rejected, including negative loops. Parallel edges compete normally. Negative weights and ties need no special treatment. Isolates remain singleton components. With n=0, cost and component count are zero and no IDs are selected.

Time: O(n+m log(m+1)); DSU processing is subsumed by sorting. Space: O(n+m), including sorted IDs, sorting workspace, DSU and result.

## Prim

Steps: build undirected adjacency and a lazy priority queue ordered by edge weight. Start at each unvisited vertex, counting a new component. When a vertex becomes visited, offer arcs whose target is still unvisited. Repeatedly remove the minimum arc; discard it if its target is already visited, otherwise accept its original ID and expand that target. Exhaust the heap before restarting.

Invariant: the accepted edges in the current component form a tree over its visited vertices. The heap retains all offered crossing arcs; stale arcs may remain. After discarding stale arcs, its minimum is a minimum-weight edge crossing the visited/unvisited cut. The cut property proves each accepted edge is safe. The heap becomes empty exactly when this connected component is exhausted, so restarting covers all components and isolates. An accepted edge always adds a new vertex, precluding cycles and duplicate selected IDs.

Boundaries: self-loops are never offered to an unvisited target from a visited endpoint; parallel, tied and negative edges work through the same comparator. Empty graphs return zero components.

Time: O(n+m log(m+1)) with lazy heap entries, which can number O(m). Space: O(n+m), including adjacency, heap and forest. Prim is a separate algorithm; it does not call Kruskal or DSU.

## Kosaraju SCC

Steps: construct the original and transposed graphs. First DFS records finish order on the original graph. In decreasing finish order, start DFS on the transpose from each unlabelled vertex, assigning one new label to all newly reached vertices. Scan original edges and collect each distinct cross-label pair in a HashSet of packed long pairs. Store exactly those pairs as condensation.

Invariant: in the first pass, a frame retains its next unprocessed adjacency arc and a vertex is appended only after all descendants finish. In the second pass, each started traversal identifies one SCC among the unassigned vertices. The condensation DAG finish-order theorem ensures the next unassigned maximum-finish SCC cannot reach another unassigned SCC in the transpose. All vertices within it remain mutually reachable, so the traversal assigns exactly that SCC. Repeating proves the labels are exactly mutual-reachability classes. Packing two nonnegative int labels into separate halves of a long is injective; the set removes duplicates without conflating pairs.

Boundaries: loops are internal and omitted from condensation; repeated cross edges appear once. Isolates each receive a separate label. Empty graphs give two empty arrays. Both DFS passes use explicit stacks. Vertices in pass two are labelled when pushed, so each is pushed once.

Time: O(n+m) expected with hash deduplication. Space: O(n+m), including both graphs, stacks, labels, hash entries and condensation. The hash qualification matters; the proof does not assume every hash operation has unconditional constant worst-case time.

## Three-state DFS topological order

Steps: process every unvisited vertex using states 0=unvisited, 1=active, 2=finished. An explicit stack stores both vertex and next arc. An edge to state 1 rejects the whole graph. Otherwise append vertices on finish and reverse that sequence.

Invariant: active vertices are exactly the DFS ancestry stack. An edge to an active ancestor closes a directed cycle, including a self-loop. Conversely, DFS of any directed cycle eventually encounters a back edge. Without a cycle, for each edge u->v, v finishes before u: either DFS explores it before finishing u, or v was already finished. Reversing finish order therefore places u before v and lists every vertex once.

Boundaries: all components are checked, including isolated vertices and disconnected cycles. Edges to state 2 are harmless. Parallel edges do not alter validity. Empty graphs return true and an empty order.

Time and space: O(n+m). The next-arc cursor prevents rescanning neighbours after returning from a child. The explicit O(n) stack supports a 100,000-vertex chain without recursion or StackOverflowError.

## DAG shortest paths and route reconstruction

Steps: perform the same whole-graph three-state DFS before any relaxation. If cyclic, return false and two empty arrays. Otherwise initialise source to 0, every other distance to null and all parents to -1. Process reverse finish order. For each reachable u, relax each outgoing edge and store its original ID on strict improvement.

Invariant: when a vertex is processed, its distance is the optimum among all source paths to it, or null if unreachable. Every incoming predecessor appears earlier in topological order; all paths into the vertex have therefore already been considered. Relaxation considers their last edges, proving the optimum by induction. Parents record incoming edges achieving those distances. They go backwards in a topological order and consequently terminate at source. Skipping null distances prevents creating spurious routes from unreachable vertices. Negative weights are safe because acyclicity prevents revisiting vertices along a path.

Boundaries: cycles anywhere invalidate the request, even if unreachable from source. A finite distance -1 is distinct from null. Parallel and tied edges can produce different valid parents; strict comparison retains the first selected optimum. The source keeps distance 0 and parent -1: in a DAG no reachable predecessor can point back to the source. A singleton returns [0] and [-1]. A simple path has at most n-1 edges, so magnitudes are at most 99,999,000,000,000 within the stated limits, safely inside signed long; forest totals have the same bound.

Time and space: O(n+m), including whole-graph detection, relaxation, adjacency and result arrays. Nullable distances use Long[].

reconstructRoute(source,target,edges,distance,parentEdge) assumes the matching valid acyclic result. It returns [] for an unreachable target. Otherwise it follows incoming parent edge IDs to source, appends vertices and reverses the list. Each step moves to the predecessor on the certified shortest path; termination and direction follow from the parent invariant. Time and output space are O(L). The helper does not add another JSON task.

### Actual helper calls

The GraphChecks.routes test runs dag on n=5, source=0 and edges:

```
0: [0,1,3]
1: [0,2,8]
2: [1,2,-7]
3: [2,3,4]
4: [0,1,3]
```

The resulting parent array was [-1,4,2,3,-1]. Using those distance and parent arrays, the calls to reconstructRoute produced:

| source,target | Route | Selected edge IDs in forward order | Verified total |
|---|---|---|---|
| 0,3 | [0,1,2,3] | [4,2,3] | 3-7+4=0 |
| 0,0 | [0] | [] | 0 |
| 0,4 | [] | [] | unreachable, distance=null |
| 0,1 | [0,1] | [4] | 3 |

IDs 0 and 4 are tied parallel edges. ID 4 was selected because the head-linked adjacency scans it before ID 0. Either is a valid witness. The test verifies that each selected edge starts at the previous route vertex, ends at the next vertex, and that the sum equals the returned distance. Actual stdout is stored in tests/routes_actual.txt.

## Test evidence

The 36 supplied cases passed, followed by two complete batches in one process; setup ping passed separately. tests/public_actual.txt records the public run. tests/verify.py adds 462 independently specified/generated requests, then repeats that mixed batch in the same process (924 total). Fixed seed 2515 is used for the generated small cases.

Independent expected results use direct set merging for DSU, exhaustive forest subsets for small MSTs, Floyd-Warshall reachability for SCC membership and cycle detection, and Bellman-Ford relaxation for acyclic shortest distances. These do not rely on the Java algorithms' output. Returned forests, SCC condensation, topological precedence and shortest-path witnesses are checked using the template semantic validator. Expected JSON records deliberately omit arbitrary witness choices; they specify the optimum/partition/acyclicity that a valid witness must support. Actual output files retain full witnesses.

The small cases include n=0 where allowed, repeated/self unions, isolates, disconnected graphs, loops, parallel/tied edges, negative weights, 64-bit distances, null distances and unreachable cycles. tests/witness_checks.json contains an alternative valid answer and a deliberately invalid answer for each of Kruskal, Prim, SCC, topo and DAG. All five alternatives were accepted and all five invalid witnesses rejected. These invalid answers test the validator; they are not deliberate mistakes in the submitted algorithms.

Nine further scale requests run in one JSON Lines process with -Xmx384m. A 100,000-vertex chain is extended to exactly 250,000 rows by repeated 0->99999 edges (or repeated DSU operations). Expected DSU state after chain union i is [n-i-1,i+2], then [1,n]. Each MSF must select the n-1 chain edges of weight -10^9, cost -(n-1)*10^9. Directed chain reachability is one-way, so each SCC is a singleton; each condensation pair is checked. Topological precedence must hold for every edge. DAG distances are -i*10^9. Another 100,000-vertex directed cycle must form one SCC and be rejected by topo and DAG. Inputs, expected specifications and actual results are saved as scale_* files. This checks both full limits and deep traversal; the process completed in 1.416 seconds locally, including startup, JSON input/output and pipe handling, not compilation. This time is evidence about this run, not a grading guarantee.

## Kruskal versus Prim benchmark

Environment: Windows 11 10.0 amd64, AMD Ryzen 5 7535HS with Radeon Graphics, 12 logical processors reported, OpenJDK 25.0.1+8-27; source compiled for Java 17. JVM command includes -Xmx384m. Timer: System.nanoTime converted to milliseconds. See tests/GraphChecks.java and tests/benchmark_actual.txt.

Generator: Java Random(2515L+n), same graph object for both algorithms. The first n-1 edges are the consecutive chain i--(i+1), ensuring connectivity; remaining endpoints are uniform nextInt(n). Each weight is nextInt(2000000001)-1000000000, including negative weights. m=min(250000,4*n). For each size, one correctness call per algorithm precedes three warm-up calls per algorithm. Three measured calls per method follow, alternating which algorithm is measured first in each round. Correctness checks are outside the measured interval.

Boundary: each sample times only the complete Algorithms.kruskal or Algorithms.prim method call on an already built request Map. Both include Input.edges conversion from lists to int[][] and their own sorting/DSU or adjacency/heap construction plus result construction. Both exclude random graph generation, request-list construction, JSON parsing/serialisation, process startup, printing, independent witness validation and compilation. JVM allocation and any garbage collection during a method call are naturally included. A separate process per sample is not used.

Before timing and after every measured call, the harness verifies edge uniqueness, no cycles, spanning of every input connection, reported components and exact selected-weight sum. The two algorithms must agree on optimum cost; small-case exhaustive tests independently support their optimum correctness.

| n | m | Kruskal samples (ms) | Median (ms) | Prim samples (ms) | Median (ms) |
|---|---|---|---|---|---|
| 1,000 | 4,000 | 1.7270, 1.5058, 1.4712 | 1.5058 | 2.8194, 1.9369, 2.1920 | 2.1920 |
| 10,000 | 40,000 | 46.6056, 14.3944, 14.1098 | 14.3944 | 42.2129, 29.7276, 31.6111 | 31.6111 |
| 100,000 | 250,000 | 226.8434, 241.5726, 237.2116 | 237.2116 | 682.3184, 684.8816, 667.7707 | 682.3184 |

Verified costs respectively: -714590111777, -7139826933422 and -54671134988764; both methods reported one component. Kruskal was faster on these sparse generated graphs. Prim performs lazy heap operations over offered edges, while sorting once was cheaper here. This is an observation for this implementation and generator, not a proof that Kruskal is always faster. Three samples and short warm-up leave JIT, GC and scheduler noise; the high first medium-size Kruskal sample illustrates this. Both retain the stated O(n+m log(m+1)) bound, and the largest case has a different m/n ratio.

## Sources

The supplied RZhaniya/daa-assignment-3-graphs template, README and Assignment_3_EN.pdf define the contracts, algorithm requirements, adapters and public validator. Source: https://github.com/RZhaniya/daa-assignment-3-graphs . No third-party graph library or external solution code is used.

## Submission

submission.json selects Java. This local project includes the selected source, tests, analysis and commands. The course requires an exact commit URL from the student's own GitHub repository in Moodle Online text. A local archive alone is not that submission. No Moodle submission or successful remote Actions run is claimed here. Create a repository from the supplied template, push these changes, check Public checks, then submit the exact commit URL and verify the Moodle status. Do not push to the instructor's template repository.
