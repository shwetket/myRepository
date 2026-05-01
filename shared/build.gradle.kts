plugins {
    java
}

dependencies {
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:3.21.0"))
    implementation("io.quarkus:quarkus-arc")
}