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

Build all supported versions:

```powershell
.\gradlew.bat buildAndCollect
```

Build only Minecraft 26.1.2:

```powershell
.\gradlew.bat :26.1.2:buildAndCollect
```

Replace `26.1.2` with another supported target to build that version.

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

Set `JAVA_HOME` to your JDK 25 installation if Gradle selects a different Java version.

## Build output

Collected files appear in:

```text
build/libs/1.4.1/
```

The folder name follows `mod.version` in `stonecutter.properties.toml`.

For Minecraft 26.1.2, install:

```text
EdenMCRegions-1.4.1+mc26.1.2.jar
```

Do **not** install the file ending in `-sources.jar`. Use only the JAR matching your Minecraft version, and remove the older EdenMCRegions JAR before replacing it.

To play, install Fabric Loader 0.19.3 or newer and the Fabric API version required by that target. Minecraft 1.21.x builds use Java 21 at runtime; 26.x builds use Java 25. All targets are built using JDK 25.

## Error logs

Mod errors are appended to `logs/edenmcregions-errors.txt` inside the Minecraft instance folder.
Entries include a UTC timestamp, the operation that failed, and the full exception with its causes.
This covers map downloads, map parsing, cache and request timestamp files, and settings loading or saving.
Errors also appear in Minecraft's normal log. Once the dedicated log reaches 1 MiB, it is rotated to
`logs/edenmcregions-errors.previous.txt`, keeping one previous file.

## License

[MIT](LICENSE). Map data belongs to its respective contributors.
