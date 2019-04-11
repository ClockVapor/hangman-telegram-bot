import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.21"
}

group = "clockvapor.telegram"
version = "0.0.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("test-junit"))
    implementation("com.github.clockvapor", "telegram-utils", "0.0.1")
    implementation("com.xenomachina", "kotlin-argparser", "2.0.7")
    implementation("io.github.seik", "kotlin-telegram-bot", "0.3.5") {
        exclude("io.github.seik.kotlin-telegram-bot", "echo")
        exclude("io.github.seik.kotlin-telegram-bot", "dispatcher")
    }
    implementation("org.jsoup", "jsoup", "1.11.3")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

task<Jar>("fatJar") {
    manifest {
        attributes(mapOf("Main-Class" to "clockvapor.telegram.hangman.HangmanTelegramBot"))
    }
    from(configurations.runtimeClasspath
        .filter { it.exists() }
        .map { if (it.isDirectory) it else zipTree(it) }
    )
    with(tasks["jar"] as CopySpec)
}

task("stage") {
    dependsOn("clean", "fatJar")
}
