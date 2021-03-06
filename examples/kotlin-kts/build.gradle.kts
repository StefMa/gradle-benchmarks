@file:Suppress("UnstableApiUsage")

import org.jetbrains.gradle.benchmarks.*
import org.jetbrains.kotlin.allopen.gradle.*
import org.jetbrains.kotlin.gradle.tasks.*

plugins {
    java
    kotlin("jvm") version "1.3.20"
    kotlin("plugin.allopen") version "1.3.20"
    id("org.jetbrains.gradle.benchmarks.plugin") version "0.1.6" 
}

repositories {
    mavenCentral()
    maven { setUrl("https://dl.bintray.com/kotlin/kotlin-eap") }
    maven { setUrl("https://dl.bintray.com/kotlin/kotlin-dev") }
}

sourceSets.all {
    java.srcDir("$name/src")
    resources.srcDir("$name/resources")
}

configure<AllOpenExtension> {
    annotation("org.openjdk.jmh.annotations.State") 
}

dependencies {
    kotlin("kotlin-stdlib-jdk8")    
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

configure<BenchmarksExtension> {
    configurations {
        register("main") {
            //jmhVersion = "1.21"
        }
    }
}