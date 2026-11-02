# EdenMCRegions

A client-side Fabric mod that displays your nation, region, and territory states using the [Eden community map](https://map.edenmc.lol/).

## Requirements

- **JDK 25** to build the project. A JDK is required, not just a Java runtime.
- An internet connection for Gradle to download dependencies on the first build.
- No separate Gradle installation is needed; use the included wrapper.

Supported Minecraft targets: **1.21.8, 1.21.9, 1.21.10, 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2, and 26.3**.

## Build from a terminal

Open a terminal in the repository root, where `gradlew`, `gradlew.bat`, and `settings.gradle.kts` are located.

### Windows PowerShell

Check the installed Java version:

```powershell
java -version
```

If Gradle is using the wrong Java installation, set `JAVA_HOME` to your JDK 25 installation for the current terminal session:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-25'
```

Build public and experimental JARs for all supported versions:

```powershell
.\gradlew.bat buildAndCollect
```

Build only the public Minecraft 26.1.2 JAR:

```powershell
.\gradlew.bat :26.1.2:buildAndCollect
```

Replace `26.1.2` with another supported target to build that version.

Build its experimental variant:

```powershell
.\gradlew.bat :26.1.2-experimental:buildAndCollect
```

### Linux and macOS

Make the wrapper executable if necessary, then build:

```sh
chmod +x gradlew
./gradlew buildAndCollect
```

To build only one target:

```sh
./gradlew :26.1.2:buildAndCollect
```

Use `:26.1.2-experimental:buildAndCollect` for the experimental variant.

Set `JAVA_HOME` to your JDK 25 installation if Gradle selects a different Java version.

## Build output

Collected files appear in:

```text
dist/public/1.4.1/
dist/experimental/1.4.1/
```

The version folder follows `mod.version` in `stonecutter.properties.toml`. These folders contain installable JARs only.
Gradle also builds source JARs in each target's `versions/<target>/build/libs/` folder.

For Minecraft 26.1.2, install:

```text
EdenMCRegions-1.4.1+mc26.1.2.jar
```

The experimental equivalent is `EdenMCRegions-1.4.1-experimental+mc26.1.2.jar`.
Install **one variant**, not both: they use the same mod ID and settings file.

Do **not** install the file ending in `-sources.jar`. Use only the JAR matching your Minecraft version, and remove the older EdenMCRegions JAR before replacing it.

To play, install Fabric Loader 0.19.3 or newer and the Fabric API version required by that target. Minecraft 1.21.x builds use Java 21 at runtime; 26.x builds use Java 25. All targets are built using JDK 25.

## Experimental features

Both variants are built from this repository using Stonecutter's `experimental` condition.
The public binary excludes experimental controls and their behavior; editing the config cannot enable them.
The experimental build includes **Settings → Experiment → Overworld borders in Nether**, disabled by default.
When enabled, Nether locations use Overworld borders at the same X/Z coordinates, without 8:1 scaling.
The existing Overworld cache and refresh interval are shared. This assumes matching borders in both dimensions.

## Build on GitHub

After uploading this repository, open **Actions → Build distributions → Run workflow**.
On completion, the run provides separate `EdenMCRegions-public` and `EdenMCRegions-experimental` artifacts.
Each contains JARs for all supported Minecraft versions. To run it yourself without repository write access,
fork the repository and enable Actions in your fork. The workflow builds files; it does not publish releases or upload to CurseForge.

## Error logs

Mod errors are appended to `logs/edenmcregions-errors.txt` inside the Minecraft instance folder.
Entries include a UTC timestamp, the operation that failed, and the full exception with its causes.
This covers map downloads, map parsing, cache and request timestamp files, and settings loading or saving.
Errors also appear in Minecraft's normal log. Once the dedicated log reaches 1 MiB, it is rotated to
`logs/edenmcregions-errors.previous.txt`, keeping one previous file.

## License

[MIT](LICENSE). Map data belongs to its respective contributors.
