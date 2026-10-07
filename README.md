![Traveler's Lantern](https://raw.githubusercontent.com/ProfetGit/travelers-lantern/main/docs/banner.webp)

<p align="center">
<a href="https://github.com/ProfetGit"><img src="https://raw.githubusercontent.com/ProfetGit/assets/main/buttons/github.webp" alt="GitHub" width="23.96%"></a>
<a href="https://ko-fi.com/profetgit"><img src="https://raw.githubusercontent.com/ProfetGit/assets/main/buttons/kofi.webp" alt="Ko-fi" width="23.96%"></a>
</p>

**Your light, hands free.** Hang a lantern on your belt with one key. It lights your way wherever you walk, swings as you move, and leaves both hands free for your pickaxe, sword or shield. For Minecraft 26.2 and 26.3 on Fabric, NeoForge and Forge. It works on your client alone on any server, and with the mod on the server too, everyone sees each other's lanterns.

<p align="center">
<img src="https://raw.githubusercontent.com/ProfetGit/travelers-lantern/main/docs/clips/hook.webp" alt="Press B: the lantern goes from your hand to your belt" width="49%">
<img src="https://raw.githubusercontent.com/ProfetGit/travelers-lantern/main/docs/clips/walk.webp" alt="The lantern swings as you walk and jump, and the light moves with you" width="49%">
<img src="https://raw.githubusercontent.com/ProfetGit/travelers-lantern/main/docs/clips/cave.webp" alt="Both hands free in a dark cave, the light comes along" width="49%">
</p>

<p align="center">
<img src="https://raw.githubusercontent.com/ProfetGit/travelers-lantern/main/docs/clips/compare.webp" alt="Vanilla vs Traveler's Lantern on the same night walk, shaders off" width="88%">
</p>

![Features](https://raw.githubusercontent.com/ProfetGit/travelers-lantern/main/docs/desc/title-features.webp)

- **Hands-free light.** Press B and the lantern goes from your hand to your belt. Press B again to take it back.
- **It really swings.** The lantern hangs from your hip and swings as you walk, sprint, jump and turn, and gives a quiet clink when it knocks against your leg.
- **Smooth moving light.** The light glides along with the lantern instead of jumping a block at a time, and eases over steps and ledges. No other light mod needed.
- **Fits the moment.** A soft glow at noon or in a lit room, full strength at night and underground. It fades between the two as you walk in and out of caves.
- **Every lantern.** Lanterns, soul lanterns (a dimmer light), copper lanterns and lanterns from other mods.
- **Works on any server.** On a server without the mod, only you see your lantern. With the mod on the server, everyone sees everyone's lanterns and light, and even players without the mod get the light.

![How to use](https://raw.githubusercontent.com/ProfetGit/travelers-lantern/main/docs/desc/title-how-to-use.webp)

Carry a lantern and press **B**. The one in your hand goes on your belt first, otherwise one from your inventory. Press **B** again to take it off. Change the key in Controls.

On a server without the mod the lantern stays an ordinary item in your inventory, and your belt just shows it. On a server with the mod (and in singleplayer) it moves into the belt slot, so it's kept when you log out and drops like any item when you die (with `keepInventory` on, you keep it).

![Settings](https://raw.githubusercontent.com/ProfetGit/travelers-lantern/main/docs/desc/title-settings.webp)

The settings are in `config/travelers_lantern.json`: `adaptiveLight` (softer light in bright places), `smoothLight` (off: the light moves a block at a time, which is lighter on slow computers), `lightUpdatesPerSecond` (20 to 60), `clientLight` (the lantern lights things at all), `leftSide` (which hip) and `clinks`. On a server, operators can use `/travelerslantern light [true|false]` to turn the light off or on for players who don't have the mod.

![Compatibility](https://raw.githubusercontent.com/ProfetGit/travelers-lantern/main/docs/desc/title-compatibility.webp)

- Works with Sodium, and with Iris shaders: shader packs see the belt lantern as a held light.
- Works with Fresh Animations and its player animations: the lantern follows the animated hips.
- Client and server are both optional: a client with the mod can join any server, and a client without it can join a server that has it.

![Installation](https://raw.githubusercontent.com/ProfetGit/travelers-lantern/main/docs/desc/title-installation.webp)

Put the jar for your loader in your `mods` folder: Fabric, NeoForge or Forge, for Minecraft 26.2 and 26.3. No other mods needed. Before removing it from a server, take your lantern off (press B), or it waits in the hidden belt slot until the mod is back.

![Good to know](https://raw.githubusercontent.com/ProfetGit/travelers-lantern/main/docs/desc/title-good-to-know.webp)

- The light shines through walls, as with other moving-light mods.
- A soul lantern gives less light than a normal one, just like the blocks.

![More from Profet](https://raw.githubusercontent.com/ProfetGit/travelers-lantern/main/docs/desc/title-more-from-profet.webp)

<!-- promo:start -->
<p align="center">
<a href="https://www.curseforge.com/minecraft/mc-mods/tidy-pockets"><img src="https://raw.githubusercontent.com/ProfetGit/assets/main/promo/tidy-pockets.webp" alt="Tidy Pockets: One click. All sorted. Client side." width="49%"></a>
<a href="https://www.curseforge.com/minecraft/mc-mods/having-a-blast"><img src="https://raw.githubusercontent.com/ProfetGit/assets/main/promo/having-a-blast.webp" alt="Having a Blast: One boom. Bouncy blocks. Client or server." width="49%"></a>
<a href="https://www.curseforge.com/minecraft/mc-mods/far-out-zoom"><img src="https://raw.githubusercontent.com/ProfetGit/assets/main/promo/far-out-zoom.webp" alt="Far Out Zoom: The horizon, up close. Client side." width="49%"></a>
<a href="https://github.com/ProfetGit/overreacting-mobs"><img src="https://raw.githubusercontent.com/ProfetGit/assets/main/promo/overreacting-mobs.webp" alt="Overreacting Mobs: One hit. Big drama. Client side." width="49%"></a>
</p>
<!-- promo:end -->

![Support](https://raw.githubusercontent.com/ProfetGit/travelers-lantern/main/docs/desc/title-support.webp)

Traveler's Lantern is free. If it lights your way, a coffee helps fund the next update.

[![Support me on Ko-fi](https://raw.githubusercontent.com/ProfetGit/assets/main/kofi-banner.webp)](https://ko-fi.com/profetgit)

Want your own server to play on with friends? My BisectHosting affiliate link gives you 25% off the first month, and I get a small commission.

[![Get 25% off your first month at BisectHosting](https://raw.githubusercontent.com/ProfetGit/assets/main/bisecthosting-banner.webp)](https://url-shortener.curseforge.com/Pp2BN)

![License](https://raw.githubusercontent.com/ProfetGit/travelers-lantern/main/docs/desc/title-license.webp)

All rights reserved, with permissions: you can use it anywhere, include it in modpacks with credit, and show it in videos. Full terms: [LICENSE](https://github.com/ProfetGit/travelers-lantern/blob/main/LICENSE).

Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft.

![](https://raw.githubusercontent.com/ProfetGit/travelers-lantern/main/docs/desc/divider.webp)
