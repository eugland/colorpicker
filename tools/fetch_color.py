import argparse
import json
import re
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

import requests

HEX_RE = re.compile(r"^[0-9A-Fa-f]{6}$")
API = "https://www.thecolorapi.com/id?hex={hex}"

def norm_hex(s: str) -> str:
    s = s.strip().lstrip("#")
    if not HEX_RE.match(s):
        raise ValueError(f"Invalid hex: {s}")
    return s.upper()

def load_json(path: Path) -> dict:
    if not path.exists():
        return {}
    txt = path.read_text(encoding="utf-8").strip()
    return json.loads(txt) if txt else {}

def save_json(path: Path, data: dict):
    path.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":"), sort_keys=True), encoding="utf-8")

def fetch_name(session: requests.Session, hx: str, timeout: int = 20) -> str:
    r = session.get(API.format(hex=hx), timeout=timeout)
    r.raise_for_status()
    j = r.json()
    return j["name"]["value"]

def main():
    p = argparse.ArgumentParser()
    p.add_argument("--in", dest="infile", default="hex_list.txt", help="One hex per line")
    p.add_argument("--out", dest="outfile", default="color_names.json", help="Output JSON cache")
    p.add_argument("--workers", type=int, default=12, help="Parallel workers (keep modest)")
    args = p.parse_args()

    infile = Path(args.infile)
    outfile = Path(args.outfile)

    hexes = []
    for line in infile.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line:
            continue
        hexes.append(norm_hex(line))

    hexes = sorted(set(hexes))
    cache = load_json(outfile)

    todo = [hx for hx in hexes if hx not in cache]
    print(f"Total hexes: {len(hexes)} | Cached: {len(hexes)-len(todo)} | To fetch: {len(todo)}")

    if not todo:
        print("Nothing to do.")
        return

    # Keep concurrency modest to avoid hammering the service.
    # If you see 429s, reduce --workers.
    with requests.Session() as session:
        with ThreadPoolExecutor(max_workers=args.workers) as ex:
            futures = {ex.submit(fetch_name, session, hx): hx for hx in todo}
            done = 0
            for fut in as_completed(futures):
                hx = futures[fut]
                try:
                    name = fut.result()
                    cache[hx] = name
                except Exception as e:
                    cache[hx] = None  # mark failure, can retry later
                    print(f"{hx} -> ERROR: {e}")
                done += 1
                if done % 50 == 0:
                    save_json(outfile, cache)  # incremental save
                    print(f"Progress: {done}/{len(todo)}")

    save_json(outfile, cache)
    print(f"Saved: {outfile} (entries: {len(cache)})")

if __name__ == "__main__":
    main()
