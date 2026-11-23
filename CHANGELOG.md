# Changelog

## 0.2.20 — 2026-11-23

- New: Minecraft 1.21.11 on Fabric, Quilt, NeoForge and Forge.
- The swing is smoother. Sneaking, jumping, quick turns and body-animation packs used to throw the lantern across in a single frame; the lantern now moves with a sudden pose change and its drawn swing eases to the simulation (never faster than about 600 degrees a second).
- New: a settings screen (Mod Menu on Fabric and Quilt, the mod list on NeoForge and Forge) with clink sounds on/off, clink volume (0 is silent), hip side, lantern light, smooth light, light updates per second and softer daylight light. `clinkVolume` (0 to 100) is also in `config/travelers_lantern.json`.
- Shader packs with Iris get a colour for the held lantern light: amber for a lantern, blue for a soul lantern, green for the copper lanterns (packs that read it, or colour by item such as Complementary). Other players' lanterns light as before.

## 0.2.19 — 2026-11-15

- Minecraft 1.21.1 Forge gets 0.2.18's full-strength daylight light. It was held back by a problem in our test setup, not in the mod: nothing changes for players.

## 0.2.18 — 2026-11-15

- The lantern now shines at full strength in daylight too. It used to dim itself under open sky, so it seemed to work only in shade, caves or at night. The old behaviour is still there as `softInDaylight` in `config/travelers_lantern.json` (off by default; this also resets the old `adaptiveLight` setting).

## 0.2.17 — 2026-11-10

- Fixed the lantern's light skipping the tops of ledges, blocks and floors above the lantern's height with Sodium: they now light up like everything else around you.

## 0.2.16 — 2026-11-10

- New: Minecraft 1.21.1 on NeoForge and Forge, and Minecraft 1.20.1 on Fabric, Quilt, Forge and NeoForge. The lantern, the swing, the smooth light and Sodium support work as on newer versions. On 1.20.1 other players see your lantern when the server has the mod.
- Fixed dark corners next to blocks popping on and off around the lantern's light without Sodium (the lantern no longer lights the inside of solid blocks).

## 0.2.15 — 2026-11-09

- Fixed a crash with Sodium 0.9.3 beta 2 and newer (Sodium renamed the quad accessors the lantern light reads).

## 0.2.14 — 2026-10-14

- Fixed a crash on Minecraft 26.2 when Sodium is installed.
