import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	kotlin("jvm") version "1.4-M2"
	application
}

group = "com.github.lastexceed"
version = "0.1.0"

repositories {
	mavenCentral()
	maven("https://dl.bintray.com/kotlin/kotlin-eap")
}

dependencies {
	implementation(kotlin("stdlib-jdk8"))
	implementation("org.javacord", "javacord", "3.0.6")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		jvmTarget = "13"
	}
}

application {
	mainClassName = "MainKt"
}