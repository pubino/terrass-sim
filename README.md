# Territory Assignment Optimizer

Part of [Prof. Robert Vanderbei's Java Applications](https://pubino.github.io/vanderbei-apps#terrass-sim).

A Java application for optimizing territory assignments using space-filling curves and local improvement heuristics. Originally based on Robert Vanderbei's work from Princeton University.

## Run in your browser

[Open Territory Assignment](https://pubino.github.io/terrass-sim/). The default
run loads 41,352 historical ZIP locations and divides them into 500 territories.
Choose a territory below the map to see its ZIP count and weight against the target.
Change the number of territories, bias schedule, or geographic bounds, then
select **Optimize**. Select **Stop** to cancel an in-progress run.

The Python model runs in a Pyodide worker. It ports the Java projection,
space-filling curve, boundary reassignment, and territory center updates. The
browser uses Java's pseudorandom generator with an editable seed (default 524).
It orders exact curve-hash ties by descending ZIP code so a seed is
reproducible. The original Java orders those ties by object identity.

The build verifies and packages the historical inputs from the
[original Princeton applet](https://vanderbei.princeton.edu/JAVA/terrass/).
You can also load local copies of these file formats. Local files stay in the
browser. Omit the optional adjacency file to generate a network using the
original four-pass method.

### Browser development

```sh
docker-compose run --build --rm -T js-check
docker-compose run --build --rm -T test
docker-compose up --build preview
```

The preview serves http://localhost:8000. The tests compare all historical
assignments, center coordinates, directed adjacency, and optimization passes
with the original Java solver. They also exercise browser startup, cancellation,
and local files. For native development, use `uv sync --locked --python 3.13.12`,
`uv run playwright install --only-shell chromium`, and
`uv run python scripts/build_site.py` before running
`uv run python -m unittest discover -s tests -v` with JDK 25 on your path.

## Overview

This application solves the territory assignment problem: dividing a geographic region into balanced territories based on specified criteria. It uses:

- **Space-filling curves**: Orders geographic points along a continuous curve that preserves locality
- **Local improvement heuristics**: Iteratively reassigns ZIPs to improve balance and compactness

The algorithm balances territories to have equal "weight" (e.g., population, sales potential) while minimizing geographic dispersion.

## Features

- Visual display of territory assignments on a map
- Configurable number of territories
- Adjustable optimization parameters (bias, reduction factor)
- Geographic bounds filtering
- Progress output in UI panel

## Requirements

- Java 14+ (for running the JAR)
- Java 14+ with `jpackage` (for building macOS app bundle)

## Data Files Required

The Java application reads these whitespace-delimited files from the current
working directory. Each starts with a record count.

| File | Fields |
| --- | --- |
| [zlls.dat](https://vanderbei.princeton.edu/JAVA/terrass/zlls.dat) | ZIP, latitude, longitude, state code |
| [zta.dat](https://vanderbei.princeton.edu/JAVA/terrass/zta.dat) | ZIP, historical territory ID, weight |
| [mknet/arcs.dat](https://vanderbei.princeton.edu/JAVA/terrass/mknet/arcs.dat) | Source ZIP, neighboring ZIP (optional) |

Keep leading zeros in ZIPs and list locations in ZIP order for the Java
algorithm's adjacency indexing. Geographic bounds are exclusive. Java replaces
missing and zero weights with 0.001 and optimizes new territory assignments
starting from the space-filling order.

## Building

```bash
# Build the JAR file
make jar

# Build macOS .app bundle (unsigned)
make app
```

## Running

```bash
# Run the JAR
make run

# Or run directly
java -jar dist/TerritoryAssignment.jar
```

## Usage

1. Set the number of territories to create
2. Configure bias parameters:
   - **Bias Start**: Initial bias weight for balancing
   - **Bias End**: Final bias weight (lower = more balanced)
   - **Reduction**: Factor to reduce bias each iteration
3. Set geographic bounds (latitude/longitude min/max)
4. Click "Start" to begin optimization
5. Click "Stop" to halt the process
6. View progress (bias level, changes per iteration) in output panel

## Parameters

- **Number of Terrs**: Target number of territories
- **Bias Start/End**: Controls the trade-off between balance and compactness
- **Reduction**: How quickly bias decreases (larger = faster convergence)
- **Frac**: Fraction parameter for local search
- **Num Changes**: Minimum changes threshold to continue iterating

## Project Structure

```
terrass-sim/
├── terrass/
│   ├── TerrAss.java       # Main application and optimization
│   ├── POINT.java         # Point/coordinate data structure
│   └── Sfc.java           # Space-filling curve implementation
├── myutil/
│   ├── AppletFrame.java   # Applet-to-Frame wrapper
│   ├── Comparable.java    # Comparison interface
│   ├── FileIO.java        # File I/O utilities
│   ├── Format.java        # Number formatting
│   ├── GL.java            # Graphics library helpers
│   ├── GridPanel.java     # Grid layout panel
│   ├── Memory.java        # Memory utilities
│   ├── Search.java        # Search algorithms
│   ├── SL.java            # String parsing utilities
│   └── Sort.java          # Sorting algorithms
├── Makefile               # Build configuration
└── .github/workflows/     # CI/CD configuration
```

## Algorithm

The territory assignment algorithm:

1. **Initialization**: Orders points using a space-filling curve (preserves geographic locality)
2. **Initial Assignment**: Divides ordered points into equal-weight segments
3. **Local Improvement**:
   - Identifies boundary points between territories
   - Evaluates moving one ZIP at a time to improve the objective
   - Objective balances territory weights against geographic compactness
4. **Bias Reduction**: Gradually reduces bias to emphasize balance over compactness
5. **Termination**: Stops when changes fall below threshold
