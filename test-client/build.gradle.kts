plugins {
    id("org.jetbrains.kotlin.jvm") version "1.3.20"
}

group = "ru.kotlin566.messenger"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val springSecurityVersion: String by project
val junitVersion: String by project

dependencies {

    compile (project(":server"))
    compile ("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compile ("org.jetbrains.kotlin:kotlin-reflect")
    compile ("org.springframework.security:spring-security-config:$springSecurityVersion")

    testImplementation ("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly ("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform ()
}