# Closest Pair of Points — Java solution

Both algorithms, random input generation, correctness tests, and runtime measurements are implemented in **ClosestPair.java**. HTML/JavaScript displays the Java-generated results and recorded recursion. No Python or external libraries are required.

## Windows: run it

1. Extract the ZIP. Open the `closest_pair_java` folder.
2. Click File Explorer's address bar, type `cmd`, press Enter.
3. Run the following single command (Java 11 or newer):

```
echo 32 | java ClosestPair.java
```

4. Wait until the folder prompt returns, then open the report:

```
start closest_pair_report.html
```

Change 32 to any nonnegative integer. Start with 10–50 for readable animation.
You may instead run `java ClosestPair.java`, then type N at the explicit prompt.

If Java is not recognized, check your JDK installation with `java -version`.

### Traditional compile-and-run option

```
javac ClosestPair.java
echo 32 | java ClosestPair
```

Keep `ClosestPair.java` and `report_template.html` together and run from their folder. The generated report is self-contained. `sample_report.html` is an example you can open immediately.

## Required output

The first three stdout lines contain the Brute-Force answer, Divide-and-Conquer answer, and each execution time in milliseconds. Distances use six decimal places; coordinates retain their double-precision values. Input prompts and benchmark/test progress go to stderr. N<2 reports N/A because no pair exists. Negative N is rejected.

Outputs: `closest_pair_report.html`, `points.csv`, `benchmark.csv`.

## Visualization

- All points are plotted at fixed coordinates, with equal units on both axes.
- Orange highlights the Brute-Force pair; dashed blue highlights the Divide-and-Conquer pair. Rings identify coincident endpoints for duplicates.
- The line and shaded strip show the active merge, with initial strip width 2d, clipped to the axes. d=0 collapses the strip.
- Reset, Play, Step, Final, speed selection and a timeline slider replay actual Java recursive events. Purple is the current subproblem's answer. Turn off “Show both final closest pairs” to focus on recursion.
- Full animation is recorded for N<=512; larger inputs show all points and the final root strip with playback disabled. This is a report-size limit, not an algorithm limit. Brute Force becomes slow for large N.
- The graph plots actual median Java timings versus N, with a table and a short analysis. Sample results come from the machine used to generate them; run locally to measure your own computer.

## Corner cases and tests

```
java ClosestPair.java --test-only
echo 0 | java ClosestPair.java
echo 1 | java ClosestPair.java
echo 2 | java ClosestPair.java
echo 32 | java ClosestPair.java --mode duplicates
echo 32 | java ClosestPair.java --mode collinear
echo 32 | java ClosestPair.java --mode same-x
echo 32 | java ClosestPair.java --mode same-y
```

1,524 cases include explicit corner cases, all 512 subsets of a 3x3 grid, and 1,000 seeded random integer/floating-point point sets. Tests validate distinct point IDs, membership, pair distances, and agreement before rounding (relative/absolute tolerance 1e-12). A 3-4-5 example independently checks the distance formula. Every input and benchmark trial is also verified. Different pairs with equal minimum distance are valid. Finite testing provides evidence, not a claim to have tested every possible input.

## Performance settings

```
echo 32 | java ClosestPair.java --sizes 100,500,1000,2000,4000 --repeats 7
echo 32 | java ClosestPair.java --seed 17
```

Defaults: seed 42, coordinates in [-100,100), sizes 100/250/500/1000/2000, five trials per size. Both algorithms receive identical points within each trial. Trials alternate order and use new reproducible datasets. `System.nanoTime()` measures each call. Initial D&C sorting is included; random generation, validation, recording, rendering and file output are excluded. A 200-iteration warm-up precedes measurement, but JVM JIT/GC and operating-system scheduling can still cause noise. These are educational measurements, not a rigorous JMH benchmark. The requested N's first two calls are reported separately from the repeated benchmark medians.

## Algorithms

Brute Force checks N(N-1)/2 unordered pairs: O(N^2) time and O(1) additional live algorithm space, excluding input.

Divide and Conquer presorts once by x and y. At each recursion, split the x-order by index; partition the y-order using unique IDs to handle tied x values and duplicates. Recurse on both halves and let d be the smaller half-distance. Check the strip in y-order, at most seven subsequent candidates per point. The divide-and-conquer recurrence has linear work per level, yielding O(N log N) time (using expected constant-time hash membership) and O(N) peak additional live space. Trace recording adds O(N log N) point IDs and is excluded from timing.

Correctness: any best pair is either within a half or crosses the split. Recursion covers the first case. Any cross-half improvement must lie inside the strip of width 2d. The geometric packing bound permits checking the next seven y-ordered points. A distance of zero is already optimal. Strict comparisons can return any one tied optimum.

## GitHub upload

Upload the contents of this folder to your repository. The main source is `ClosestPair.java`; `report_template.html` must accompany it. Include this README and the sample report/CSVs if desired. Generated `.class` files are not needed. Python files from the earlier version are not part of this Java project.
