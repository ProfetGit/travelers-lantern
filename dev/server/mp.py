#!/usr/bin/env python3
"""Belt Lantern on real servers, off-screen: the mod on the client only, on both sides, and on the server only.

mp.py [--mc 26.3] [--loader fabric] [--case client_only,both,server_only]

client_only  a vanilla dedicated server; the modded client hangs a lantern (client-only belt: the lantern leaves the
             hand for the inventory), walks, takes it off. Checks: its own checks (results.json) and, on the server,
             that its light block never existed there.
both         the modded server with two modded clients: LanternCam wears and walks, LanternFriend watches. Checks:
             the wearer's checks, the watcher draws the wearer's lantern, and the server has the light block.
server_only  the modded server and a vanilla client: /beltlantern run for it from the console hangs the lantern and
             lights its cell; the client stays connected and both logs stay clean.
Work dirs: dev/server/.work/mp-<case>-<mc>-<loader>.
"""
import argparse
import importlib.util
import json
import re
import shutil
import subprocess
import sys
import threading
import time
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
WS = ROOT.parent
sys.path.insert(0, str(WS / "ModTest"))
sys.path.insert(0, str(WS / "ModJar"))
import client  # noqa: E402
import slots  # noqa: E402

_spec = importlib.util.spec_from_file_location("modjar_smoke", WS / "ModJar" / "smoke.py")
ms = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(ms)

WEAR = ("LanternCam", "5e1dcaa0-0000-4000-8000-00000000b001")
WATCH = ("LanternFriend", "5e1dcaa0-0000-4000-8000-00000000b002")
FAMILY = {"fabric": "fabric", "quilt": "fabric", "neoforge": "neoforge", "forge": "forge"}


def jar(mc: str, loader: str) -> Path:
    b = f"{mc}-{FAMILY[loader]}"
    hits = sorted((ROOT / "versions" / b / "build/libs").glob(f"beltlantern-*+{b}.jar"))
    if not hits:
        sys.exit(f"no jar for {b}: build it first")
    return hits[-1]


def vanilla_cmd(mc: str, work: Path) -> list[str]:
    meta = client.META
    vdir = sorted(meta.glob(f"versions/{mc}-*"))[-1]
    cp = subprocess.check_output(["python3", str(WS / "ClientCapture/classpath.py"), str(vdir / f"{vdir.name}.json"), str(meta / "libraries")], text=True).strip()
    return [ms.java(), "-Xmx2G", f"-Djava.io.tmpdir={work / 'tmp'}", "-cp", cp + ":" + str(vdir / f"{vdir.name}.jar"), "net.minecraft.server.Main", "--nogui"]


class Server:
    def __init__(self, work: Path, cmd: list[str], port: int, mod: Path | None):
        shutil.rmtree(work, ignore_errors=True)
        (work / "mods").mkdir(parents=True)
        (work / "tmp").mkdir()
        (work / "eula.txt").write_text("eula=true\n")
        (work / "server.properties").write_text(
            f"online-mode=false\nserver-ip=127.0.0.1\nserver-port={port}\nlevel-type=minecraft\\:flat\ngenerate-structures=false\n"
            "view-distance=6\nsimulation-distance=6\nspawn-protection=0\ndifficulty=peaceful\npause-when-empty-seconds=0\n"
            "white-list=false\nenforce-whitelist=false\nsync-chunk-writes=false\n")
        if mod:
            shutil.copy(mod, work / "mods")
        self.log = work / "server.log"
        self.out = open(self.log, "w")
        self.proc = subprocess.Popen(cmd, cwd=work, stdin=subprocess.PIPE, stdout=self.out, stderr=subprocess.STDOUT, text=True)
        self.lock = threading.Lock()

    def send(self, *lines):
        with self.lock:
            for l in lines:
                self.proc.stdin.write(l + "\n")
            self.proc.stdin.flush()

    def text(self) -> str:
        return self.log.read_text(errors="replace")

    def wait(self, pattern: str, timeout: float, after: int = 0) -> re.Match | None:
        end = time.time() + timeout
        while time.time() < end:
            m = re.search(pattern, self.text()[after:])
            if m:
                return m
            if self.proc.poll() is not None:
                return None
            time.sleep(0.25)
        return None

    def test(self, cmd: str) -> bool:
        """Runs an `execute if ...` from the console and reads its answer."""
        n = len(self.text())
        self.send(cmd)
        m = self.wait(r"Test (passed|failed)", 10, n)
        return bool(m and m.group(1) == "passed")

    def stop(self):
        try:
            self.send("stop")
            self.proc.wait(90)
        except Exception:
            self.proc.kill()
        self.out.close()


def watch_client_log(game: Path, pattern: str, timeout: float) -> re.Match | None:
    log = game / "client.log"
    end = time.time() + timeout
    while time.time() < end:
        if log.exists():
            m = re.search(pattern, log.read_text(errors="replace"))
            if m:
                return m
        time.sleep(0.5)
    return None


def run_case(case: str, mc: str, loader: str, port: int) -> tuple[bool, list[str]]:
    work = HERE / ".work" / f"mp-{case}-{mc}-{loader}"
    shutil.rmtree(work, ignore_errors=True)
    notes, ok = [], True
    modded_server = case != "client_only"
    modded_client = case != "server_only"
    scmd = ms.command(mc, loader, work / "server") if modded_server else vanilla_cmd(mc, work / "server")
    with slots.slot("server", f"BeltLantern mp {case} {mc}-{loader}"):
        srv = Server(work / "server", scmd, port, jar(mc, loader) if modded_server else None)
        try:
            if not srv.wait(r"Done \(\d", 240):
                return False, ["server did not start"] + ms.tail(srv.log)
            results = {}

            def run_client(name, uuid, role, seconds):
                props = {}
                if modded_client:
                    props = {"beltlantern.demo": str(work / f"out-{role}"), "beltlantern.demo.mp": role, "beltlantern.demo.frames": "false",
                             "beltlantern.demo.mp.seconds": str(seconds), "modtest.audit": "1"}
                results[role] = client.run(mc=mc, loader=loader if modded_client else "vanilla", out=work / f"out-{role}",
                                           game=work / f"game-{role}", mod_jars=[jar(mc, loader)] if modded_client else [], props=props,
                                           join=f"127.0.0.1:{port}", options={"fps": 60, "volume": 0.0001, "render_distance": 6},
                                           timeout=seconds + 150, user=name, uuid=uuid, label=f"BeltLantern mp {case} {role}")

            threads = []
            wear = threading.Thread(target=run_client, args=(*WEAR, "wear", 60))
            wear.start()
            threads.append(wear)
            if case == "both":
                time.sleep(3)
                t = threading.Thread(target=run_client, args=(*WATCH, "watch", 25))
                t.start()
                threads.append(t)
            if not srv.wait(r"LanternCam joined the game", 300):
                notes.append("the client never joined")
                ok = False
            else:
                srv.send("gamerule advance_time false", "time set 18000", "gamerule spawn_monsters false", "gamerule spawn_mobs false",
                         "op LanternCam", "gamemode survival LanternCam", "give LanternCam lantern 2", "tp LanternCam 0.5 -60 0.5 -90 0")
                if case == "both" and srv.wait(r"LanternFriend joined the game", 120):
                    srv.send("tp LanternFriend 3.5 -60 -5.5 facing entity LanternCam")
                if case == "server_only":
                    time.sleep(12)
                    srv.send("execute as LanternCam run beltlantern")
                    time.sleep(1.5)
                    lit = srv.test("execute at LanternCam if block ~ ~ ~ minecraft:light")
                    notes.append(("PASS " if lit else "FAIL ") + "server_only/light  light block at the vanilla player's feet")
                    ok &= lit
                    srv.send("execute as LanternCam run beltlantern")
                    time.sleep(1.5)
                    dark = srv.test("execute at LanternCam if block ~ ~ ~ minecraft:air")
                    notes.append(("PASS " if dark else "FAIL ") + "server_only/dark  the cell is air again after /beltlantern")
                    ok &= dark
                else:
                    game = work / "game-wear"
                    m = watch_client_log(game, r"light cell (-?\d+) (-?\d+) (-?\d+) light_moved", 200)
                    if not m:
                        notes.append("FAIL server/light_cell  the client logged no light cell")
                        ok = False
                    else:
                        x, y, z = m.groups()
                        want = "minecraft:light" if modded_server else "minecraft:air"
                        good = srv.test(f"execute if block {x} {y} {z} {want}")
                        what = "the server has the light block" if modded_server else "the server never had the client-side light"
                        notes.append(("PASS " if good else "FAIL ") + f"server/light  {what} ({x} {y} {z} is {want})")
                        ok &= good
            for t in threads:
                t.join(600)
        finally:
            srv.stop()
    for role, r in results.items():
        if not modded_client:
            # a vanilla client has no director: it runs until the timeout; only its log counts
            text = Path(r["log"]).read_text(errors="replace") if Path(r["log"]).exists() else ""
            bad = [l for l in text.splitlines() if re.search(r"Exception|ERROR\]|Disconnected|lost connection", l)
                   and not re.search(r"authlib|Failed to fetch user properties|InvalidCredentials|401|profile|Narrator|libflite|telemetry|realms", l, re.I)]
            notes.append(("PASS " if not bad else "FAIL ") + f"{role}/client_log  " + (bad[0][:200] if bad else "clean"))
            ok &= not bad
            continue
        for x in r["results"]:
            notes.append(("PASS " if x["pass"] else "FAIL ") + f"{role}/{x['name']}  {x.get('detail', '')}")
            ok &= bool(x["pass"])
        if not r["results"]:
            ok = False
            notes.append(f"FAIL {role}: no results ({'; '.join(r['notes'])})")
        for n in r["notes"]:
            if n.startswith("log errors"):
                ok = False
                notes.append(f"FAIL {role}: {n[:240]}")
    stext = srv.text()
    left = re.findall(r"(LanternCam|LanternFriend) lost connection: (.*)", stext)
    for who, why in left:
        if "Disconnected" not in why and "Server closed" not in why:
            ok = False
            notes.append(f"FAIL server: {who} dropped: {why[:200]}")
    for line in stext.splitlines():
        if ms.PROBLEM.search(line) and not any(re.search(n, line) for n in ms.NOISE) and "Can't keep up" not in line:
            ok = False
            notes.append("FAIL SERVER LOG " + line.strip()[:220])
    return ok, notes


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--mc", default="26.3")
    ap.add_argument("--loader", default="fabric")
    ap.add_argument("--case", default="client_only,both,server_only")
    a = ap.parse_args()
    good, total, passed = True, 0, 0
    for i, case in enumerate(a.case.split(",")):
        ok, notes = run_case(case, a.mc, a.loader, 25940 + i)
        good &= ok
        print(f"{'PASS' if ok else 'FAIL'} {case} {a.mc}-{a.loader}", flush=True)
        for n in notes:
            print("     " + n, flush=True)
            if n.startswith(("PASS", "FAIL")):
                total += 1
                passed += n.startswith("PASS")
    print(f"mp {a.mc}-{a.loader}: {passed}/{total} passed, {'PASS' if good else 'FAIL'}")
    sys.exit(0 if good else 1)


if __name__ == "__main__":
    main()
