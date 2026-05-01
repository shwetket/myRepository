plugins {
    id("io.quarkus")
}

quarkusDev {
    jvmArgs = mutableListOf("--add-opens", "java.base/java.lang=ALL-UNNAMED")
}

dependencies {
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:3.29.0"))
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-hibernate-orm-panache")
    implementation("io.quarkus:quarkus-jdbc-postgresql")
    implementation("io.quarkus:quarkus-flyway")
    implementation("io.quarkus:quarkus-smallrye-openapi")
    implementation(project(":shared"))
    testImplementation("io.quarkus:quarkus-junit5")
}