// Traveler's Lantern icon scene + animation (pack-icon-animation skill). Run inside Blockbench (free format project):
//   eval(require('fs').readFileSync('<this file>', 'utf8')); window.TL = TL; TL.loadTextures()   // then, in a later call:
//   TL.build(); TL.animate(); TL.camera()                                                        // then:
//   TL.render(0, TL.FRAMES - 1)                                                                  // 1600px frames -> frames/
// Other sessions drive the same Blockbench: every entry point re-selects this project (uuid, else name), the global is `TL`,
// and render() holds window.BB_LOCK while it runs and refuses to start under someone else's.
// The copper golem mascot (Profile-Avatar's rig and textures) holds a lantern, hooks it onto its belt (clink), throws its
// hands up (hands free), hops with a pirouette so the lantern flings out, lands, watches it swing and settle, then takes it
// back into its hand. The lantern is a pendulum simulated in yaw space at 40 steps per frame (gravity, damping, a hinge stop, the body
// in the way), driven by the hand or the belt hook, like the mod's own swing. Light: a pool on the
// ground in key colours that make_icon.py maps under the outline.
// Rig (yaw space, golem faces +z): yaw > root (lift, pirouette) > squash (feet pivot) > legs, body, belt, arms, neck > head.
// The lantern hangs off `yaw` directly: its position is the hook/hand point, its rotation points it along the pendulum.
// Animated rotations land on mesh.rotation as they are (order ZYX), no negation. Every channel is sampled once per frame.
var TL = (function () {
  const fs = require('fs');
  const DIR = '/home/emppu/Projects/Minecraft Datapacks/mods/BeltLantern/dev/icon/';
  const TEX = DIR + 'sprites/';
  const PROJECT = { uuid: '1df832cb-4f04-e464-bdd3-cc43e596a79f', name: 'travelers_lantern_icon' };
  const LOCK_OWNER = 'travelers-lantern-icon';
  const FPS = 20, DT = 1 / FPS, FRAMES = 64, LEN = FRAMES / FPS;
  const YAW = 30;
  const CAM_POS = [0, 60, 104], CAM_TARGET = [0, 13, 0], CAM_PAN = [0, 0, 0], CAM_ZOOM = 0.7;
  const PITCH = -Math.atan2(CAM_POS[1] - CAM_TARGET[1], CAM_POS[2] - CAM_TARGET[2]) * 180 / Math.PI;
  const O = new THREE.Vector3(...CAM_TARGET);
  const RAD = Math.PI / 180;
  const SHOULDER_L = [5.5, 11, 0], GRIP = 8.5;
  const HOOK = [3.0, 7.8, 4.6];           // where the handle hangs on the belt bracket
  const LS = 1.2;                         // lantern scale (a bit bigger than true to size, so it reads at 96 px)
  const ROD = 3.6 * LS;                   // handle top -> the lantern's centre of mass

  const T = {
    lift0: 0.25, place: 0.5, snap: 0.55, hooked: 0.6, armBack: 0.75,
    tada0: 0.8, tada: 0.9, tada1: 1.1,
    crouch: 1.25, jump: 1.35, land: 1.85,
    look0: 1.95, look1: 2.3,
    reach0: 2.35, grab: 2.6, grabbed: 2.65, hold: 2.9, calm0: 2.85, calm1: 3.15,
  };
  const P = { hop: 7, spin: 360, g: 450, damp: 3.2, dampHand: 7, stop: 72, frontZ: 4.8 };

  const q = t => Math.round(t * FPS) / FPS;
  const worldToScreen = w => new THREE.Vector3(...w).sub(O).applyEuler(new THREE.Euler(-PITCH * RAD, 0, 0)).add(O);
  const yawToWorld = a => new THREE.Vector3(...a).applyEuler(new THREE.Euler(0, YAW * RAD, 0));
  const yawToScreen = a => worldToScreen(yawToWorld(a).toArray());

  function own() {
    const p = ModelProject.all.find(p => p.uuid === PROJECT.uuid) || ModelProject.all.find(p => p.name === PROJECT.name);
    if (!p) throw new Error('project ' + PROJECT.name + ' is not open');
    if (Project !== p) p.select();
    return p;
  }

  const tex = {};
  function loadTextures() {
    own();
    Texture.all.slice().forEach(t => t.remove(true));
    for (const f of fs.readdirSync(TEX).filter(f => f.endsWith('.png') && !/^(bg|banner|review|chain|lantern_item)/.test(f))) {
      const url = 'data:image/png;base64,' + fs.readFileSync(TEX + f).toString('base64');
      tex[f.slice(0, -4)] = new Texture({ name: f }).fromDataURL(url).add(false);
    }
    return Object.keys(tex).length + ' textures';
  }
  function ensureTex() {
    if (!Object.keys(tex).length) Texture.all.forEach(t => { tex[t.name.replace('.png', '')] = t; });
  }

  function group(name, origin, parent, rotation) {
    const g = new Group({ name, origin, rotation: rotation || [0, 0, 0] });
    g.addTo(parent); g.init();
    return g;
  }
  const FACES = ['north', 'south', 'east', 'west', 'up', 'down'];
  function cube(name, from, to, parent, faceTex) {
    const c = new Cube({ name, from, to, box_uv: false });
    c.addTo(parent); c.init();
    for (const f of FACES) {
      const spec = faceTex[f] || faceTex.all;
      if (spec) c.faces[f].extend({ texture: tex[spec[0]].uuid, uv: [0, 0, spec[1], spec[2]] });
      else c.faces[f].extend({ texture: null });
    }
    return c;
  }
  function plane(name, centre, size, parent, texName) {
    const [x, y, z] = centre, h = size / 2;
    const c = new Cube({ name, from: [x - h, y - h, z], to: [x + h, y + h, z], box_uv: false });
    c.addTo(parent); c.init();
    for (const f of FACES) c.faces[f].extend(f === 'south' ? { texture: tex[texName].uuid, uv: [0, 0, 16, 16] } : { texture: null });
    return c;
  }

  // golem faces (Profile-Avatar): [sprite, used width, used height]
  const HEAD = { up: ['head_top', 8, 10], south: ['head_front_s', 8, 5], north: ['head_back_s', 8, 5],
    west: ['head_side_w', 10, 5], east: ['head_side_w', 10, 5], down: ['head_bottom', 8, 10] };
  const NOSE = { up: ['nose_top', 2, 2], south: ['nose_front_s', 2, 3], west: ['nose_side_w', 2, 3], east: ['nose_side_w', 2, 3],
    down: ['nose_side_w', 2, 2] };
  const EYE = { south: ['eye', 2, 2] };
  const RODT = { north: ['rod_side_w', 2, 4], south: ['rod_side_w', 2, 4], west: ['rod_side_w', 2, 4], east: ['rod_side_w', 2, 4] };
  const KNOB = { up: ['knob_top', 4, 4], south: ['knob_side_s', 4, 4], north: ['knob_side_s', 4, 4],
    west: ['knob_side_w', 4, 4], east: ['knob_side_w', 4, 4], down: ['knob_side_w', 4, 4] };
  const BODY = { up: ['head_bottom', 8, 6], south: ['body_front_s', 8, 6], north: ['body_back_s', 8, 6],
    west: ['body_side_w', 6, 6], east: ['body_side_w', 6, 6], down: ['head_bottom', 8, 6] };
  const ARM = { up: ['arm_top', 3, 4], south: ['arm_front_s', 3, 10], north: ['arm_front_s', 3, 10],
    west: ['arm_side_w', 4, 10], east: ['arm_side_w', 4, 10], down: ['arm_hand', 3, 4] };
  const LEG = { up: ['leg_side_w', 4, 4], south: ['leg_side_s', 4, 5], north: ['leg_side_s', 4, 5],
    west: ['leg_side_w', 4, 5], east: ['leg_side_w', 4, 5], down: ['leg_side_w', 4, 4] };
  // new: belt, hook, lantern
  const BELT = { south: ['belt_front', 16, 3], north: ['belt_side', 12, 3], east: ['belt_side_w', 12, 3], west: ['belt_side_w', 12, 3],
    up: ['belt_top', 16, 3] };
  const IRON = { all: ['iron_dark', 4, 4], up: ['iron_light', 4, 4] };
  const LANTERN = { south: ['lantern_side', 8, 10], north: ['lantern_side', 8, 10], east: ['lantern_side_w', 8, 10],
    west: ['lantern_side_w', 8, 10], up: ['lantern_top', 8, 8], down: ['lantern_bottom', 8, 8] };
  const CAP = { south: ['cap_side', 5, 2], north: ['cap_side', 5, 2], east: ['cap_side_w', 5, 2], west: ['cap_side_w', 5, 2],
    up: ['cap_top', 5, 5], down: ['cap_side_w', 5, 2] };

  const FX = { clink: ['fx_clink', 4], clink2: ['fx_clink', 4], clink3: ['fx_clink', 4], spark_a: ['fx_spark', 4], spark_b: ['fx_spark', 3.5],
    dust_a: ['fx_dust', 7], dust_b: ['fx_dust', 7] };

  const G = {};
  function build() {
    own(); ensureTex();
    Animation.all.slice().forEach(a => a.remove(false));
    Outliner.root.slice().forEach(n => n.remove(false));

    G.yaw = group('yaw', [0, 0, 0], undefined, [0, YAW, 0]);
    G.pool = group('pool', [0, 0, 0], G.yaw);
    cube('pool_plane', [-11, 0.03, -11], [11, 0.03, 11], G.pool, { up: ['pool', 16, 16] });
    G.shadow = group('shadow', [0, 0, 0], G.yaw);
    cube('shadow_plane', [-8, 0.06, -8], [8, 0.06, 8], G.shadow, { up: ['shadow', 16, 16] });
    G.root = group('root', [0, 0, 0], G.yaw);
    G.squash = group('squash', [0, 0, 0], G.root);
    G.leg_r = group('leg_r', [-2, 5, 0], G.squash);
    cube('leg_r_box', [-4, 0, -2], [0, 5, 2], G.leg_r, LEG);
    G.leg_l = group('leg_l', [2, 5, 0], G.squash);
    cube('leg_l_box', [0, 0, -2], [4, 5, 2], G.leg_l, LEG);
    cube('body', [-4, 5, -3], [4, 11, 3], G.squash, BODY);
    cube('belt', [-4.3, 7.0, -3.3], [4.3, 8.5, 3.3], G.squash, BELT);
    cube('hook', [2.6, 7.6, 3.3], [3.4, 8.2, HOOK[2] + 0.3], G.squash, IRON);
    G.arm_r = group('arm_r', [-5.5, 11, 0], G.squash);
    cube('arm_r_box', [-7, 2, -2], [-4, 12, 2], G.arm_r, ARM);
    G.arm_l = group('arm_l', SHOULDER_L, G.squash);
    cube('arm_l_box', [4, 2, -2], [7, 12, 2], G.arm_l, ARM);
    G.neck = group('neck', [0, 11, 0], G.squash);
    G.head = group('head', [0, 11, 0], G.neck);
    cube('head_box', [-4, 11, -5], [4, 16, 5], G.head, HEAD);
    cube('nose_box', [-1, 10, 4], [1, 13, 6], G.head, NOSE);
    G.eye_r = group('eye_r', [-2, 14, 5], G.head);
    cube('eye_r_box', [-3, 13, 5], [-1, 15, 5.1], G.eye_r, EYE);
    G.eye_l = group('eye_l', [2, 14, 5], G.head);
    cube('eye_l_box', [1, 13, 5], [3, 15, 5.1], G.eye_l, EYE);
    G.antenna = group('antenna', [0, 16, 0], G.head);
    cube('rod', [-1, 16, -1], [1, 20, 1], G.antenna, RODT);
    cube('knob', [-2, 20, -2], [2, 24, 2], G.antenna, KNOB);

    // lantern: pivot = the handle top at the origin
    G.lantern = group('lantern', [0, 0, 0], G.yaw);
    const L = (a) => a.map(v => v * LS);
    cube('handle_top', L([-0.9, -0.3, -0.2]), L([0.9, 0.1, 0.2]), G.lantern, IRON);
    cube('handle_l', L([-0.9, -1.1, -0.2]), L([-0.5, -0.3, 0.2]), G.lantern, IRON);
    cube('handle_r', L([0.5, -1.1, -0.2]), L([0.9, -0.3, 0.2]), G.lantern, IRON);
    cube('cap', L([-1.2, -1.9, -1.2]), L([1.2, -1.1, 1.2]), G.lantern, CAP);
    cube('lantern_body', L([-1.8, -6.3, -1.8]), L([1.8, -1.9, 1.8]), G.lantern, LANTERN);

    G.screen = group('screen', O.toArray(), undefined, [PITCH, 0, 0]);
    for (const [name, [t, size]] of Object.entries(FX)) {
      G[name] = group(name, [0, 0, 0], G.screen);
      plane(name + '_plane', [0, 0, 0], size, G[name], t);
    }
    Canvas.updateAll();
    return Outliner.elements.length + ' elements';
  }

  // ---- motion curves (t in seconds) ----
  const EASE = { lin: u => u, in: u => u * u, out: u => 1 - (1 - u) * (1 - u), io: u => (u < 0.5 ? 2 * u * u : 1 - 2 * (1 - u) * (1 - u)) };
  const lerp = (a, b, u) => (Array.isArray(a) ? a.map((x, i) => x + (b[i] - x) * u) : a + (b - a) * u);
  function pw(keys) {
    return t => {
      if (t <= keys[0][0] + 1e-9) return keys[0][1];
      for (let i = 1; i < keys.length; i++) {
        const [t1, v1, e] = keys[i], [t0, v0] = keys[i - 1];
        if (t <= t1 + 1e-9) {
          if (e === 'step') return t >= t1 - 1e-9 ? v1 : v0;
          return lerp(v0, v1, EASE[e || 'io']((t - t0) / (t1 - t0)));
        }
      }
      return keys[keys.length - 1][1];
    };
  }
  const clamp01 = u => Math.max(0, Math.min(1, u));
  const win = (t, a, b) => t >= a - 1e-9 && t <= b + 1e-9;

  // hop: parabola from jump to land, a pirouette in the air
  const liftAt = t => {
    if (!win(t, T.jump, T.land)) return 0;
    const u = (t - T.jump) / (T.land - T.jump);
    return 4 * P.hop * u * (1 - u);
  };
  const spinAt = t => P.spin * EASE.io(clamp01((t - T.jump) / (T.land - T.jump)));
  const squashAt = pw([
    [0, [1, 1]], [T.snap, [1, 1]], [T.hooked, [1.06, 0.93], 'out'], [T.hooked + 0.1, [0.97, 1.03]], [T.hooked + 0.2, [1, 1]],
    [T.tada0, [1, 1]], [T.tada, [0.94, 1.08], 'out'], [T.tada1, [1, 1]],
    [T.crouch, [1, 1]], [T.jump - 0.05, [1.14, 0.82], 'out'], [T.jump, [0.88, 1.18], 'lin'], [T.jump + 0.15, [0.97, 1.04]],
    [T.jump + 0.25, [1, 1]], [T.land - 0.05, [0.94, 1.08]], [T.land, [1.25, 0.72], 'lin'], [T.land + 0.1, [0.9, 1.12]],
    [T.land + 0.2, [1.05, 0.96]], [T.land + 0.25, [1, 1]],
    [T.grab, [1, 1]], [T.grabbed, [1.04, 0.95], 'out'], [T.grabbed + 0.15, [1, 1]], [LEN, [1, 1]],
  ]);
  const rootRot = t => [0, spinAt(t) % 360, 0];

  // arms: rotation that points an arm (hanging along -y) from its (shifted) shoulder at a target
  let ROT_ORDER = 'ZYX';
  function aimArm(target, shoulder, len) {
    const [sx, sy] = shoulder, dx = target[0] - sx, dy = target[1] - sy;
    const zs = target[2] - Math.sqrt(Math.max(0, len * len - dx * dx - dy * dy));
    const d = new THREE.Vector3(dx, dy, target[2] - zs).normalize();
    const qn = new THREE.Quaternion().setFromUnitVectors(new THREE.Vector3(0, -1, 0), d);
    const e = new THREE.Euler().setFromQuaternion(qn, ROT_ORDER);
    return { pos: [0, 0, zs], rot: [e.x / RAD, e.y / RAD, e.z / RAD] };
  }
  const REST = { pos: [0, 0, 0], rot: [0, 0, 0] };
  let HOLD, LIFT, PLACE;
  const TADA_L = { pos: [0, 0, 0], rot: [0, 0, 150] }, TADA_R = { pos: [0, 0, 0], rot: [0, 0, -150] };
  const flat = v => [...v.pos, ...v.rot];
  const split = k => ({ pos: k.slice(0, 3), rot: k.slice(3) });
  const armL = t => split(pw([
    [0, flat(HOLD)], [T.lift0, flat(HOLD)], [T.lift0 + 0.15, flat(LIFT), 'out'], [T.place, flat(PLACE), 'in'], [T.hooked, flat(PLACE)],
    [T.armBack, flat(REST), 'out'], [T.tada0, flat(REST)], [T.tada, flat(TADA_L), 'out'], [T.tada + 0.1, [0, 0, 0, 0, 0, 140]],
    [T.tada1, flat(REST), 'in'], [T.crouch, flat(REST)], [T.jump, [0, 0, 0, 0, 0, 30], 'out'], [T.jump + 0.2, [0, 0, 0, 0, 0, 80]],
    [T.land - 0.1, [0, 0, 0, 0, 0, 60]], [T.land + 0.15, flat(REST), 'out'],
    [T.reach0, flat(REST)], [T.grab, flat(PLACE), 'io'], [T.grabbed, flat(PLACE)], [T.hold, flat(HOLD), 'io'], [LEN, flat(HOLD)],
  ])(t));
  const armR = t => split(pw([
    [0, flat(REST)], [T.tada0, flat(REST)], [T.tada, flat(TADA_R), 'out'], [T.tada + 0.1, [0, 0, 0, 0, 0, -140]],
    [T.tada1, flat(REST), 'in'], [T.crouch, flat(REST)], [T.jump, [0, 0, 0, 0, 0, -30], 'out'], [T.jump + 0.2, [0, 0, 0, 0, 0, -80]],
    [T.land - 0.1, [0, 0, 0, 0, 0, -60]], [T.land + 0.15, flat(REST), 'out'], [LEN, flat(REST)],
  ])(t));
  const legs = (t, s) => [pw([[0, 0], [T.jump, 0], [T.jump + 0.15, 25 * s, 'out'], [T.land - 0.15, -15 * s], [T.land, 0, 'in'], [LEN, 0]])(t), 0, 0];

  // head watches the lantern swing
  const neckRot = pw([[0, [0, 0, 0]], [T.lift0, [0, 0, 0]], [T.place, [14, 18, 0]], [T.hooked + 0.15, [10, 14, 0]], [T.tada0, [0, 0, 0]],
    [T.tada, [-10, 0, 0], 'out'], [T.tada1, [0, 0, 0]], [T.look0, [0, 0, 0]], [T.look0 + 0.2, [16, 24, -6], 'out'],
    [T.look1, [12, 20, -4]], [T.reach0 + 0.15, [14, 18, 0]], [T.hold, [0, 0, 0]], [LEN, [0, 0, 0]]]);
  const eyeScale = pw([[0, [1, 1, 1]], [T.tada0, [1, 1, 1]], [T.tada0 + 0.05, [1.1, 0.25, 1], 'step'], [T.tada1, [1.1, 0.25, 1]],
    [T.tada1 + 0.05, [1, 1, 1], 'step'], [T.jump, [1, 1, 1]], [T.jump + 0.05, [1.3, 1.3, 1], 'step'], [T.land, [1.3, 1.3, 1]],
    [T.land + 0.05, [1.2, 0.3, 1], 'step'], [T.land + 0.15, [1, 1, 1], 'step'], [LEN, [1, 1, 1]]]);
  const antennaRot = t => {
    const wob = (t0, amp) => (t >= t0 && t < t0 + 0.8 ? amp * Math.exp(-(t - t0) / 0.18) * Math.sin(2 * Math.PI * (t - t0) / 0.16) : 0);
    return [wob(T.jump, -10) + wob(T.land, 20), 0, wob(T.hooked, 6) + wob(T.land, -12)];
  };

  // ---- rig maths (yaw space) ----
  const V = (a) => new THREE.Vector3(...a);
  // a point on the squash group -> yaw space at time t
  function squashToYaw(p, t) {
    const [sx, sy] = squashAt(t);
    const v = new THREE.Vector3(p[0] * sx, p[1] * sy, p[2] * sx);
    v.applyEuler(new THREE.Euler(...rootRot(t).map(a => a * RAD), 'ZYX'));
    v.y += liftAt(t);
    return v;
  }
  function handAt(t) {
    const a = armL(t);
    const tip = new THREE.Vector3(0, -GRIP, 0).applyEuler(new THREE.Euler(...a.rot.map(x => x * RAD), ROT_ORDER));
    return squashToYaw([SHOULDER_L[0] + a.pos[0] + tip.x, SHOULDER_L[1] + a.pos[1] + tip.y, SHOULDER_L[2] + a.pos[2] + tip.z], t);
  }
  const hookAt = t => squashToYaw(HOOK, t);
  const onHook = t => t >= T.snap - 1e-9 && t < T.grab - 1e-9;
  // the lantern's hanging point: the hand, or the hook; the hand-offs take 1 frame (a thrown catch)
  function pivotAt(t) {
    if (win(t, T.snap, T.hooked)) return handAt(T.snap).lerp(hookAt(T.hooked), EASE.out((t - T.snap) / (T.hooked - T.snap)));
    if (win(t, T.grab, T.grabbed)) return hookAt(T.grab).lerp(handAt(T.grabbed), EASE.out((t - T.grab) / (T.grabbed - T.grab)));
    return onHook(t) ? hookAt(t) : handAt(t);
  }

  // pendulum: bob at distance ROD from the pivot, 40 steps per frame (Verlet + distance constraint)
  const SIM = { dirs: [], clinks: [] };
  function simulate() {
    const sub = 40, h = DT / sub, n = FRAMES * sub;
    let piv = pivotAt(0), b = piv.clone().add(new THREE.Vector3(0, -ROD, 0)), prev = b.clone();
    SIM.dirs = []; SIM.clinks = [];
    let lastClink = -1;
    for (let i = 0; i <= n; i++) {
      const t = i * h;
      if (i % sub === 0) SIM.dirs[i / sub] = b.clone().sub(piv).normalize();
      if (i === n) break;
      const nt = (i + 1) * h;
      const np = pivotAt(nt);
      const vel = b.clone().sub(prev).multiplyScalar(Math.exp(-(t > T.grab ? P.dampHand : P.damp) * h));  // a hand steadies it
      prev = b.clone();
      b.add(vel).add(new THREE.Vector3(0, -P.g * h * h, 0));
      piv = np;
      // rod length
      let d = b.clone().sub(piv); d.setLength(ROD); b = piv.clone().add(d);
      // hinge stop: at most P.stop degrees from hanging
      const ang = Math.acos(Math.max(-1, Math.min(1, -d.y / ROD))) / RAD;
      if (ang > P.stop) {
        const horiz = new THREE.Vector3(d.x, 0, d.z).normalize();
        const s = Math.sin(P.stop * RAD), c = Math.cos(P.stop * RAD);
        b = piv.clone().add(new THREE.Vector3(horiz.x * s * ROD, -c * ROD, horiz.z * s * ROD));
      }
      // on the belt: the body is in the way (golem frame: the bob stays in front of z = frontZ)
      if (onHook(nt)) {
        const inv = new THREE.Euler(0, -rootRot(nt)[1] * RAD, 0);
        const local = b.clone().sub(new THREE.Vector3(0, liftAt(nt), 0)).applyEuler(inv);
        if (local.z < P.frontZ) {
          const speed = b.clone().sub(prev).length() / h;
          local.z = P.frontZ;
          b = local.applyEuler(new THREE.Euler(0, rootRot(nt)[1] * RAD, 0)).add(new THREE.Vector3(0, liftAt(nt), 0));
          d = b.clone().sub(piv); d.setLength(ROD); b = piv.clone().add(d);
          if (speed > 25 && nt - lastClink > 0.2 && nt > T.hooked + 0.05) { SIM.clinks.push(q(nt)); lastClink = nt; }
        }
      }
    }
    return SIM.dirs.length;
  }
  // loop seam: the swing fades to hanging straight down over the calm window (it is nearly still by then)
  function dirAt(f) {
    const t = f * DT, d = SIM.dirs[Math.min(f, FRAMES)].clone();
    const k = EASE.io(clamp01((t - T.calm0) / (T.calm1 - T.calm0)));
    return d.lerp(new THREE.Vector3(0, -1, 0), k).normalize();
  }
  function lanternRot(f) {
    const t = f * DT, d = dirAt(f);
    const qd = new THREE.Quaternion().setFromUnitVectors(new THREE.Vector3(0, -1, 0), d);
    const twist = onHook(t) ? rootRot(t)[1] : 0;
    const qy = new THREE.Quaternion().setFromEuler(new THREE.Euler(0, twist * RAD, 0));
    const e = new THREE.Euler().setFromQuaternion(qd.multiply(qy), ROT_ORDER);
    return [e.x / RAD, e.y / RAD, e.z / RAD];
  }
  // the lantern's glowing centre (yaw space)
  const glowAt = f => pivotAt(f * DT).add(dirAt(f).multiplyScalar(4.1 * LS));

  // ---- animation ----
  let A = null;
  function K(g, ch, t, v) {
    const [x, y, z] = typeof v === 'number' ? [v, v, v] : v;
    A.getBoneAnimator(g).addKeyframe({ channel: ch, time: q(t), interpolation: 'linear', data_points: [{ x, y, z }] });
  }
  function sampled(g, ch, f) {
    const v = Array.from({ length: FRAMES + 1 }, (_, i) => {
      const r = f(i * DT, i);
      return typeof r === 'number' ? [r, r, r] : r;
    });
    const same = (a, b) => a.every((x, i) => Math.abs(x - b[i]) < 1e-6);
    v.forEach((val, i) => {
      if (i > 0 && i < FRAMES && same(val, v[i - 1]) && same(val, v[i + 1])) return;
      K(g, ch, i * DT, val);
    });
  }
  // screen-space coordinates of a yaw-space point (FX planes sit at the screen origin; their position keys are these)
  const scr = a => yawToScreen(a).toArray();

  function ensureG() {
    if (!G.yaw) Group.all.forEach(g => { G[g.name] = g; });
  }
  function animate() {
    own(); ensureTex(); ensureG();
    Animation.all.slice().forEach(a => a.remove(false));
    A = new Animation({ name: 'icon_loop', length: LEN, loop: 'loop', snapping: FPS }).add(false);
    A.select();
    ROT_ORDER = G.arm_l.mesh.rotation.order;
    HOLD = aimArm([6.8, 8.6, 7.4], SHOULDER_L, GRIP);
    LIFT = aimArm([6.6, 9.8, 7.2], SHOULDER_L, GRIP);
    PLACE = aimArm([3.6, 7.8, 7.3], SHOULDER_L, GRIP);
    simulate();

    sampled(G.root, 'position', t => [0, liftAt(t), 0]);
    sampled(G.root, 'rotation', rootRot);
    sampled(G.squash, 'scale', t => { const [a, b] = squashAt(t); return [a, b, a]; });
    sampled(G.shadow, 'scale', t => { const s = 1 - liftAt(t) / 18; return [s, 1, s]; });
    sampled(G.leg_r, 'rotation', t => legs(t, 1));
    sampled(G.leg_l, 'rotation', t => legs(t, -1));
    sampled(G.arm_l, 'rotation', t => armL(t).rot);
    sampled(G.arm_l, 'position', t => armL(t).pos);
    sampled(G.arm_r, 'rotation', t => armR(t).rot);
    sampled(G.neck, 'rotation', neckRot);
    sampled(G.eye_r, 'scale', eyeScale);
    sampled(G.eye_l, 'scale', eyeScale);
    sampled(G.antenna, 'rotation', antennaRot);

    sampled(G.lantern, 'position', t => pivotAt(t).toArray());
    sampled(G.lantern, 'rotation', (t, f) => lanternRot(f));
    // light: the pool on the ground under the lantern
    sampled(G.pool, 'position', (t, f) => { const g = glowAt(f); return [g.x, 0, g.z]; });

    // clinks: the hook catch, the grab, and the lantern knocking the belt
    const clinkAt = (name, t0, where) => {
      sampled(G[name], 'scale', pw([[0, 0], [t0 - 0.05, 0], [t0, 1.3, 'step'], [t0 + 0.05, 1, 'out'], [t0 + 0.15, 0, 'lin'], [LEN, 0]]));
      sampled(G[name], 'rotation', pw([[0, [0, 0, 0]], [t0, [0, 0, 0]], [t0 + 0.15, [0, 0, 45], 'lin'], [LEN, [0, 0, 45]]]));
      const s = scr(where.toArray());
      sampled(G[name], 'position', () => [s[0], s[1], s[2] + 30]);
    };
    clinkAt('clink', T.hooked, hookAt(T.hooked));
    clinkAt('clink2', T.grabbed, handAt(T.grabbed));
    const knock = SIM.clinks.find(t => t > T.land && t < T.grab - 0.2);
    if (knock) clinkAt('clink3', knock, hookAt(knock).add(new THREE.Vector3(0, -2, 1)));
    else sampled(G.clink3, 'scale', () => 0);
    // hands-free sparkles above the raised hands
    [['spark_a', T.tada, [9, 19, 1]], ['spark_b', T.tada + 0.1, [-9, 20, 1]]].forEach(([n, t0, p]) => {
      sampled(G[n], 'scale', pw([[0, 0], [t0 - 0.05, 0], [t0, 1.2, 'step'], [t0 + 0.1, 0.9], [t0 + 0.2, 0, 'lin'], [LEN, 0]]));
      const s = scr(p);
      sampled(G[n], 'position', () => [s[0], s[1], s[2] + 30]);
    });
    // landing dust
    [['dust_a', -4], ['dust_b', 4]].forEach(([n, dx]) => {
      sampled(G[n], 'scale', pw([[0, 0], [T.land - 0.05, 0], [T.land, 0.5, 'step'], [T.land + 0.1, 0.85, 'out'], [T.land + 0.25, 0.9],
        [T.land + 0.3, 0, 'lin'], [LEN, 0]]));
      sampled(G[n], 'position', t => { const u = EASE.out(clamp01((t - T.land) / 0.3)); const s = scr([dx * (1 + 0.5 * u), 0.8 + 2 * u, 2]);
        return [s[0], s[1], s[2] + 20]; });
    });

    Animator.preview();
    return 'clinks ' + JSON.stringify(SIM.clinks) + ', knock ' + knock;
  }

  // ---- checks ----
  function maxSwing() {
    return SIM.dirs.map((d, f) => [f, Math.round(Math.acos(-d.y) / RAD)]).filter(([, a]) => a > 5);
  }

  // ---- camera + render ----
  function camera(zoom, pan) {
    own();
    const p = Preview.selected;
    p.setProjectionMode(true);
    const pp = pan || CAM_PAN;
    const pos = CAM_POS.map((v, i) => v + pp[i]), tgt = CAM_TARGET.map((v, i) => v + pp[i]);
    p.camera.position.set(...pos);
    p.controls.target.set(...tgt);
    p.camera.lookAt(...tgt);
    p.camera.zoom = zoom || CAM_ZOOM; p.camera.updateProjectionMatrix();
    p.controls.update();
  }
  function setTime(t) {
    Timeline.setTime(t);
    Animator.preview();
  }
  function render(first, last, res, dir) {
    const lock = window.BB_LOCK;
    if (lock && lock.owner !== LOCK_OWNER && lock.until > Date.now()) return 'locked by ' + lock.owner + ' for ' + Math.round((lock.until - Date.now()) / 1000) + ' s';
    window.BB_LOCK = { owner: LOCK_OWNER, until: Date.now() + 180000 };
    own();
    res = res || 1600;
    dir = dir || DIR + 'frames/';
    if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
    const shot = f => new Promise(done => {
      setTime(f * DT);
      Screencam.advancedScreenshot(Preview.selected, { angle_preset: 'view', resolution: [res, res], anti_aliasing: 'none', shading: false }, url => {
        fs.writeFileSync(dir + 'frame_' + String(f).padStart(3, '0') + '.png', Buffer.from(url.split(',')[1], 'base64'));
        done();
      });
    });
    return (async () => {
      try { for (let f = first; f <= last; f++) await shot(f); } finally { window.BB_LOCK = null; }
      return `rendered ${first}..${last} into ${dir}`;
    })();
  }
  function scaleSweep() {
    own();
    const bad = [];
    for (let f = 0; f <= FRAMES; f++) {
      setTime(f * DT);
      Group.all.forEach(g => { const s = g.mesh.scale; if (s.x < 0 || s.y < 0 || s.z < 0) bad.push(g.name + '@' + f); });
    }
    return bad.length ? bad.join(',') : 'no negative scale';
  }

  return { FPS, DT, FRAMES, LEN, T, P, PITCH, G, SIM, tex, own, loadTextures, build, animate, camera, render, setTime, scaleSweep,
    maxSwing, handAt, hookAt, pivotAt, worldToScreen, yawToScreen, q };
})();
