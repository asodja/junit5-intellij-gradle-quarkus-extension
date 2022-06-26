pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("io.quarkus") version providers.gradleProperty("quarkusVersion")
    }
}
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "quarkus-junit-extension"

productionProjects(
    "junit5-extension"
)

testProjects(
    "java-fixtures",
    "java-project",
    "kotlin-project",
    "kotlin-library"
)

fun productionProjects(vararg names: String) {
    names.forEach { name: String ->
        include(name)
        project(":$name").projectDir = file("projects/$name")
    }
}

fun testProjects(vararg names: String) {
    names.forEach { name: String ->
        include(name)
        project(":$name").projectDir = file("test-projects/$name")
    }
}