plugins {
    kotlin("jvm") version "1.3.20"
    application
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
