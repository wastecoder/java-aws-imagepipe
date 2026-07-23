import info.solidsoft.gradle.pitest.PitestPluginExtension

plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
    alias(libs.plugins.pitest) apply false
}

// Classes with no behaviour worth asserting on. Filtered out of both the JaCoCo
// report and the verification gate, so the numbers describe real code only.
val coverageExclusions = listOf(
    "**/*Application.class",
    "**/adapter/config/**",
    "**/*Config.class",
    "**/entity/*Entity.class",
    "**/dto/**",
)

subprojects {
    apply(plugin = "java")
    apply(plugin = "jacoco")
    apply(plugin = "info.solidsoft.pitest")

    group = "com.wastecoder"
    version = "0.0.1-SNAPSHOT"

    repositories { mavenCentral() }

    val sourceSets = extensions.getByType<SourceSetContainer>()

    extensions.configure<JavaPluginExtension> {
        toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
    }

    // Integration tests live in their own source set so `build` stays green on a
    // machine with no Docker running. Testcontainers belongs here, never in `test`.
    sourceSets.create("integrationTest") {
        compileClasspath += sourceSets["main"].output + sourceSets["test"].output
        runtimeClasspath += sourceSets["main"].output + sourceSets["test"].output
    }
    configurations["integrationTestImplementation"]
        .extendsFrom(configurations["testImplementation"])
    configurations["integrationTestRuntimeOnly"]
        .extendsFrom(configurations["testRuntimeOnly"])

    // The domain kernel declares zero dependencies of its own, so the test
    // baseline is injected here instead. See PROJECT.md section 5.
    dependencies {
        add("testImplementation", platform(rootProject.libs.junit.bom))
        add("testImplementation", rootProject.libs.junit.jupiter)
        add("testImplementation", rootProject.libs.assertj.core)
        add("testRuntimeOnly", rootProject.libs.junit.platform.launcher)
    }

    extensions.configure<JacocoPluginExtension> {
        toolVersion = rootProject.libs.versions.jacoco.get()
    }

    tasks.withType<Test>().configureEach { useJUnitPlatform() }

    tasks.register<Test>("integrationTest") {
        description = "Runs the integration tests (Testcontainers). Requires Docker."
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        testClassesDirs = sourceSets["integrationTest"].output.classesDirs
        classpath = sourceSets["integrationTest"].runtimeClasspath
        shouldRunAfter(tasks.named("test"))
    }

    fun filtered(dirs: FileCollection): FileCollection =
        files(dirs.files.map { fileTree(it) { exclude(coverageExclusions) } })

    tasks.named<JacocoReport>("jacocoTestReport") {
        dependsOn(tasks.named("test"))
        classDirectories.setFrom(filtered(classDirectories))
        reports {
            xml.required.set(true)
            csv.required.set(true)
        }
    }

    // Coverage demanded scales with distance from the core: the hexagon promises
    // the centre is testable without infrastructure, so covering it is cheap.
    tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
        dependsOn(tasks.named("jacocoTestReport"))
        classDirectories.setFrom(filtered(classDirectories))
        violationRules {
            rule {
                element = "PACKAGE"
                includes = listOf(
                    "com.wastecoder.imagepipe.shared",
                    "com.wastecoder.imagepipe.*.domain.*",
                )
                limit { counter = "LINE";   value = "COVEREDRATIO"; minimum = "0.95".toBigDecimal() }
                limit { counter = "BRANCH"; value = "COVEREDRATIO"; minimum = "0.90".toBigDecimal() }
            }
            rule {
                element = "PACKAGE"
                includes = listOf("com.wastecoder.imagepipe.*.application.*")
                limit { counter = "LINE";   value = "COVEREDRATIO"; minimum = "0.90".toBigDecimal() }
                limit { counter = "BRANCH"; value = "COVEREDRATIO"; minimum = "0.85".toBigDecimal() }
            }
            rule {
                element = "PACKAGE"
                includes = listOf("com.wastecoder.imagepipe.*.adapter.*")
                limit { counter = "LINE";   value = "COVEREDRATIO"; minimum = "0.60".toBigDecimal() }
                limit { counter = "BRANCH"; value = "COVEREDRATIO"; minimum = "0.50".toBigDecimal() }
            }
        }
    }

    tasks.named("check") { dependsOn(tasks.named("jacocoTestCoverageVerification")) }

    // Pitest thresholds are per project, not per package, so the adapter is kept
    // out of the target set entirely and the threshold is set per module.
    val isKernel = project.name == "shared"
    extensions.configure<PitestPluginExtension> {
        pitestVersion.set(rootProject.libs.versions.pitest.get())
        junit5PluginVersion.set(rootProject.libs.versions.pitestJunit5.get())
        targetClasses.set(
            if (isKernel) {
                setOf("com.wastecoder.imagepipe.shared.*")
            } else {
                setOf(
                    "com.wastecoder.imagepipe.${project.name}.domain.*",
                    "com.wastecoder.imagepipe.${project.name}.application.*",
                )
            }
        )
        mutationThreshold.set(if (isKernel) 90 else 85)
        // The skeleton modules have no mutants yet. Without this, Phase 0a fails.
        failWhenNoMutations.set(false)
        timestampedReports.set(false)
    }
}
