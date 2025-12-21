# Territory Assignment Optimizer

A Java application for optimizing territory assignments using space-filling curves and local improvement heuristics. Originally based on Robert Vanderbei's work from Princeton University.

## Overview

This application solves the territory assignment problem: dividing a geographic region into balanced territories based on specified criteria. It uses:

- **Space-filling curves**: Orders geographic points along a continuous curve that preserves locality
- **Local improvement heuristics**: Iteratively swaps assignments to improve balance and compactness

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
   - Evaluates swapping assignments to improve objective
   - Objective balances territory weights against geographic compactness
4. **Bias Reduction**: Gradually reduces bias to emphasize balance over compactness
5. **Termination**: Stops when changes fall below threshold

## License

Based on original work by Robert Vanderbei, Princeton University.
