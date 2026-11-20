"""Assemble the static site and verify the self-hosted Pyodide runtime."""

import argparse
import shutil
from hashlib import sha256
from pathlib import Path
from urllib.request import urlopen

ROOT = Path(__file__).resolve().parents[1]
VERSION = "0.29.3"
CDN = f"https://cdn.jsdelivr.net/pyodide/v{VERSION}/full/"
LICENSE_URL = f"https://raw.githubusercontent.com/pyodide/pyodide/{VERSION}/LICENSE"

# Pin the core runtime used by the StackImages migration.
ASSETS = {
    "pyodide.js": "718d40f1c015dd25ec724cc8fc4e2325d6a45a92ae225121ff6953f224a16f72",
    "pyodide.js.map": (
        "70ca2a53dfadf7d0c6c9eae6b34a5765474a9aa4819891b223cd7302ed76cfab"
    ),
    "pyodide.asm.js": (
        "1263f02b5b26099b96112378156f242dd98b39a8201ba7765e5fe3d455c5ce91"
    ),
    "pyodide.asm.wasm": (
        "e2f4ee75b325e35eb31bfb8c613d4dd5098f5502c156a97847686875b5025480"
    ),
    "python_stdlib.zip": (
        "4298b6ee445cb724c3973437da47789752b9e6ff4e26619026b283ec801fc46b"
    ),
    "pyodide-lock.json": (
        "3256ffc76388de0e37f4b34d42ab484268d1afc675179ff97b2a5bb14f84ccac"
    ),
    "LICENSE": "1f256ecad192880510e84ad60474eab7589218784b9a50bc7ceee34c2b91f1d5",
}
DATA_URL = "https://vanderbei.princeton.edu/JAVA/terrass/"
# The author's historical inputs, fetched into the build rather than committed.
DATA = {
    "zlls.dat": "2f316398d692b11a5b0a36831c34824c662b11f6faa3b9a0d9ac6a33e881c84b",
    "zta.dat": "1266b00c6d1e1cee231962a3820fc55866f774a743fb7d188fb582cadec28025",
    "mknet/arcs.dat": (
        "5fe65c5046779c6e0d1fabeb234e579f5d15fd48da3a5ff6c932d2c919d44b57"
    ),
}
WEB_FILES = (
    "index.html",
    "style.css",
    "app.js",
    "view.js",
    "worker.js",
    "simulation.py",
)


def fetch_verified(url: str, target: Path, expected: str) -> None:
    if target.is_file() and sha256(target.read_bytes()).hexdigest() == expected:
        return
    with urlopen(url, timeout=120) as response:
        content = response.read()
    if sha256(content).hexdigest() != expected:
        raise ValueError(f"Checksum mismatch for {target.name}.")
    partial = target.with_suffix(target.suffix + ".part")
    partial.write_bytes(content)
    partial.replace(target)


def build_site(site: Path) -> None:
    vendor = site / "vendor"
    vendor.mkdir(parents=True, exist_ok=True)
    for name, digest in ASSETS.items():
        url = LICENSE_URL if name == "LICENSE" else CDN + name
        fetch_verified(url, vendor / name, digest)
    for name, digest in DATA.items():
        target = site / "data" / name
        target.parent.mkdir(parents=True, exist_ok=True)
        fetch_verified(DATA_URL + name, target, digest)
    for name in WEB_FILES:
        shutil.copy2(ROOT / "web" / name, site / name)
    shutil.copy2(ROOT / "LICENSE.md", site / "LICENSE.md")
    print(f"Built static site at {site} with Pyodide {VERSION}.")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=ROOT / "build/site")
    args = parser.parse_args()
    build_site(args.output)


if __name__ == "__main__":
    main()
