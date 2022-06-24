plugins {
    id("java-library")
    id("io.quarkus")
    id("java-test-fixtures")
}

val quarkusVersion: String by project
dependencies {
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:$quarkusVersion"))
    implementation("io.quarkus:quarkus-resteasy")
    testImplementation(project(":junit5-extension"))
    testImplementation(testFixtures(project(":java-fixtures")))
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
}

tasks.compileJava {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.compileTestJava {
    options.encoding = "UTF-8"
}