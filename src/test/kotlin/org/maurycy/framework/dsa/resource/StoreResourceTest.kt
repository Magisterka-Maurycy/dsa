package org.maurycy.framework.dsa.resource

import io.quarkus.test.common.http.TestHTTPEndpoint
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.security.TestSecurity
import io.restassured.RestAssured
import java.io.File
import jakarta.ws.rs.core.MediaType
import org.hamcrest.CoreMatchers
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@QuarkusTest
@TestHTTPEndpoint(StoreResource::class)
class StoreResourceTest {


    @Test
    @TestSecurity(user = "testUser", roles = ["admin","user"])
    fun uploadFileFailedWhenEmpty() {
        RestAssured.given()
            .`when`().post()
            .then()
            .statusCode(415)
            .body(CoreMatchers.`is`(""))
    }


    @ParameterizedTest
    @ValueSource(strings = ["test.txt", "test.doc", "test.docx", "test.pdf"])
    @TestSecurity(user = "testUser", roles = ["admin","user"])
    fun uploadFile(fileName: String) {
        val file = File(
            javaClass.classLoader.getResource(fileName)?.file ?: fail("File $fileName not found in resource directory")
        )
        RestAssured.given()
            .multiPart("files", file)
            .accept(MediaType.APPLICATION_JSON)
            .`when`().post()
            .then()
            .statusCode(201)
            .header(
                "Location",
                CoreMatchers.containsString("http://localhost:")
            )
            .header(
                "Location",
                CoreMatchers.containsString("/store/$fileName")
            )
            .body(CoreMatchers.`is`(""))
    }

    @ParameterizedTest
    @ValueSource(strings = ["test.txt", "test.doc", "test.docx", "test.pdf"])
    @TestSecurity(user = "testUser", roles = ["admin","user"])
    fun downloadFile(fileName: String) {
        val file = File(
            javaClass.classLoader.getResource(fileName)?.file ?: fail("File $fileName not found in resource directory")
        )
        RestAssured.given()
            .multiPart("files", file)
            .accept(MediaType.APPLICATION_JSON)
            .`when`().post()

        RestAssured.given()
            .accept("*/*")
            .get(fileName)
            .then()
            .statusCode(200)
            .header("Content-Disposition", "attachment;filename=$fileName")

    }

    @Test
    @TestSecurity(user = "testUser", roles = ["admin","user"])
    fun getAll() {
        RestAssured.given()
            .`when`().get()
            .then()
            .statusCode(200)
            .body(
                CoreMatchers.containsString("["),
                CoreMatchers.containsString("]")
            )
    }

    @Test
    @TestSecurity(user = "testUser", roles = ["admin","user"])
    fun searchForFile() {
        //TODO: TEST searching capabilities and indexes based on current knowledge indexes created for non .txt files are created in a wrong manner
    }
}