plugins {
    id("java-library")
    id("maven-publish")
    id("signing")
}

val quarkusVersion: String by project
dependencies {
    compileOnly(enforcedPlatform("io.quarkus.platform:quarkus-bom:$quarkusVersion"))
    compileOnly("io.quarkus:quarkus-junit5")
}

tasks.compileJava {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.compileTestJava {
    options.encoding = "UTF-8"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<GenerateModuleMetadata>() {
    enabled = false
}

tasks.withType<Sign> {
    onlyIf { !(version as String).endsWith("-SNAPSHOT") }
}

publishing {
    repositories {
        maven {
            url = when {
                (version as String).endsWith("-SNAPSHOT") -> uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
                else -> uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            }
            credentials {
                username = System.getenv("MAVEN_CENTRAL_USERNAME")
                password = System.getenv("MAVEN_CENTRAL_PASSWORD")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = "io.github.asodja"
            artifactId = "junit5-intellij-gradle-quarkus-extension"
            pom {
                description.set("Junit5 extension that makes Quarkus run fast when using Gradle project with IntelliJ runner.")
                url.set("https://github.com/asodja/junit5-intellij-gradle-quarkus-extension")
                name.set("junit5-intellij-gradle-quarkus-extension")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("http://www.opensource.org/licenses/mit-license.php")
                    }
                }
                developers {
                    developer {
                        name.set("Anže Sodja")
                        url.set("https://github.com/asodja")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/asodja/junit5-intellij-gradle-quarkus-extension.git")
                    developerConnection.set("scm:git:ssh://github.com:asodja/junit5-intellij-gradle-quarkus-extension.git")
                    url.set("https://github.com/asodja/junit5-intellij-gradle-quarkus-extension")
                }
            }
        }
    }
}

afterEvaluate {
    signing {
        sign(publishing.publications["maven"])
    }
}
