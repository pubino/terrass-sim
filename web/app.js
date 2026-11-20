import {drawMap} from "./view.js";

/**
 * @template {HTMLElement} T
 * @param {string} id
 * @returns {T}
 */
function element(id) {
  return /** @type {T} */ (document.getElementById(id));
}

const form = /** @type {HTMLFormElement} */ (element("settings"));
const source = /** @type {HTMLSelectElement} */ (element("source"));
const highlight = /** @type {HTMLSelectElement} */ (element("highlight"));
const rebuild = /** @type {HTMLInputElement} */ (element("rebuild"));
const fields = /** @type {HTMLFieldSetElement} */ (element("inputs"));
const run = /** @type {HTMLButtonElement} */ (element("run"));
const stop = /** @type {HTMLButtonElement} */ (element("stop"));
const map = /** @type {HTMLCanvasElement} */ (element("map"));
const status = element("status");
const error = element("error");
const selection = element("selection");
const units = new Intl.NumberFormat("en-US", {maximumFractionDigits: 2});
/** @type {Worker | null} */
let worker = null;
/** @type {import("./protocol").Result | null} */
let result = null;
/** @type {HTMLInputElement[]} */
const fileFields = ["locations", "weights", "arcs"].map((id) => element(id));

/** @param {boolean} busy */
function setBusy(busy) {
  fields.disabled = run.disabled = busy;
  stop.disabled = !busy;
}

/** @param {string} message */
function fail(message) {
  error.textContent = message;
  error.hidden = false;
  status.textContent = result ? "Showing the previous assignment." : "";
  setBusy(false);
}

/** @param {string} message */
function resetWorker(message) {
  worker?.terminate();
  worker = null;
  fail(message);
}

function showSelection() {
  if (!result) return;
  const index = highlight.value ? Number(highlight.value) : null;
  drawMap(map, result.points, index);
  selection.hidden = index === null;
  if (index !== null) {
    const [count, weight] = result.territories[index];
    selection.textContent = `${units.format(count)} ZIPs · Weight ${units.format(weight)} · Target ${units.format(result.target)}`;
  }
}

/** @param {import("./protocol").Result} state */
function showResult(state) {
  result = state;
  const {points, territories, statistics} = state;
  highlight.replaceChildren(new Option("All territories", ""));
  const options = document.createDocumentFragment();
  territories.forEach((_, i) => options.append(new Option(`Territory ${i + 1}`, String(i))));
  highlight.append(options);
  highlight.disabled = false;
  showSelection();
  element("zip-count").textContent = units.format(points.length);
  element("territory-count").textContent = units.format(territories.length);
  element("distance").textContent = `${units.format(statistics.distance)} mi`;
  for (const name of /** @type {const} */ (["over", "worst", "geometric"])) {
    element(name).textContent = `${units.format(statistics[name])}%`;
  }
  element("results").hidden = false;
  status.textContent = "Assignment complete.";
  setBusy(false);
}

function optimize() {
  error.hidden = true;
  const settings = Object.fromEntries(new FormData(form));
  const [locations, weights, arcs] = fileFields.map((input) => input.files?.[0] ?? null);
  const custom = source.value === "custom";
  if (custom && (!locations || !weights)) {
    fail("Choose both ZIP locations and ZIP weights files.");
    return;
  }
  if (custom && [locations, weights, arcs].some((file) => file && file.size > 24 * 1024 * 1024)) {
    fail("Each input file must be smaller than 24 MB.");
    return;
  }

  if (!worker) {
    worker = new Worker(new URL("worker.js", import.meta.url));
    worker.onerror = () => resetWorker("The Python worker stopped. Select Optimize to retry.");
    worker.onmessage = (/** @type {MessageEvent<import("./protocol").Reply>} */ {data}) => {
      if (data.type === "result") showResult(data.result);
      else if (data.type === "failed") resetWorker(data.message);
      else if (data.type === "progress") {
        status.textContent = `Bias ${units.format(data.bias)} · ${units.format(data.changes)} ZIP moves in this pass`;
      } else status.textContent = data.message;
    };
  }
  status.textContent = custom ? "Loading Python and local files…" : "Loading Python and the historical data…";
  setBusy(true);
  worker.postMessage(/** @type {import("./protocol").Request} */ ({
    settings, rebuild: rebuild.checked, files: custom && locations && weights ? {locations, weights, arcs} : null,
  }));
}

source.addEventListener("change", () => {
  const custom = source.value === "custom";
  element("files").hidden = !custom;
  element("rebuild-label").hidden = custom;
});
highlight.addEventListener("change", showSelection);
form.addEventListener("submit", (event) => {
  event.preventDefault();
  optimize();
});
stop.addEventListener("click", () => {
  worker?.terminate();
  worker = null;
  status.textContent = result ? "Optimization stopped. Showing the previous assignment." : "Optimization stopped.";
  setBusy(false);
});
optimize();
