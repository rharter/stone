plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.annotation)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

val stoneSds = objects.sourceDirectorySet("stone", "Stone sources")
stoneSds.srcDirs("src/test/stone")
stoneSds.include("**/*.stone")
val stoneTask = tasks.register<Exec>("generateStone") {
    outputs.dir(layout.buildDirectory.dir("generated/sources/stone/"))
    outputs.upToDateWhen { false }
    inputs.files(stoneSds.files)

    workingDir = layout.projectDirectory.dir("../..").asFile
    commandLine(
        listOf(
            "python",
            "-m", "stone.cli",
            "--attribute", ":all",
            "java_types",
            outputs.files.singleFile.absolutePath,
            *inputs.files.files.map { it.absolutePath }.toTypedArray(),
            "--", "--package", "com.dropbox.stone"
        ),
    )
}

sourceSets.configureEach {
    java.srcDirs(stoneTask)
}
tasks.withType<JavaCompile>().configureEach {
    dependsOn(stoneTask)
}

