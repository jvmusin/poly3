@file:Suppress("UNUSED_VARIABLE")

import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktorVersion = "1.6.5"
val retrofitVersion = "2.9.0"
val serializationVersion = "1.1.0"
val kotestVersion = "4.4.3"
val koinVersion = "2.2.2"
val kotlinReactVersion = "17.0.1-pre.148-kotlin-1.4.30"
val kotlinStyledVersion = "5.2.1-pre.148-kotlin-1.4.30"
val okhttp3Version = "5.0.0-alpha.2"
val jsoupVersion = "1.13.1"
val reactTooltipVersion = "4.2.15"
val config4kVersion = "0.4.2"
val log4j2Version = "2.14.1"

plugins {
    kotlin("multiplatform") version "1.6.0"
    application
    kotlin("plugin.serialization") version "1.6.0-RC2"
}

group = "jvmusin"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
    mavenCentral()
    maven { url = uri("https://github.com/ktorio/ktor") }
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "15"
        }
        withJava()
    }
    js(IR) {
        browser {
            binaries.executable()
            commonWebpackConfig {
                cssSupport.enabled = true
            }
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    }
    sourceSets {
        all {
            languageSettings.apply {
                optIn("kotlin.ExperimentalStdlibApi")
                optIn("kotlin.time.ExperimentalTime")
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                optIn("kotlinx.serialization.ExperimentalSerializationApi")
            }
        }
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
            }
        }
        val commonTest by getting
        val jvmMain by getting {
            dependencies {
                implementation("com.squareup.retrofit2:retrofit:$retrofitVersion")
                implementation("com.squareup.retrofit2:converter-scalars:$retrofitVersion")

                implementation("com.squareup.okhttp3:logging-interceptor:$okhttp3Version")
                implementation("com.squareup.okhttp3:okhttp:$okhttp3Version")
                implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:0.8.0")

                implementation("io.ktor:ktor-serialization:$ktorVersion")
                implementation("io.ktor:ktor-server-core:$ktorVersion")
                implementation("io.ktor:ktor-server-netty:$ktorVersion")
                implementation("io.ktor:ktor-server-sessions:$ktorVersion")
                implementation("io.ktor:ktor-websockets:$ktorVersion")

                implementation("org.jsoup:jsoup:$jsoupVersion")

                implementation("org.apache.logging.log4j:log4j-api:$log4j2Version")
                implementation("org.apache.logging.log4j:log4j-core:$log4j2Version")
                implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4j2Version")

                implementation("io.ktor:ktor-client-cio-jvm:$ktorVersion")
                implementation("io.ktor:ktor-client-auth-jvm:$ktorVersion")
                implementation("io.ktor:ktor-client-logging-jvm:$ktorVersion")
                implementation("io.ktor:ktor-client-features:$ktorVersion")

                implementation("org.koin:koin-core:$koinVersion")
                implementation("org.koin:koin-core-ext:$koinVersion")
                implementation("org.koin:koin-ktor:$koinVersion")

                implementation("io.github.config4k:config4k:$config4kVersion")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("io.kotest:kotest-runner-junit5:$kotestVersion")
                implementation("io.kotest:kotest-assertions-core:$kotestVersion")
                implementation("io.kotest:kotest-property:$kotestVersion")

                implementation("org.koin:koin-test:$koinVersion")
                implementation("io.kotest:kotest-extensions-koin:$kotestVersion")
            }
        }
        val jsMain by getting {
            dependencies {
                implementation("org.jetbrains:kotlin-react:$kotlinReactVersion")
                implementation("org.jetbrains:kotlin-react-dom:$kotlinReactVersion")
                implementation("org.jetbrains:kotlin-styled:$kotlinStyledVersion")

                implementation("io.ktor:ktor-client-js:$ktorVersion")
                implementation("io.ktor:ktor-client-json-js:$ktorVersion")
                implementation("io.ktor:ktor-client-serialization-js:$ktorVersion")
                implementation("io.ktor:ktor-client-websockets-js:$ktorVersion")

                implementation(npm("react-tooltip", reactTooltipVersion, false))
            }
        }
        val jsTest by getting
    }
}

application {
    mainClass.set("server.ServerKt")
    // to set jvm args, put them below
    // applicationDefaultJvmArgs = listOf("-Dio.netty.noKeySetOptimization=true", "-Dio.netty.noUnsafe=true")
}

tasks.getByName<KotlinWebpack>("jsBrowserProductionWebpack") {
    outputFileName = "output.js"
}
tasks.getByName<KotlinWebpack>("jsBrowserDevelopmentWebpack") {
    outputFileName = "output.js"
}

tasks.getByName<Jar>("jvmJar") {
    // creates a big unoptimized js pack, replace 'Development' with 'Production' to make it work better
    val webpackTask = tasks.getByName<KotlinWebpack>("jsBrowserDevelopmentWebpack")
    dependsOn(webpackTask) // make sure JS gets compiled first
    from(File(webpackTask.destinationDirectory, webpackTask.outputFileName)) // bring output file along into the JAR
}

tasks.getByName<JavaExec>("run") {
    dependsOn(tasks.getByName<Jar>("jvmJar"))
    classpath(tasks.getByName<Jar>("jvmJar"))
}

// Alias "installDist" as "stage" (for cloud providers)
tasks.create("stage") {
    dependsOn(tasks.getByName("installDist"))
}

// only necessary until https://youtrack.jetbrains.com/issue/KT-37964 is resolved
distributions {
    main {
        contents {
            from("$buildDir/libs") {
                rename("${rootProject.name}-jvm", rootProject.name)
                into("lib")
            }
        }
    }
}

tasks.withType<KotlinCompile>().all {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events(*TestLogEvent.values())
        showExceptions = true
        showCauses = true
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
    }
}