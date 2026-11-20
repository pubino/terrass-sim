"""Vanderbei's national territory assignment model."""

import json
from collections.abc import Callable, Iterator
from dataclasses import dataclass, field
from itertools import pairwise
from math import acos, atan2, cos, exp, floor, isfinite, log, pi, sin, sqrt
from operator import attrgetter
from typing import Any

EPS = 0.001
RADIUS = 24000 / (2 * pi)
RAD = pi / 180
HUGE = 1e300
DEFAULTS: dict[str, int | float] = {
    "count": 500,
    "seed": 524,
    "bias_start": 500,
    "bias_end": 100,
    "reduction": 2,
    "fraction": 0,
    "min_changes": 0,
    "lat_min": 20,
    "lat_max": 60,
    "lon_min": -130,
    "lon_max": -60,
}
Vector = tuple[float, float, float]


def number(raw: Any, minimum: float, maximum: float, label: str) -> float:
    if type(raw) not in (float, int, str):
        raise ValueError(f"{label} is required.")
    try:
        value = float(raw)
    except (ValueError, OverflowError) as exc:
        raise ValueError(f"{label} must be numeric.") from exc
    if not isfinite(value) or not minimum <= value <= maximum:
        raise ValueError(f"{label} must be between {minimum:g} and {maximum:g}.")
    return value


def integer(raw: Any, minimum: int, maximum: int, label: str) -> int:
    value = number(raw, minimum, maximum, label)
    if not value.is_integer():
        raise ValueError(f"{label} must be a whole number.")
    return int(value)


@dataclass(frozen=True, slots=True)
class Settings:
    count: int
    seed: int
    bias_start: float
    bias_end: float
    reduction: float
    fraction: float
    min_changes: int
    lat_min: float
    lat_max: float
    lon_min: float
    lon_max: float

    @classmethod
    def read(cls, values: dict[str, Any]) -> "Settings":
        start = number(values.get("bias_start"), 1e-9, 1e9, "Bias start")
        end = number(values.get("bias_end"), 1e-9, start, "Bias end")
        reduction = number(values.get("reduction"), 1, 1e9, "Bias reduction")
        if reduction == 1 or (log(start) - log(end)) / log(reduction) >= 128:
            raise ValueError(
                "Bias reduction must exceed 1 and allow at most 128 stages."
            )
        lat_min = number(values.get("lat_min"), -90, 90, "Minimum latitude")
        lat_max = number(values.get("lat_max"), -90, 90, "Maximum latitude")
        lon_min = number(values.get("lon_min"), -180, 180, "Minimum longitude")
        lon_max = number(values.get("lon_max"), -180, 180, "Maximum longitude")
        if lat_min >= lat_max or lon_min >= lon_max:
            raise ValueError("Each minimum bound must be less than its maximum.")
        return cls(
            integer(values.get("count"), 1, 100_000, "Territory count"),
            integer(values.get("seed"), -(1 << 31), (1 << 31) - 1, "Seed"),
            start,
            end,
            reduction,
            number(values.get("fraction"), 0, 1, "Change fraction"),
            integer(values.get("min_changes"), 0, 1_000_000, "Minimum changes"),
            lat_min,
            lat_max,
            lon_min,
            lon_max,
        )


def sphere(lat: float, lon: float, radius: float) -> Vector:
    return (
        radius * cos(lat * RAD) * cos(lon * RAD),
        radius * cos(lat * RAD) * sin(lon * RAD),
        radius * sin(lat * RAD),
    )


def coordinates(x: float, y: float, z: float) -> tuple[float, float]:
    return atan2(z, sqrt(x * x + y * y)) / RAD, atan2(y, x) / RAD


def distance(a: Vector, b: Vector) -> float:
    # The larger denominator in the Java distance keeps even coincident ZIPs
    # a small positive distance apart.
    dot = a[0] * b[0] + a[1] * b[1] + a[2] * b[2]
    return RADIUS * acos(dot / ((RADIUS + EPS) * (RADIUS + EPS)))


@dataclass(slots=True, eq=False)
class ZipPoint:
    code: str
    lat: float
    lon: float
    weight: float = EPS
    cartesian: Vector = (0, 0, 0)
    x: float = 0
    y: float = 0
    territory: int = -1
    tie_order: str = ""


@dataclass(slots=True)
class Territory:
    members: list[ZipPoint] = field(default_factory=list)
    weight: float = EPS
    lat: float = 0
    lon: float = 0
    cartesian: Vector = (RADIUS, 0, 0)
    distance: float = 0
    until_update: int = 0

    def update_center(self, initial: bool = False) -> None:
        if self.until_update:
            self.until_update -= 1
            return
        if not self.members:
            return
        self.until_update = int((0.10 if initial else 0.02) * len(self.members))
        if initial:
            x = y = z = 0.0
            for member in self.members:
                cx, cy, cz = sphere(member.lat, member.lon, member.weight * RADIUS)
                x += cx
                y += cy
                z += cz
            self.lat, self.lon = coordinates(
                x / self.weight, y / self.weight, z / self.weight
            )
        center = sphere(self.lat, self.lon, RADIUS)
        iterations = (5 if len(self.members) > 1 else 0) if initial else 1
        x, y, z = center
        for _ in range(iterations):
            nx = ny = nz = denominator = 0.0
            for member in self.members:
                cx, cy, cz = member.cartesian
                dx, dy, dz = cx - x, cy - y, cz - z
                d = sqrt(dx * dx + dy * dy + dz * dz) + EPS
                nx += member.weight * cx / d
                ny += member.weight * cy / d
                nz += member.weight * cz / d
                denominator += member.weight / d
            x, y, z = nx / denominator, ny / denominator, nz / denominator
        if iterations:
            self.lat, self.lon = coordinates(x, y, z)
        self.cartesian = sphere(self.lat, self.lon, RADIUS)

        # The original initialization measures singleton offsets from (0, 0).
        # Subsequent moves use the stored center and update that running distance.
        origin = self.cartesian if iterations else (RADIUS, 0, 0)
        self.distance = 0.0
        for member in self.members:
            self.distance += member.weight * distance(origin, member.cartesian)


def records(text: str, width: int, maximum: int, label: str) -> Iterator[list[str]]:
    tokens = text.split()
    if not tokens:
        raise ValueError(f"{label} is empty.")
    try:
        count = int(tokens[0])
    except ValueError as exc:
        raise ValueError(f"{label} must begin with a record count.") from exc
    if not 0 <= count <= maximum or len(tokens) != 1 + count * width:
        raise ValueError(f"{label} has an invalid record count.")
    for index in range(1, len(tokens), width):
        yield tokens[index : index + width]


def read_points(
    locations: str, weights: str, settings: Settings
) -> tuple[list[ZipPoint], dict[str, int]]:
    by_zip: dict[str, ZipPoint] = {}
    indexes: dict[str, int] = {}
    seen: set[str] = set()
    for code, raw_lat, raw_lon, _ in records(locations, 4, 100_000, "zlls.dat"):
        if code in seen:
            raise ValueError("zlls.dat contains a duplicate ZIP.")
        seen.add(code)
        lat = number(raw_lat, -90, 90, "ZIP latitude")
        lon = number(raw_lon, -180, 180, "ZIP longitude")
        if (
            settings.lat_min < lat < settings.lat_max
            and settings.lon_min < lon < settings.lon_max
        ):
            indexes[code] = len(indexes)
            by_zip[code] = ZipPoint(
                code, lat, lon, cartesian=sphere(lat, lon, RADIUS), tie_order=code
            )
    if not by_zip:
        raise ValueError("No ZIPs fall inside the selected bounds.")
    if settings.count > len(by_zip):
        raise ValueError(f"Set at most {len(by_zip):,} territories for these bounds.")
    for code, _, raw_weight in records(weights, 3, 100_000, "zta.dat"):
        weight = number(raw_weight, 0, 1e15, "ZIP weight")
        if code in by_zip:
            by_zip[code].weight = weight or EPS
    return sorted(by_zip.values(), key=attrgetter("code")), indexes


def project(points: list[ZipPoint]) -> None:
    vectors = [
        (
            cos(2 * pi * p.lat / 360) * cos(2 * pi * p.lon / 360),
            cos(2 * pi * p.lat / 360) * sin(2 * pi * p.lon / 360),
            sin(2 * pi * p.lat / 360),
        )
        for p in points
    ]
    x = y = z = 0.0
    for vx, vy, vz in vectors:
        x += vx
        y += vy
        z += vz
    x, y, z = x / len(points), y / len(points), z / len(points)
    theta = atan2(y, x)
    phi = atan2(sqrt(x * x + y * y), z)
    east_x, east_y = -sin(theta), cos(theta)
    north_x, north_y, north_z = -cos(phi) * cos(theta), -cos(phi) * sin(theta), sin(phi)
    for point, (vx, vy, vz) in zip(points, vectors, strict=True):
        point.x = vx * east_x + vy * east_y
        point.y = vx * north_x + vy * north_y + vz * north_z
    min_x = min(p.x for p in points)
    max_x = max(p.x for p in points)
    min_y = min(p.y for p in points)
    max_y = max(p.y for p in points)
    max_x += 0.11 * (max_x - min_x)
    min_x -= 0.11 * (max_x - min_x)
    max_y += 0.11 * (max_y - min_y)
    min_y -= 0.11 * (max_y - min_y)
    scale = max(max_x - min_x, max_y - min_y)
    for point in points:
        point.x = (point.x - min_x) / scale if scale else 0.5
        point.y = (point.y - min_y) / scale if scale else 0.5


class Curve:
    """The ternary space-filling order in Sfc.java, with a Java Random seed."""

    MULTIPLIER = 0x5DEECE66D
    MASK = (1 << 48) - 1
    STEPS = ((0, 0), (1, 1), (2, 0), (5, 3), (4, 2), (3, 3), (6, 0), (7, 1), (8, 0))

    def __init__(self, points: list[ZipPoint], seed: int):
        self.rng = (seed ^ self.MULTIPLIER) & self.MASK
        self.xshift = self.yshift = 0.0
        self.depth = int(log(len(points) + 1) / log(9.0) + 3)
        self.points = sorted(points, key=attrgetter("tie_order"), reverse=True)

    def random(self) -> float:
        self.rng = (self.rng * self.MULTIPLIER + 11) & self.MASK
        high = self.rng >> 22
        self.rng = (self.rng * self.MULTIPLIER + 11) & self.MASK
        return ((high << 27) + (self.rng >> 21)) / (1 << 53)

    def road(self) -> list[ZipPoint]:
        xs, ys = 0.20 * (self.random() - 1.0), 0.20 * (self.random() - 1.0)
        dx, dy = xs - self.xshift, ys - self.yshift
        self.xshift, self.yshift = xs, ys
        y_major, swap = self.random() >= 0.5, self.random() >= 0.5

        def key(point: ZipPoint) -> float:
            x, y = point.x + dx, point.y + dy
            if y_major:
                x, y = y, x
            corner, value, scale = 0, 0.0, 1.0
            for _ in range(self.depth):
                if corner == 1:
                    x = 1 - x
                    if swap:
                        x, y = y, x
                elif corner == 2:
                    x, y = 1 - x, 1 - y
                elif corner == 3:
                    y = 1 - y
                    if swap:
                        x, y = y, x
                jx, jy = floor(3 * x), floor(3 * y)
                scale /= 9
                cell = 3 * jx + jy
                if 0 <= cell < 9:
                    digit, corner = self.STEPS[cell]
                    value += digit * scale
                x, y = 3 * x - jx, 3 * y - jy
            return value

        return sorted(self.points, key=key)


def neighbor_lists(
    points: list[ZipPoint], indexes: dict[str, int], curve: Curve, arcs: str | None
) -> list[list[int]]:
    neighbors: list[list[int]] = [[] for _ in points]
    # Legacy arc indices are assigned before the ZIP array is sorted.
    if arcs is not None:
        for from_zip, to_zip in records(arcs, 2, 1_000_000, "arcs.dat"):
            if from_zip in indexes and to_zip in indexes:
                neighbors[indexes[from_zip]].append(indexes[to_zip])
        return neighbors
    for _ in range(4):
        road = [indexes[point.code] for point in curve.road()]
        for left, right in pairwise(road):
            if right not in neighbors[left]:
                neighbors[left].append(right)
                neighbors[right].append(left)
    for index, adjacent in enumerate(neighbors):
        if not adjacent:
            continue
        dists = [
            distance(points[index].cartesian, points[j].cartesian) for j in adjacent
        ]
        total = 0.0
        for d in dists:
            total += d
        cutoff = 4 * (total / len(adjacent)) / 3
        neighbors[index] = [
            j for j, d in zip(adjacent, dists, strict=True) if d <= cutoff
        ]
    return neighbors


class Assignment:
    def __init__(
        self,
        points: list[ZipPoint],
        indexes: dict[str, int],
        settings: Settings,
        arcs: str | None,
    ):
        self.points = points
        self.settings = settings
        project(points)
        curve = Curve(points, settings.seed)
        total = 0.0
        for point in points:
            total += point.weight
        self.target = total / settings.count
        self.territories = [Territory() for _ in range(settings.count)]
        territory, weight, target = 0, 0.0, self.target
        for point in curve.road():
            while weight + point.weight / 2 > target:
                if territory == settings.count - 1:
                    raise ValueError("The initial assignment could not place all ZIPs.")
                total -= weight
                weight = 0.0
                territory += 1
                target = total / (settings.count - territory)
            point.territory = territory
            weight += point.weight
        for point in points:
            group = self.territories[point.territory]
            group.members.append(point)
            group.weight += point.weight
        for group in self.territories:
            group.update_center(initial=True)

        self.neighbors = neighbor_lists(points, indexes, curve, arcs)
        self.tails: list[int] = []
        self.heads: list[int] = []
        self.offsets = [0]
        for index, adjacent in enumerate(self.neighbors):
            self.tails.extend([index] * len(adjacent))
            self.heads.extend(adjacent)
            self.offsets.append(len(self.heads))

    def objective_terms(self) -> tuple[float, float]:
        balance = compactness = 0.0
        for group in self.territories:
            difference = self.target - group.weight
            compactness += group.distance
            balance += difference * difference
        return balance, compactness

    def reassign(
        self, point: ZipPoint, destination: int, delta: tuple[float, float]
    ) -> None:
        source = self.territories[point.territory]
        dest = self.territories[destination]
        point.territory = destination
        dest.members.append(point)
        index = source.members.index(point)
        source.members[index] = source.members[-1]
        source.members.pop()
        dest.weight += point.weight
        source.weight -= point.weight
        dest.distance += delta[1]
        source.distance -= delta[0]
        dest.update_center()
        source.update_center()

    def improvement(
        self, point: ZipPoint, destination: int, wt: float
    ) -> tuple[float, float] | None:
        source = self.territories[point.territory]
        dest = self.territories[destination]
        old_dist = point.weight * distance(point.cartesian, source.cartesian)
        new_dist = point.weight * distance(point.cartesian, dest.cartesian)
        source_gap = self.target - source.weight
        dest_gap = self.target - dest.weight
        old_balance = source_gap * source_gap + dest_gap * dest_gap
        source_gap += point.weight
        dest_gap -= point.weight
        new_balance = source_gap * source_gap + dest_gap * dest_gap
        if new_balance + wt * new_dist < old_balance + wt * old_dist:
            return old_dist, new_dist
        return None

    def rebalance(self, wt: float) -> int:
        count = len(self.heads)
        # Python's -1 slot is the legacy head sentinel; -2 marks unlinked arcs.
        left = [-2] * (count + 2)
        right = [count] * (count + 2)
        previous = -1
        points = self.points
        for arc, (tail, head) in enumerate(zip(self.tails, self.heads, strict=True)):
            if head > tail and points[head].territory != points[tail].territory:
                right[previous], left[arc] = arc, previous
                previous = arc
        left[count] = previous
        changes, previous_obj = 0, HUGE
        while True:
            balance, compactness = self.objective_terms()
            obj = balance + wt * compactness
            if obj > previous_obj:
                break
            previous_obj = obj
            moves = borders = 0
            arc = right[-1]
            while arc < count:
                tail, head = self.tails[arc], self.heads[arc]
                first, second = points[tail], points[head]
                moved, destination = -1, -1
                delta = None
                if first.territory != second.territory:
                    borders += 1
                    delta = self.improvement(second, first.territory, wt)
                    moved, destination = head, first.territory
                    if delta is None:
                        delta = self.improvement(first, second.territory, wt)
                        moved, destination = tail, second.territory
                    if delta is None:
                        arc = right[arc]
                        continue
                if delta is not None:
                    for neighbor in range(self.offsets[moved], self.offsets[moved + 1]):
                        if (
                            left[neighbor] == -2
                            and points[self.heads[neighbor]].territory != destination
                        ):
                            right[neighbor] = right[-1]
                            right[-1] = neighbor
                            left[right[neighbor]] = neighbor
                            left[neighbor] = -1
                upcoming = right[arc]
                right[left[arc]] = upcoming
                left[upcoming] = left[arc]
                left[arc] = -2
                # The Java for-loop advances once here and again after continue.
                arc = right[upcoming]
                if delta is not None:
                    self.reassign(points[moved], destination, delta)
                    moves += 1
                    changes += 1
            if moves <= self.settings.fraction * borders:
                break
        return changes

    def optimize(self, report: Callable[[float, int], None]) -> None:
        balance, compactness = self.objective_terms()
        bias = self.settings.bias_start
        wt = bias * balance / compactness if compactness else 0.0
        while bias >= self.settings.bias_end:
            previous = HUGE
            while True:
                balance, compactness = self.objective_terms()
                objective = balance + wt * compactness
                if objective > previous:
                    break
                previous = objective
                changes = self.rebalance(wt)
                report(bias, changes)
                if changes <= self.settings.min_changes:
                    break
            bias /= self.settings.reduction
            wt /= self.settings.reduction

    def state(self) -> dict[str, Any]:
        over = 0
        worst = log_sum = total_distance = total_weight = 0.0
        for group in self.territories:
            weight = group.weight if group.weight > 0 else EPS
            ratio = max(weight / self.target, self.target / weight)
            over += ratio > 1.1
            worst = max(worst, ratio)
            log_sum += log(ratio)
            total_distance += group.distance
            total_weight += group.weight
        return {
            "points": [[p.code, p.x, p.y, p.territory, p.weight] for p in self.points],
            "territories": [
                [len(t.members), t.weight, t.distance] for t in self.territories
            ],
            "target": self.target,
            "statistics": {
                "over": 100 * over / len(self.territories),
                "worst": 100 * (worst - 1),
                "geometric": 100 * (exp(log_sum / len(self.territories)) - 1),
                "distance": total_distance / total_weight,
            },
        }


def run(
    locations: str,
    weights: str,
    arcs: str,
    has_arcs: bool,
    options: str,
    progress: Callable[[float, int], None],
) -> str:
    settings = Settings.read(json.loads(options))
    points, indexes = read_points(locations, weights, settings)
    model = Assignment(points, indexes, settings, arcs if has_arcs else None)
    model.optimize(progress)
    return json.dumps(model.state(), allow_nan=False, separators=(",", ":"))
