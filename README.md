# SKYBREAKER: ORBITAL ARSENAL  (Minecraft 26.3 · Fabric · Java 25)

You do NOT need to install Java, Gradle or anything else. GitHub builds the mod for you, for free, online.

## Get the .jar (about 5 minutes)

1. Make a free account at https://github.com and sign in.
2. Click **+** (top right) -> **New repository**. Name it `skybreaker`. Choose Public or Private. Click **Create repository**.
3. Unzip `skybreaker-project.zip` on your PC. Open the unzipped folder.
4. On the new repo page click **uploading an existing file**. Select EVERYTHING inside the unzipped folder
   (including the `.github`, `gradle`, `src` folders and `gradlew`) and drag it into the browser.
   Wait until all files finish uploading, then click **Commit changes**.
   (If `.github` is hidden in your file explorer, turn on View -> Show -> Hidden items.)
5. Click the **Actions** tab. A run called "Build Skybreaker" starts by itself (first run: allow "I understand my workflows" if asked).
   If it doesn't start: Actions -> Build Skybreaker -> **Run workflow**.
6. Wait ~3-6 minutes for the green checkmark.
7. Download the jar, either way:
   - **Releases** (right sidebar of the repo main page) -> `latest` -> click **skybreaker-26.3.jar**  (direct .jar download), or
   - the run page -> **Artifacts** -> `skybreaker-26.3` (a zip containing the jar; unzip it).

If the run shows a RED X, open it, copy the red error text and send it to me - I will fix it.

## Install
1. In SKLauncher make/select a **Fabric 26.3** instance (Fabric Loader 0.20.1 or newer).
2. Put into its `mods` folder: `skybreaker-26.3.jar` AND **Fabric API 0.165.0+26.3** (https://modrinth.com/mod/fabric-api).
3. Launch.

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
