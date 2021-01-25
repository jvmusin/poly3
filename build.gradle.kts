import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

plugins {
    kotlin("multiplatform") version "1.4.21"
    application
    kotlin("plugin.serialization") version "1.4.21"
}

group = "jvmusin"
version = "1.0-SNAPSHOT"

val ktorVersion = "1.5.0"
val retrofitVersion = "2.9.0"
val serializationVersion = "1.0.0-RC"
val junitVersion = "5.6.0"

repositories {
    jcenter()
    mavenCentral()
    maven { url = uri("https://dl.bintray.com/kotlin/kotlin-js-wrappers") }
    maven { url = uri("https://dl.bintray.com/kotlin/kotlinx") }
    maven { url = uri("https://dl.bintray.com/kotlin/ktor") }
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "13"
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
        withJava()
    }
    js(IR) {
        browser {
            binaries.executable()
            webpackTask {
                cssSupport.enabled = true
            }
            runTask {
                cssSupport.enabled = true
            }
            testTask {
                useKarma {
                    useChromeHeadless()
                    webpackConfig.cssSupport.enabled = true
                }
            }
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.2")

                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
                implementation("com.squareup.retrofit2:retrofit:$retrofitVersion")
                implementation("com.squareup.retrofit2:converter-scalars:$retrofitVersion")

                implementation("com.squareup.okhttp3:logging-interceptor:4.10.0-RC1")
                implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:0.8.0")

                implementation("io.ktor:ktor-html-builder:$ktorVersion")
                implementation("io.ktor:ktor-serialization:$ktorVersion")
                implementation("io.ktor:ktor-server-core:$ktorVersion")
                implementation("io.ktor:ktor-server-netty:$ktorVersion")
                implementation("io.ktor:ktor-websockets:$ktorVersion")
                implementation("ch.qos.logback:logback-classic:1.3.0-alpha5")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
            }
        }
        val jsMain by getting {
            dependencies {
                implementation("org.jetbrains:kotlin-react:17.0.1-pre.141-kotlin-1.4.21")
                implementation("org.jetbrains:kotlin-react-dom:17.0.1-pre.141-kotlin-1.4.21")
                implementation("org.jetbrains:kotlin-styled:5.2.0-pre.141-kotlin-1.4.21")
                implementation("org.jetbrains:kotlin-react-router-dom:5.2.0-pre.141-kotlin-1.4.21")
                implementation("org.jetbrains:kotlin-redux:4.0.5-pre.141-kotlin-1.4.21")
                implementation("org.jetbrains:kotlin-react-redux:7.2.1-pre.141-kotlin-1.4.21")

                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-js:$ktorVersion")
                implementation("io.ktor:ktor-client-json-js:$ktorVersion")
                implementation("io.ktor:ktor-client-serialization-js:$ktorVersion")
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

application {
    mainClassName = "ServerKt"
}

tasks.getByName<KotlinWebpack>("jsBrowserProductionWebpack") {
    outputFileName = "output.js"
}

tasks.getByName<Jar>("jvmJar") {
    dependsOn(tasks.getByName("jsBrowserProductionWebpack"))
    val jsBrowserProductionWebpack = tasks.getByName<KotlinWebpack>("jsBrowserProductionWebpack")
    from(File(jsBrowserProductionWebpack.destinationDirectory, jsBrowserProductionWebpack.outputFileName))
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