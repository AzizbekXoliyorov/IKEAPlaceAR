"""Generates optimized GLB furniture models (real-world metres) and PNG thumbnails."""
import json, math, struct, os
from PIL import Image, ImageDraw

OUT_M = "app/src/main/assets/models"
OUT_T = "app/src/main/res/drawable"
def C(r, g, b): return (r / 255, g / 255, b / 255)
WOOD = C(176, 132, 90); DARK = C(40, 40, 42); WHITE = C(240, 238, 234); GRAY = C(130, 132, 138)
LGRAY = C(165, 167, 172); BROWN = C(112, 70, 46); TAN = C(196, 160, 118); CREAM = C(240, 225, 190)
TEAL = C(70, 110, 112); BLUE = C(60, 90, 140); RED = C(170, 60, 50); GREEN = C(80, 120, 80)

def box(cx, cy, cz, sx, sy, sz, c): return ("box", cx, cy, cz, sx, sy, sz, c)
def cyl(cx, y0, cz, r, h, c): return ("cyl", cx, y0, cz, r, h, c)

def legs4(x, z, h, r, c): return [cyl(sx * x, 0, sz * z, r, h, c) for sx in (-1, 1) for sz in (-1, 1)]

M = {}
M["sofa_modern"] = [box(0, .25, 0, 2.2, .3, .9, GRAY), box(0, .46, .1, 2.0, .12, .68, LGRAY), box(0, .65, -.35, 2.2, .5, .2, GRAY),
                    box(-1.0, .5, 0, .2, .4, .9, GRAY), box(1.0, .5, 0, .2, .4, .9, GRAY)] + legs4(1.0, .38, .1, .03, DARK)
M["sofa_classic"] = [box(0, .3, 0, 2.0, .3, .9, BROWN), box(-.45, .5, .08, .88, .12, .7, TAN), box(.45, .5, .08, .88, .12, .7, TAN),
                     box(0, .75, -.37, 2.0, .7, .16, BROWN), box(-.95, .5, 0, .18, .5, .9, BROWN), box(.95, .5, 0, .18, .5, .9, BROWN)] + legs4(.9, .38, .12, .035, DARK)
M["armchair"] = [box(0, .3, 0, .85, .3, .85, TEAL), box(0, .5, .08, .6, .12, .6, C(90, 135, 138)), box(0, .65, -.35, .85, .55, .15, TEAL),
                 box(-.38, .55, 0, .12, .2, .85, TEAL), box(.38, .55, 0, .12, .2, .85, TEAL)] + legs4(.35, .35, .15, .03, DARK)
M["dining_table"] = [box(0, .73, 0, 1.6, .05, .9, WOOD)] + [box(sx * .74, .355, sz * .39, .06, .71, .06, WOOD) for sx in (-1, 1) for sz in (-1, 1)]
M["office_desk"] = [box(0, .74, 0, 1.4, .04, .7, WHITE), box(-.65, .36, 0, .04, .72, .65, WHITE), box(.5, .35, 0, .4, .68, .62, WHITE),
                    box(0, .5, -.3, 1.2, .4, .02, LGRAY), box(.5, .55, .32, .12, .02, .02, DARK), box(.5, .3, .32, .12, .02, .02, DARK)]
M["modern_chair"] = [box(0, .45, 0, .45, .05, .45, BLUE), box(0, .75, -.2, .45, .5, .04, BLUE)] + legs4(.19, .19, .43, .015, DARK)
M["bed"] = [box(0, .2, 0, 1.6, .25, 2.0, WOOD), box(0, .4, .05, 1.5, .2, 1.9, WHITE), box(0, .5, -1.02, 1.7, 1.0, .06, BROWN),
            box(-.38, .54, -.72, .6, .12, .35, CREAM), box(.38, .54, -.72, .6, .12, .35, CREAM), box(0, .52, .45, 1.5, .03, 1.0, C(150, 170, 190))]
M["wardrobe"] = [box(0, 1.0, 0, 1.2, 2.0, .58, WHITE), box(0, 1.0, .292, .01, 1.9, .005, DARK), box(-.08, 1.0, .3, .02, .25, .02, DARK), box(.08, 1.0, .3, .02, .25, .02, DARK)]
M["floor_lamp"] = [cyl(0, 0, 0, .15, .03, DARK), cyl(0, 0, 0, .012, 1.3, DARK), cyl(0, 1.25, 0, .2, .3, CREAM)]
shelf = [box(sx * .4, .9, 0, .03, 1.8, .3, WOOD) for sx in (-1, 1)] + [box(0, y, 0, .77, .03, .3, WOOD) for y in (.015, .38, .74, 1.1, 1.46, 1.785)] + [box(0, .9, -.14, .8, 1.8, .01, C(150, 110, 75))]
for i, (x, y, col) in enumerate([(-.25, .5, RED), (-.15, .5, BLUE), (-.05, .5, GREEN), (.2, .87, TEAL), (.3, .87, TAN), (-.2, 1.23, RED), (0, 1.23, DARK)]):
    shelf.append(box(x, y, 0, .06, .28, .2, col))
M["bookshelf"] = shelf

def box_geo(p):
    _, cx, cy, cz, sx, sy, sz, _ = p
    h = (sx / 2, sy / 2, sz / 2)
    X, Y, Z = (1, 0, 0), (0, 1, 0), (0, 0, 1)
    neg = lambda v: tuple(-a for a in v)
    faces = [(X, Y, Z), (neg(X), Z, Y), (Y, Z, X), (neg(Y), X, Z), (Z, X, Y), (neg(Z), Y, X)]
    V, N, I = [], [], []
    for n, u, v in faces:
        c = [cx + n[0] * h[0], cy + n[1] * h[1], cz + n[2] * h[2]]
        uu = [u[i] * h[i] for i in range(3)]; vv = [v[i] * h[i] for i in range(3)]
        b = len(V)
        for su, sv in ((-1, -1), (1, -1), (1, 1), (-1, 1)):
            V.append(tuple(c[i] + su * uu[i] + sv * vv[i] for i in range(3))); N.append(n)
        I += [b, b + 1, b + 2, b, b + 2, b + 3]
    return V, N, I

def cyl_geo(p, seg=20):
    _, cx, y0, cz, r, h, _ = p
    V, N, I = [], [], []
    for i in range(seg):
        a0, a1 = 2 * math.pi * i / seg, 2 * math.pi * (i + 1) / seg
        b = len(V)
        for a, y in ((a0, y0), (a1, y0), (a1, y0 + h), (a0, y0 + h)):
            V.append((cx + r * math.cos(a), y, cz + r * math.sin(a))); N.append((math.cos(a), 0, math.sin(a)))
        I += [b, b + 2, b + 1, b, b + 3, b + 2]
        t = len(V)
        V += [(cx, y0 + h, cz), (cx + r * math.cos(a1), y0 + h, cz + r * math.sin(a1)), (cx + r * math.cos(a0), y0 + h, cz + r * math.sin(a0))]
        N += [(0, 1, 0)] * 3; I += [t, t + 1, t + 2]
    return V, N, I

def build_glb(parts, name):
    mats, prims = {}, []
    for p in parts:
        mats.setdefault(p[-1], []).append(p)
    blob = bytearray(); views, accs, materials, primitives = [], [], [], []
    def add(data, target, ctype, count, typ, mn=None, mx=None):
        while len(blob) % 4: blob.append(0)
        views.append({"buffer": 0, "byteOffset": len(blob), "byteLength": len(data), "target": target}); blob.extend(data)
        a = {"bufferView": len(views) - 1, "componentType": ctype, "count": count, "type": typ}
        if mn: a["min"], a["max"] = mn, mx
        accs.append(a); return len(accs) - 1
    for col, ps in mats.items():
        V, N, I = [], [], []
        for p in ps:
            v, n, i = box_geo(p) if p[0] == "box" else cyl_geo(p)
            o = len(V); V += v; N += n; I += [k + o for k in i]
        pos = b"".join(struct.pack("<3f", *v) for v in V); nor = b"".join(struct.pack("<3f", *n) for n in N)
        idx = b"".join(struct.pack("<I", i) for i in I)
        mn = [min(v[k] for v in V) for k in range(3)]; mx = [max(v[k] for v in V) for k in range(3)]
        pa = add(pos, 34962, 5126, len(V), "VEC3", mn, mx); na = add(nor, 34962, 5126, len(V), "VEC3"); ia = add(idx, 34963, 5125, len(I), "SCALAR")
        materials.append({"pbrMetallicRoughness": {"baseColorFactor": [*col, 1], "metallicFactor": 0.0, "roughnessFactor": 0.75}, "doubleSided": False})
        primitives.append({"attributes": {"POSITION": pa, "NORMAL": na}, "indices": ia, "material": len(materials) - 1})
    g = {"asset": {"version": "2.0", "generator": "ikea-place-ar"}, "scene": 0, "scenes": [{"nodes": [0]}],
         "nodes": [{"mesh": 0, "name": name}], "meshes": [{"primitives": primitives}], "materials": materials,
         "buffers": [{"byteLength": len(blob)}], "bufferViews": views, "accessors": accs}
    js = json.dumps(g, separators=(",", ":")).encode()
    js += b" " * (-len(js) % 4)
    while len(blob) % 4: blob.append(0)
    total = 12 + 8 + len(js) + 8 + len(blob)
    return struct.pack("<III", 0x46546C67, 2, total) + struct.pack("<II", len(js), 0x4E4F534A) + js + struct.pack("<II", len(blob), 0x004E4942) + bytes(blob)

def thumb(parts, path):
    S = 256; im = Image.new("RGB", (S, S), (240, 234, 226)); d = ImageDraw.Draw(im)
    px = lambda x, y, z: (x + .45 * z, y + .3 * z)
    items = []
    for p in parts:
        if p[0] == "box": _, cx, cy, cz, sx, sy, sz, c = p; x0, x1, y0, y1, z0, z1 = cx - sx / 2, cx + sx / 2, cy - sy / 2, cy + sy / 2, cz - sz / 2, cz + sz / 2
        else: _, cx, y0, cz, r, h, c = p; x0, x1, y1, z0, z1 = cx - r, cx + r, y0 + h, cz - r, cz + r
        items.append((z1, x0, x1, y0, y1, z0, z1, c))
    pts = [px(x, y, z) for (_, x0, x1, y0, y1, z0, z1, _) in items for x in (x0, x1) for y in (y0, y1) for z in (z0, z1)]
    minx, maxx = min(a for a, _ in pts), max(a for a, _ in pts); miny, maxy = min(b for _, b in pts), max(b for _, b in pts)
    sc = (S * .78) / max(maxx - minx, maxy - miny); ox = (S - (maxx - minx) * sc) / 2; oy = (S + (maxy - miny) * sc) / 2
    T = lambda x, y, z: (ox + (px(x, y, z)[0] - minx) * sc, oy - (px(x, y, z)[1] - miny) * sc)
    sh = lambda c, k: tuple(int(max(0, min(255, v * 255 * k))) for v in c)
    for (_, x0, x1, y0, y1, z0, z1, c) in sorted(items, key=lambda t: (t[5], t[3])):
        d.polygon([T(x0, y1, z0), T(x1, y1, z0), T(x1, y1, z1), T(x0, y1, z1)], fill=sh(c, 1.12))
        d.polygon([T(x1, y0, z1), T(x1, y1, z1), T(x1, y1, z0), T(x1, y0, z0)], fill=sh(c, .72))
        d.polygon([T(x0, y0, z1), T(x1, y0, z1), T(x1, y1, z1), T(x0, y1, z1)], fill=sh(c, .95))
    im.save(path)

os.makedirs(OUT_M, exist_ok=True)
for k, parts in M.items():
    data = build_glb(parts, k); open(f"{OUT_M}/{k}.glb", "wb").write(data)
    thumb(parts, f"{OUT_T}/thumb_{k}.png"); print(k, len(data), "bytes")
