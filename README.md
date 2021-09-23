# IntelliJ Gradle Quarkus Junit extension

Experimental Junit extension that makes Quarkus run fast when using Gradle project with JUnit also with IntelliJ runner.

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