-- Traveler's Lantern icon sprites (pack-icon-animation skill). Run through the aseprite MCP:
--   dofile("/home/emppu/Projects/Minecraft Datapacks/BeltLantern/dev/icon/draw_sprites.lua")
-- The copper golem's textures are copied from Profile-Avatar/dev/icon/sprites (same author, the channel mascot).
-- New here: the lantern (iron frame, flame), the belt and hook, the light keys, backgrounds and the lettering.
-- Face textures sit in the top-left of a 16x16 canvas (Blockbench treats taller-than-wide images as animated strips).
-- Light and shadow are drawn in key colours; make_icon.py maps them to their real colours under the outline:
--   #FF00FF golem shadow, #00FFFF/#00FFC0/#00FF80 ground light pool (outer..inner).
dofile("/home/emppu/Projects/Minecraft Datapacks/.claude/skills/pack-icon-animation/assets/pixel_art.lua")
local OUT = "/home/emppu/Projects/Minecraft Datapacks/BeltLantern/dev/icon/sprites/"

local C = {
  -- iron, cool blue-grey ramp
  k = "#161A2B", i = "#2E3450", m = "#4C5578", l = "#7A84A8", h = "#B4BCD6",
  -- flame and glass
  W = "#FFF7D6", Y = "#FFD45C", O = "#FF9A2E", R = "#D9621E", r = "#9C3B16",
  -- leather
  ["1"] = "#2A170C", ["2"] = "#4A2A16", ["3"] = "#6E4024", ["4"] = "#935A33", ["5"] = "#B87A4A",
  -- keys
  S = "#FF00FF", A = "#00FFFF", B = "#00FFC0", D = "#00FF80",
  -- sparkle
  w = "#FFFFFF", p = "#FFF3B0",
}
local IRON = { k = "k", i = "k", m = "i", l = "m", h = "l" }

local function pad(rows, n)
  n = n or 16
  local out = {}
  for y = 1, n do out[y] = (rows[y] or "") .. string.rep(".", n - #(rows[y] or "")) end
  return out
end
local function face(name, rows)
  PA.sprite_from_grid(OUT .. name, pad(rows), C)
end
-- a copy with the iron one step darker (the flame keeps glowing)
local function darker(name, suffix)
  local m = {}
  for from, to in pairs(IRON) do m[C[from]] = C[to] end
  for _, g in ipairs({ "W", "Y", "O", "R", "r" }) do m[C[g]] = C[g] end
  PA.remap(OUT .. name .. ".aseprite", OUT .. name .. suffix, m)
end

-- lantern body: 4 x 5 units at 2 px per unit. Iron frame round a glowing glass, the flame in the middle.
face("lantern_side", {
  "lhhhhhhm",
  "lOYYYYOk",
  "lYYWWYYk",
  "lYWWWWYk",
  "lYWWWWYk",
  "lYYWWYYk",
  "lOYYYYOk",
  "lROOOORk",
  "mmmmmmmk",
  "iiiiiiik",
})
darker("lantern_side", "_w")
face("lantern_top", {
  "hhhhhhhl",
  "hOOOOOOm",
  "hOYYYYOm",
  "hOYWWYOm",
  "hOYWWYOm",
  "hOYYYYOm",
  "hOOOOOOm",
  "lmmmmmmm",
})
face("lantern_bottom", { "iiiiiiii", "iiiiiiii", "iikkkkii", "iikkkkii", "iikkkkii", "iikkkkii", "iiiiiiii", "iiiiiiii" })
-- cap: 2.5 x 1 x 2.5 units, 2 px per unit
face("cap_side", { "hhhhl", "mmmmi" })
darker("cap_side", "_w")
face("cap_top", { "hhhhl", "hlllm", "hlklm", "hlllm", "lmmmm" })
-- handle and hook: flat iron (the bars are thin, one tone per face)
face("iron_light", { "llll", "llll", "llll", "llll" })
face("iron_dark", { "mmmm", "mmmm", "mmmm", "mmmm" })

-- belt: 1.5 units tall round the golem's body (8 wide, 6 deep), 2 px per unit. Copper-free leather, an iron buckle.
face("belt_front", {
  "5555555hhhhl5555",
  "444444hi33ik4444",
  "3333333mmmmk3333",
})
face("belt_side", {
  "555555555555",
  "455455455455",
  "333333333333",
})
PA.remap(OUT .. "belt_side.aseprite", OUT .. "belt_side_w",
  { [C["5"]] = C["4"], [C["4"]] = C["3"], [C["3"]] = C["2"] })
face("belt_top", { "4444444444444444", "4444444444444444", "4444444444444444" })

-- a stepped disc in key colours (make_icon.py maps them): the ground light pool
local function rings(name, radii, keys)
  local rows = {}
  for y = 0, 31 do
    local r = {}
    for x = 0, 31 do
      local d = math.sqrt((x - 15.5) ^ 2 + (y - 15.5) ^ 2)
      local ch = "."
      for i, rad in ipairs(radii) do if d <= rad then ch = keys[i] end end
      r[#r + 1] = ch
    end
    rows[#rows + 1] = table.concat(r)
  end
  PA.sprite_from_grid(OUT .. name, rows, C)
end
rings("pool", { 15.9, 11, 6 }, { "A", "B", "D" })

-- clink: a small white 4-point sparkle
PA.sprite_from_grid(OUT .. "fx_clink", pad({
  "...w...",
  "...w...",
  "..wpw..",
  "wwpppww",
  "..wpw..",
  "...w...",
  "...w...",
}, 8), C)

-- item-size lantern for the description kit caps and divider (16 x 16, vanilla-like silhouette, drawn from scratch)
PA.sprite_from_grid(OUT .. "lantern_item", {
  "................",
  "......kkkk......",
  ".....k.mm.k.....",
  ".....k....k.....",
  "......kmmk......",
  ".....khhlmk.....",
  "....khhhhlmk....",
  "...kmllllllik...",
  "...klOYYYYOik...",
  "...klYWWWWYik...",
  "...klYWWWWYik...",
  "...klYYWWYYik...",
  "...klROOOORik...",
  "...kmmmmmmmik...",
  "....kiiiiiik....",
  ".....kkkkkk.....",
}, C)
-- chain strip for the description kit: iron links along a 16-px tile, 5 rows tall
PA.sprite_from_grid(OUT .. "chain_strip", {
  ".hhl....hhl.....",
  "h..lmmmm..lmmmm.",
  "l..mk..ik.mk..i.",
  ".lmm....lmm.....",
  "................",
}, C)

-- backgrounds: flat night navy, 64x64 (icon) and 192x64 (banner) cells, a few stars on the banner
local NAVY = "#1A2140"
local function flat(name, w, h, stars)
  local img = Image(w, h, ColorMode.RGB)
  local c = Color(PA.hex(NAVY))
  for y = 0, h - 1 do for x = 0, w - 1 do img:drawPixel(x, y, c) end end
  for _, s in ipairs(stars or {}) do
    img:drawPixel(s[1], s[2], Color(PA.hex(s[3])))
  end
  local spr = Sprite(w, h, ColorMode.RGB)
  spr.cels[1].image = img
  spr:saveAs(OUT .. name .. ".aseprite")
  spr:saveCopyAs(OUT .. name .. ".png")
  spr:close()
  print("saved " .. name)
end
flat("bg_flat", 64, 64)
local stars = {}
local pts = { {9,8}, {27,17}, {44,6}, {61,12}, {80,5}, {96,9}, {104,21}, {118,4}, {131,14}, {150,7}, {163,18}, {176,5}, {186,12},
  {14,52}, {38,58}, {60,49}, {183,55}, {170,46} }
for n, p in ipairs(pts) do stars[#stars + 1] = { p[1], p[2], n % 3 == 0 and "#8F9AC8" or "#4A5585" } end
flat("banner_bg", 192, 64, stars)

-- lettering: TRAVELER'S in cream over LANTERN in lantern gold, iron extrude, tagline
local WARM = { "#FFF7D6", "#FFE08A", "#FFE08A", "#FFE08A", "#FFC14D", "#FFC14D", "#FFC14D", "#FF9A2E", "#FF9A2E", "#FF9A2E" }
local CREAM = { "#FFFFFF", "#FFF7E6", "#FFF7E6", "#FFF7E6", "#F2E2C4", "#F2E2C4", "#F2E2C4", "#DCC39B", "#DCC39B", "#DCC39B" }
local EXTRUDE = { "#4C5578", "#2E3450" }
PA.title_sprite(OUT .. "banner_title_top", "TRAVELER'S", { bands = CREAM, extrude = EXTRUDE, outline = "#0B0F20" })
PA.title_sprite(OUT .. "banner_title", "LANTERN", { bands = WARM, extrude = EXTRUDE, outline = "#0B0F20" })
PA.label_sprite(OUT .. "banner_tagline", "YOUR LIGHT. HANDS FREE.", function(i) return i > 12 and "#FFD45C" or "#FFFFFF" end,
  "#0B0F20")
