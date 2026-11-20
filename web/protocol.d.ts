export type ZipPoint = [code: string, x: number, y: number, territory: number, weight: number];
export type Territory = [count: number, weight: number, weightedMiles: number];
export type Result = {
  points: ZipPoint[];
  territories: Territory[];
  target: number;
  statistics: {over: number; worst: number; geometric: number; distance: number};
};
export type Request = {
  settings: Record<string, FormDataEntryValue>;
  rebuild: boolean;
  files: {locations: File; weights: File; arcs: File | null} | null;
};
export type Reply =
  | {type: "status"; message: string}
  | {type: "progress"; bias: number; changes: number}
  | {type: "result"; result: Result}
  | {type: "failed"; message: string};

export type Run = (
  locations: string, weights: string, arcs: string, hasArcs: boolean, settings: string,
  progress: (bias: number, changes: number) => void,
) => string;
export type Pyodide = {
  FS: {writeFile: (path: string, data: string) => void};
  runPython: (code: string) => unknown;
};
export type WorkerScope = {
  location: Location;
  importScripts: (url: string) => void;
  loadPyodide: (options: {indexURL: string}) => Promise<Pyodide>;
  postMessage: (message: Reply) => void;
  onmessage: ((event: MessageEvent<Request>) => void) | null;
};
