plugins {
    java
    id("io.quarkus") version "3.9.4" apply false
}

allprojects {
    group = "com.myRepository"
    version = "1.0.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    java {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }

    dependencies {
        val testImplementation by configurations
        testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}