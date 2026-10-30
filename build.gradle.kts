plugins {
    id("dev.kikugie.loom-back-compat")
}

val mcVersion = sc.current.version
val requiredJava = if (sc.current.parsed >= "26.1") 25 else 21
val fabricApiVersion = sc.properties.get<String>("deps.fabric_api")

version = "${property("mod.version")}+mc$mcVersion"
base.archivesName = property("mod.name") as String

repositories {
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    loomx.applyMojangMappings()
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")
    runConfigs.configureEach {
        runDirectory = rootProject.file("run/$mcVersion")
    }
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
    sourceCompatibility = JavaVersion.toVersion(requiredJava)
    targetCompatibility = JavaVersion.toVersion(requiredJava)
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.release = requiredJava
}

tasks.processResources {
    val properties = mapOf(
        "version" to project.version,
        "minecraft" to mcVersion,
        "java" to requiredJava,
        "api" to fabricApiVersion
    )
    inputs.properties(properties)
    filesMatching("fabric.mod.json") { expand(properties) }
}

tasks.jar {
    from(rootProject.file("LICENSE")) { rename { "LICENSE-EdenMCRegions" } }
}


tasks.register<Copy>("buildAndCollect") {
    group = "build"
    description = "Builds this version and collects release and sources JARs."
    dependsOn(tasks.build)
    from(loomx.modJar.flatMap { it.archiveFile }, loomx.modSourcesJar.flatMap { it.archiveFile })
    into(rootProject.layout.buildDirectory.dir("libs/${project.property("mod.version")}"))
}
