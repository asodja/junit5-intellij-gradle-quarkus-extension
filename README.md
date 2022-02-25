# IntelliJ Gradle Quarkus Junit extension

Experimental Junit extension that makes Quarkus run fast when using Gradle project with JUnit also with IntelliJ runner.

On the `main` branch is version compatible with Quarkus `2.7.2`.

For Quarkus `2.7.1` and `2.7.0` compatible extension is in `2.7.1` branch.

For Quarkus `2.6.x` and `2.5.x` compatible extension is in `2.6.x` branch.

## Usage
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