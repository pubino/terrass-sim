import subprocess
import tempfile
import unittest
from pathlib import Path
from tempfile import TemporaryDirectory

from web.simulation import DEFAULTS, Assignment, Settings, read_points

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "build/site/data"


class TerritoryTest(unittest.TestCase):
    classes: TemporaryDirectory[str]

    @classmethod
    def setUpClass(cls) -> None:
        cls.classes = TemporaryDirectory()
        cls.addClassCleanup(cls.classes.cleanup)
        subprocess.run(
            [
                "javac",
                "--release",
                "25",
                "-d",
                cls.classes.name,
                *sorted((ROOT / "terrass").glob("*.java")),
                *sorted((ROOT / "myutil").glob("*.java")),
                ROOT / "tests/TerritoryReference.java",
            ],
            check=True,
            timeout=60,
        )

    def compare_with_java(
        self,
        data: Path,
        options: dict[str, int | float],
        network: bool,
    ) -> None:
        settings = Settings.read(options)
        args = (
            settings.seed,
            settings.count,
            settings.bias_start,
            settings.bias_end,
            settings.reduction,
            settings.fraction,
            settings.min_changes,
            settings.lat_min,
            settings.lat_max,
            settings.lon_min,
            settings.lon_max,
        )
        output = subprocess.check_output(
            [
                "java",
                "-Djava.awt.headless=true",
                "--add-opens",
                "java.base/java.lang=ALL-UNNAMED",
                "-cp",
                self.classes.name,
                "terrass.TerritoryReference",
                *map(str, args),
            ],
            cwd=data,
            text=True,
            timeout=150,
        )
        trace: dict[str, list[list[str]]] = {
            name: [] for name in ("zip", "adj", "territory", "progress")
        }
        for line in output.splitlines():
            key, *row = line.split("\t")
            if key in trace:
                trace[key].append(row)

        points, indexes = read_points(
            (data / "zlls.dat").read_text(), (data / "zta.dat").read_text(), settings
        )
        self.assertEqual(len(points), len(trace["zip"]))
        # Java breaks equal curve hashes with the in-process ZIPDATA identity string.
        for point, row in zip(points, trace["zip"], strict=True):
            self.assertEqual(point.code, row[0])
            point.tie_order = row[5]
        graph = (data / "mknet/arcs.dat").read_text() if network else None
        model = Assignment(points, indexes, settings, graph)
        progress: list[tuple[float, int]] = []
        model.optimize(lambda bias, changes: progress.append((bias, changes)))
        expected_progress = []
        for (entry,) in trace["progress"]:
            value = entry.split(" = ")[1]
            if entry.startswith("bias"):
                bias = float(value)
            else:
                expected_progress.append((bias, int(value)))

        self.assertEqual(progress, expected_progress)
        self.assertEqual(model.neighbors, [list(map(int, row)) for row in trace["adj"]])
        self.assertEqual(
            [point.territory for point in points], [int(row[1]) for row in trace["zip"]]
        )
        for point, row in zip(points, trace["zip"], strict=True):
            self.assertEqual(point.weight, float(row[2]))
            for actual, reference in zip((point.x, point.y), row[3:5], strict=True):
                self.assertAlmostEqual(actual, float(reference), delta=1e-10)
        for territory, row in zip(model.territories, trace["territory"], strict=True):
            self.assertEqual(len(territory.members), int(row[4]))
            self.assertEqual(model.target, float(row[5]))
            actuals = (
                territory.weight,
                territory.lat,
                territory.lon,
                territory.distance,
            )
            for actual, expected in zip(actuals, map(float, row[:4]), strict=True):
                self.assertAlmostEqual(
                    actual, expected, delta=max(1e-8, abs(expected) * 1e-9)
                )

    def test_princeton_files_match_java(self) -> None:
        self.compare_with_java(DATA, DEFAULTS, True)

    def test_generated_network_matches_java(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            data = Path(directory)
            rows = [
                ("01004", 37.0, -93.0, 0),
                ("01001", 42.0, -82.2, 22),
                ("01003", 38.5, -94.0, 8),
                ("01002", 34.1, -108.3, 16),
                ("01005", 34.1, -110.0, 13),
                ("01006", 38.1, -96.7, 15),
                ("01007", 40.5, -78.7, 13),
                ("01008", 44.9, -115.0, 18),
                ("01009", 34.6, -89.0, 11),
                ("01010", 31.0, -95.7, 25),
                ("01011", 32.0, -81.5, 5),
                ("01012", 30.0, -82.5, 11),
                ("01013", 20.0, -80.0, 100),
                ("01014", 38.4, -93.1, 9),
            ]
            header = f"{len(rows)}\n"
            (data / "zlls.dat").write_text(
                header
                + "".join(f"{code} {lat} {lon} AA\n" for code, lat, lon, _ in rows)
            )
            (data / "zta.dat").write_text(
                header
                + "".join(f"{code} REGION {weight}\n" for code, _, _, weight in rows)
            )
            self.compare_with_java(
                data, DEFAULTS | {"count": 3, "seed": -82, "fraction": 0.15}, False
            )

    def test_rejects_missing_and_invalid_data(self) -> None:
        settings = Settings.read(DEFAULTS | {"count": 2})
        with self.assertRaisesRegex(ValueError, "end must"):
            Settings.read(DEFAULTS | {"bias_end": 0})
        with self.assertRaisesRegex(ValueError, "record count"):
            read_points("2\n01001 44 -98 AA\n", "0", settings)
        with self.assertRaisesRegex(ValueError, "duplicate ZIP"):
            read_points("2\n01001 44 -98 AA\n01001 40 -99 AA\n", "0", settings)
        with self.assertRaisesRegex(ValueError, "weight"):
            read_points(
                "2\n01001 44 -98 AA\n01002 40 -99 AA\n", "1\n01001 R nan\n", settings
            )

    def test_single_point_has_finite_projection_and_weight(self) -> None:
        settings = Settings.read(DEFAULTS | {"count": 1})
        points, indexes = read_points("1\n00001 44 -98 AA\n", "0", settings)
        model = Assignment(points, indexes, settings, None)
        model.optimize(lambda *_: None)
        self.assertEqual((points[0].x, points[0].y), (0.5, 0.5))
        self.assertEqual(points[0].territory, 0)
        self.assertEqual(model.neighbors, [[]])
        self.assertAlmostEqual(model.target, 0.001)
