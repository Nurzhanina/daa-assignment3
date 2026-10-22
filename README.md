# Assignment 3: Graph Algorithms

Student: Nurzhan Zholaman
Student/group identifier: SE-2515
Language: Java 17

Implemented methods: DSU, Kruskal, Prim, Kosaraju SCC, DFS topological ordering and DAG shortest paths, including route reconstruction.

## Build and run

```sh
javac --release 17 -encoding UTF-8 -d .build/Java src/Java/*.java
java -Xmx384m -cp .build/Java Main
python tools/run.py --setup
python tools/run.py --check
python tests/verify.py
```

In IntelliJ, open this folder, choose JDK 17 or newer, mark src/Java as Sources Root and run Main. The program reads UTF-8 JSON Lines from standard input. On Windows, Run.ps1 compiles and launches it.

ANALYSIS.md explains correctness, complexity, test evidence, route reconstruction and benchmark measurements. tests/ contains full inputs, expected specifications, actual outputs and reproducible test tools.

The project uses the supplied adapters and public checks from [the course template](https://github.com/RZhaniya/daa-assignment-3-graphs). The complete assignment requirements remain available there.

Submission requires an exact commit URL in the student's own repository and a completed Moodle submission. No remote submission is included in this archive.
