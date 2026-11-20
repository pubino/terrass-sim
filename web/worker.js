const scope = /** @type {import("./protocol").WorkerScope} */ (/** @type {unknown} */ (self));
const base = scope.location.href;

/** @param {string} path */
async function text(path) {
  const response = await fetch(new URL(path, base));
  if (!response.ok) throw new Error(`Could not load ${path.split("/").at(-1)}.`);
  return response.text();
}

async function initialize() {
  const vendor = new URL("vendor/", base).href;
  scope.importScripts(new URL("pyodide.js", vendor).href);
  const [python, source] = await Promise.all([
    scope.loadPyodide({indexURL: vendor}), text("simulation.py"),
  ]);
  python.FS.writeFile("/home/pyodide/simulation.py", source);
  return /** @type {import("./protocol").Run} */ (python.runPython("from simulation import run\nrun"));
}

/** @type {Promise<import("./protocol").Run> | null} */
let engine = null;
/** @type {Promise<[string, string]> | undefined} */
let historical;
/** @type {Promise<string> | undefined} */
let adjacency;

scope.onmessage = async (/** @type {MessageEvent<import("./protocol").Request>} */ {data}) => {
  try {
    engine ??= initialize();
    const {files, rebuild, settings} = data;
    const locationsAndWeights = files
      ? Promise.all([files.locations.text(), files.weights.text()])
      : historical ??= Promise.all([text("data/zlls.dat"), text("data/zta.dat")]);
    const network = files
      ? files.arcs?.text()
      : rebuild ? null : adjacency ??= text("data/mknet/arcs.dat");
    const [run, [locations, weights], arcs] = await Promise.all([engine, locationsAndWeights, network]);
    scope.postMessage({type: "status", message: "Projecting ZIPs and building the initial assignment…"});
    const result = JSON.parse(run(locations, weights, arcs ?? "", typeof arcs === "string", JSON.stringify(settings),
      (bias, changes) => scope.postMessage({type: "progress", bias, changes})));
    scope.postMessage({type: "result", result});
  } catch (error) {
    const message = String(error).match(/ValueError: ([^\r\n]+)/)?.[1];
    scope.postMessage({
      type: "failed",
      message: message ?? "Could not load the Python runtime or data. Select Optimize to retry.",
    });
  }
};
