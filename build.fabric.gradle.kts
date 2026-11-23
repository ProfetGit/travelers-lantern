plugins {
    id("net.fabricmc.fabric-loom")
}

val mc = stonecutter.current.version
val modId = property("mod.id") as String

version = "${property("mod.version")}+$mc-fabric"
group = property("mod.group") as String
base.archivesName = modId

repositories {
    maven("https://maven.terraformersmc.com/") {
        name = "TerraformersMC"
        content { includeGroup("com.terraformersmc") }
    }
}

dependencies {
    compileOnly(files(rootProject.layout.buildDirectory.dir("compat-stubs")).builtBy(rootProject.tasks.named("compatStubs")))
    minecraft("com.mojang:minecraft:$mc")
    implementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    // optional: Mod Menu's settings button; compile only, not shipped or required
    compileOnly("com.terraformersmc:modmenu:${property("deps.modmenu")}") { isTransitive = false }

    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

loom {
    runs.named("client") {
        client()
        runDir = "run"
        programArgs("--username", "LanternTester")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 25
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
    systemProperty("bl.root", rootProject.projectDir.absolutePath)
}

tasks.processResources {
    exclude("META-INF/mods.toml", "META-INF/neoforge.mods.toml")
    val props = mapOf(
        "version" to project.version.toString(),
        "mc" to mc,
        "name" to project.property("mod.name"),
        "description" to project.property("mod.description"),
        "author" to project.property("mod.author"),
        "homepage" to project.property("mod.homepage"),
        "fabric_loader" to project.property("deps.fabric_loader"),
        "mc_range" to ((findProperty("deps.mc_range") as String?) ?: "~$mc"),
        "java" to "25",
    )
    inputs.properties(props)
    filesMatching("fabric.mod.json") { expand(props) }
}

tasks.named<Jar>("jar") {
    from(rootProject.file("LICENSE"))
}

// 26.1 is older than 26.2 and uses the pre-26.2 names for most of what dev/renames.json lists: run the same rename pass.
if (mc.startsWith("26.1")) {
    extra["mcVersion"] = mc
    apply(from = rootProject.file("../../tools/Backport/renames.gradle.kts"))
}
