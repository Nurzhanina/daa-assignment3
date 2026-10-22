# Own tests

Run commands from the repository root; see ANALYSIS.md for build instructions.

- `python tests/verify.py`: generates and runs small and scale JSON Lines files, compares against independent expected specifications, and records complete actual outputs.
- `java -Xmx384m -cp .build/Java GraphChecks`: calls the route helper and verifies direction and costs.
- `java -Xmx384m -cp .build/Java GraphChecks benchmark`: generates fixed-seed graphs, validates forests, warms up and records three timings per method at three sizes.
- `python tools/run.py --setup` and `python tools/run.py --check`: official transport and public checks.

The small_* and scale_* files contain requests, expected specifications and actual results. Non-unique answers use the semantic validator, not byte equality. Expected specifications supply independently derived values/partitions; valid arbitrary witnesses need not equal another run's witness. verify.py regenerates all inputs from seed 2515 or the documented chain formulas. witness_checks.json stores alternative valid and invalid witness examples and their check outcomes. own_results.txt, routes_actual.txt, benchmark_actual.txt and public_actual.txt record executed checks. Full explanations and mathematical justifications are in ANALYSIS.md.
