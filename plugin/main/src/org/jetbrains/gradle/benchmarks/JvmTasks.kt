package org.jetbrains.gradle.benchmarks

import org.gradle.api.*
import org.gradle.api.artifacts.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.compile.*

fun Project.createJvmBenchmarkCompileTask(config: BenchmarkConfiguration, compileClasspath: FileCollection) {
    val benchmarkBuildDir = benchmarkBuildDir(config)
    task<JavaCompile>(
        "${config.name}${BenchmarksPlugin.BENCHMARK_COMPILE_SUFFIX}",
        depends = BenchmarksPlugin.ASSEMBLE_BENCHMARKS_TASKNAME
    ) {
        group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
        description = "Compile JMH source files for '${config.name}'"
        dependsOn("${config.name}${BenchmarksPlugin.BENCHMARK_GENERATE_SUFFIX}")
        classpath = compileClasspath
        source = fileTree("$benchmarkBuildDir/sources")
        destinationDir = file("$benchmarkBuildDir/classes")
    }
}

fun Project.createJmhGenerationRuntimeConfiguration(name: String, jmhVersion: String): Configuration {
    // This configuration defines classpath for JMH generator, it should have everything available via reflection
    return configurations.create("$name${BenchmarksPlugin.BENCHMARK_GENERATE_SUFFIX}CP").apply {
        isVisible = false
        description = "JMH Generator Runtime Configuration for '$name'"

        val dependencies = this@createJmhGenerationRuntimeConfiguration.dependencies
        @Suppress("UnstableApiUsage")
        (defaultDependencies {
            it.add(dependencies.create("${BenchmarksPlugin.JMH_GENERATOR_DEPENDENCY}$jmhVersion"))
        })
    }
}

fun Project.createJvmBenchmarkGenerateSourceTask(
    config: BenchmarkConfiguration,
    workerClasspath: FileCollection,
    compileClasspath: FileCollection,
    compilationTask: String,
    compilationOutput: FileCollection
) {
    val benchmarkBuildDir = benchmarkBuildDir(config)
    task<JmhBytecodeGeneratorTask>("${config.name}${BenchmarksPlugin.BENCHMARK_GENERATE_SUFFIX}") {
        group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
        description = "Generate JMH source files for '${config.name}'"
        dependsOn(compilationTask)
        runtimeClasspath = workerClasspath
        inputCompileClasspath = compileClasspath
        inputClassesDirs = compilationOutput
        outputResourcesDir = file("$benchmarkBuildDir/resources")
        outputSourcesDir = file("$benchmarkBuildDir/sources")
    }
}

fun Project.createJvmBenchmarkExecTask(
    config: BenchmarkConfiguration,
    runtimeClasspath: FileCollection
) {
    task<JavaExec>(
        "${config.name}${BenchmarksPlugin.BENCHMARK_EXEC_SUFFIX}",
        depends = BenchmarksPlugin.RUN_BENCHMARKS_TASKNAME
    ) {
        val benchmarkBuildDir = benchmarkBuildDir(config)
        val reportsDir = benchmarkReportsDir(config)
        val reportFile = reportsDir.resolve("${config.name}.json")
        group = BenchmarksPlugin.BENCHMARKS_TASK_GROUP
        description = "Execute benchmark for '${config.name}'"
        main = "org.jetbrains.gradle.benchmarks.jvm.JvmBenchmarkRunnerKt"
        classpath(
            file("$benchmarkBuildDir/classes"),
            file("$benchmarkBuildDir/resources"),
            runtimeClasspath
        )
        
        //args = "-w 5 -r 5 -wi 1 -i 1 -f 1 
        args(listOf(reportFile.toString(), config.iterations(), config.iterationTime())) // TODO: configure!
        
        dependsOn("${config.name}${BenchmarksPlugin.BENCHMARK_COMPILE_SUFFIX}")
        doFirst {
            reportsDir.mkdirs()
            logger.lifecycle("Running benchmarks for ${config.name}")
            logger.info("    I:${config.iterations()} T:${config.iterationTime()}")
        }
    }
}


