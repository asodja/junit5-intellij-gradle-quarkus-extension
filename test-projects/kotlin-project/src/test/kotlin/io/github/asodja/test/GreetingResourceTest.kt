package io.github.asodja.test

import io.github.asodja.junit5.extension.IntelliJGradleQuarkusTestExtension
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(IntelliJGradleQuarkusTestExtension::class)
@QuarkusTest
class GreetingResourceTest {
    @Test
    fun testHelloEndpoint() {
        println(MyPackage())
        given()
            .`when`().get("/hello")
            .then()
            .statusCode(200)
            .body(`is`("Hello RESTEasy 42"))
    }
}