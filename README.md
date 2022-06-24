# JUnit5 IntelliJ Gradle Quarkus extension

JUnit5 extension that makes Quarkus JUnit5 tests run fast for Gradle project when using IntelliJ IDEA runner.

Table of compatible versions
| Extension version | Quarkus version |
|  :---: |  :---:  |
| 2.0.0.alpha01 | 2.10.0 |
| 1.0.0.alpha01 | TBD |


## Usage

### Define dependency
```
repositories {
    mavenCentral()
}

dependencies {
    // Quarkus dependencies has to be defined manually by the user
    testImplementation("io.quarkus:quarkus-junit5:2.10.0.Final")
    testImplementation("io.github.asodja:junit5-intellij-gradle-quarkus-extension:2.0.0.alpha01")
}
```

### Define JUnit5 extension

```
// IntelliJGradleQuarkusTestExtension must be used applied before @QuarkusTest 
@ExtendWith(IntelliJGradleQuarkusTestExtension.class)
@QuarkusTest
public class GreetingResourceTest {
}
```

or 
```
@ExtendWith(IntelliJGradleQuarkusTestExtension.class)
public class MyBaseTest {
}

@QuarkusTest
public class GreetingResourceTest extends MyBaseTest {
}
```

### Usage inside the IntelliJ IDEA
After you update the project dependencies click IntelliJ IDEA button "Load Gradle changes" or "Reload All Gradle projects". After that you can run Unit tests. Extension will automatically detect classpath changes, and it will update Quarkus App model via Gradle and cache it. On the next run, extension will use cached Quarkus App model, and it will run much faster.

In case changes are not detected, you can manually delete model in `out/my.quarkus-test-model-*.dat` and extension will rebuild model again.
