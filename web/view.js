/** @param {number} index */
function color(index) {
  return `hsl(${(index * 137.508) % 360} ${index % 2 ? 70 : 85}% ${index % 3 ? 34 : 43}%)`;
}

/**
 * @param {HTMLCanvasElement} canvas
 * @param {import("./protocol").Result["points"]} points
 * @param {number | null} selected
 */
export function drawMap(canvas, points, selected) {
  const context = canvas.getContext("2d");
  if (!context) return;
  const {width, height} = canvas;
  context.clearRect(0, 0, width, height);
  let minX = 1, maxX = 0, minY = 1, maxY = 0;
  for (const [, x, y] of points) {
    minX = Math.min(minX, x);
    maxX = Math.max(maxX, x);
    minY = Math.min(minY, y);
    maxY = Math.max(maxY, y);
  }
  const radius = Math.max(maxX - minX, maxY - minY, 0.01) * 0.525;
  const midX = (maxX + minX) / 2, midY = (maxY + minY) / 2;
  const scale = Math.min(width, height) * 0.92 / (2 * radius);
  const size = points.length > 8_000 ? 2.2 : points.length > 300 ? 3.5 : 6;

  /** @type {Map<number, import("./protocol").ZipPoint[]>} */
  const groups = new Map();
  for (const point of points) {
    const key = selected !== null && point[3] !== selected ? -1 : point[3];
    if (!groups.has(key)) groups.set(key, []);
    groups.get(key)?.push(point);
  }
  const entries = [...groups.entries()].sort(([a], [b]) => a - b);
  for (const [index, group] of entries) {
    context.fillStyle = index === -1 ? "#b6c9d8" : color(index);
    for (const [, x, y] of group) {
      context.fillRect(width / 2 + (x - midX) * scale - size / 2,
        height / 2 - (y - midY) * scale - size / 2, size, size);
    }
  }
}
