import de.gesellix.gradle.docker.tasks.DockerBuildTask
import de.gesellix.gradle.docker.tasks.DockerPushTask
import de.gesellix.gradle.docker.tasks.DockerRmiTask

plugins {
    kotlin("jvm") version "1.3.20"
    application
    id("de.gesellix.docker") version "2019-04-07T21-31-01"
}

group = "ru.kotlin566.messenger"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
    mavenCentral()
    maven {
        setUrl("https://kotlin.bintray.com/ktor")
    }
}

val kotlinVersion : String by project
val ktorVersion : String by project
val logbackVersion: String by project
val springSecurityVersion: String by project

dependencies {
    compile ("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    compile ("io.ktor:ktor-server-netty:$ktorVersion")
    compile ("io.ktor:ktor-server-core:$ktorVersion")
    compile ("io.ktor:ktor-auth:$ktorVersion")
    compile ("io.ktor:ktor-auth-jwt:$ktorVersion")
    compile ("io.ktor:ktor-jackson:$ktorVersion")
    compile ("ch.qos.logback:logback-classic:$logbackVersion")

    compile ("org.springframework.security:spring-security-config:$springSecurityVersion")

    testCompile ("io.ktor:ktor-server-tests:$ktorVersion")
    testCompile ("org.junit.jupiter:junit-jupiter-api:5.0.2")
    testCompile("org.junit.jupiter:junit-jupiter-engine:5.0.2")
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform ()
}

application {
    mainClassName = "ru.kotlin566.messenger.server.ApplicationKt"
}
val build = if (project.hasProperty("teamcity")) {
        (project.properties["teamcity"] as Map<*, *>) ["build.number"]
    }
    else {
        "local_build"
    } as String
version = build

val dockerImageName = "kotlin566/kotlin-messenger:$version"
val dockerRegistry = "registry.promoatlas.ru"
println("Version = $version")
tasks {
    val removeLocalDockerImage = register<DockerRmiTask>("removeLocalDockerImage") {
        imageId = dockerImageName
    }
    val removeTaggedDockerImage = register<DockerRmiTask>("removeTaggedDockerImage") {
        imageId = "$dockerRegistry/$dockerImageName"
    }
    register<Task>("removeDockerImages") {
        group = "Docker"
        dependsOn(removeLocalDockerImage, removeTaggedDockerImage)
    }

    val buildDockerImage = register<DockerBuildTask>("buildDockerImage") {
        imageName = dockerImageName
        setBuildContextDirectory(file("./"))
        println("Building $dockerImageName")
    }

    register<DockerPushTask>("pushDockerImage") {
        dependsOn(buildDockerImage)
        repositoryName = dockerImageName
        registry = dockerRegistry
        println("Publishing $dockerRegistry/$dockerImageName")
    }
}