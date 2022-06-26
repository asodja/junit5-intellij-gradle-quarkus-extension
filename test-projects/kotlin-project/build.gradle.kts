plugins {
    id("java")
    id("io.quarkus")
    id("java-test-fixtures")
    id("org.jetbrains.kotlin.jvm") version "1.6.20"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.6.20"
}

val quarkusVersion: String by project
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.20")
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:$quarkusVersion"))
    implementation("io.quarkus:quarkus-resteasy")
    implementation(project(":kotlin-library"))
    testImplementation(project(":junit5-extension"))
    testImplementation(testFixtures(project(":java-fixtures")))
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
}

allOpen {
    annotation("javax.ws.rs.Path")
    annotation("javax.enterprise.context.ApplicationScoped")
    annotation("io.quarkus.test.junit.QuarkusTest")
}

tasks.compileKotlin {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
    kotlinOptions.javaParameters = true
}

tasks.compileTestKotlin {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
}