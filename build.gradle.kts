import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	kotlin("jvm") version "1.3.71"
	application
}

group = "com.github.lastexceed"
version = "0.1.0"

repositories {
	mavenCentral()
}

dependencies {
	implementation(kotlin("stdlib-jdk8"))
	implementation("org.javacord", "javacord", "3.0.5")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		jvmTarget = "13"
	}
}

application {
	mainClassName = "MainKt"
}