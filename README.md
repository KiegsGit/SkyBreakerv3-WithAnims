# SKYBREAKER: ORBITAL ARSENAL  (Minecraft 26.3 · Fabric · Java 25)

## Use
Creative tab **Combat**, or `/give @s skybreaker:orbital_railgun` / `satellite_laser` / `meteor_barrage` / `black_hole`.
Hold the item: a ring shows where you're aiming (up to 128 blocks). **Right-click** to fire. Survival recipes are included.

## Singularity Cannon (black hole)
Right-click: a cinematic intro plays (black fade, title card, camera swings to the target, letterbox + countdown).
A tear opens 4 blocks above the target and becomes a black hole with a spinning accretion disk and polar jets.
For ~7 seconds it drags every living thing in 32 blocks toward it, rips loose blocks out of the ground and flings them in.
Anything that reaches the core is held there and crushed. Then it collapses: everything still stuck in the core is
killed for certain (totems and invulnerability do not save it), followed by a huge implosion blast, crater,
shockwave rings, lightning and bullet-time. You are never pulled in unless `damageOwner` is true.

## Visuals
Every particle is a custom texture made for this mod (plasma, white-hot cores, hot sparks, embers, fire, smoke, dust,
flares, violet energy wisps, the black hole body and accretion glow). Lightning is drawn by the mod itself: jagged,
branching, flickering bolts built from glowing particles. No vanilla lightning entity or vanilla explosion sprites.

## Config
`config/skybreaker.json` (created on first launch). Set `"terrainDestruction": false` to disable all block damage.
Also: per-weapon cooldown, countdown, damage, explosion radius, max blocks per strike, max block hardness, block-entity protection, player damage, camera shake.
New: `cinematicCamera` (eases your view toward the target during the intro), and the `blackHole*` options
(cooldown, countdown, pull time, pull radius, kill radius, collapse radius/damage, terrain).
