package io.github.asodja.test;

import io.github.asodja.junit5.extension.IntelliJGradleQuarkusTestExtension;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@ExtendWith(IntelliJGradleQuarkusTestExtension.class)
@QuarkusTest
public class GreetingResourceTest {

    @Test
    public void testHelloEndpoint() {
        System.out.println(new MyPackage());
        given()
                .when().get("/hello")
                .then()
                .statusCode(200)
                .body(is("Hello RESTEasy"));
    }

}