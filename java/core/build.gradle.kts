plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.annotation)
    implementation(libs.jackson.core)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

val stoneTask = tasks.register<Exec>("generateStone") {
    val stoneSds = objects.sourceDirectorySet("stone", "Stone sources")
    stoneSds.srcDirs("src/test/stone")
    stoneSds.include("**/*.stone")
    inputs.files(stoneSds.files)

    // If any of the stone sources change we need to regenerate
    inputs.dir("../../stone")

    outputs.dir(layout.buildDirectory.dir("generated/sources/stone/"))

    workingDir = layout.projectDirectory.dir("../..").asFile
    executable(layout.projectDirectory.dir("../../.venv/bin/python"))
    args(
        "-m", "stone.cli",
        "--attribute", ":all",
        "java_types",
        outputs.files.singleFile.absolutePath,
        *stoneSds.files.map { it.absolutePath }.toTypedArray(),
        "--", "--package", "com.dropbox.stone"
    )
}

sourceSets.configureEach {
    java.srcDirs(stoneTask)
}
tasks.withType<JavaCompile>().configureEach {
    dependsOn(stoneTask)
}

