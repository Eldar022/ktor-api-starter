package com.example

import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContains

class ApplicationTest {
    @AfterTest
    fun cleanup() = TaskStore.clear()

    @Test
    fun healthEndpointReturnsOk() = testApplication {
        application { module() }
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertContains(response.bodyAsText(), """"status": "ok"""")
    }

    @Test
    fun createAndFetchTask() = testApplication {
        application { module() }
        val createResponse = client.post("/tasks") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Write tests"}""")
        }
        assertEquals(HttpStatusCode.Created, createResponse.status)
        assertContains(createResponse.bodyAsText(), "Write tests")

        val listResponse = client.get("/tasks")
        assertEquals(HttpStatusCode.OK, listResponse.status)
        assertContains(listResponse.bodyAsText(), "Write tests")
    }

    @Test
    fun blankTitleReturnsBadRequest() = testApplication {
        application { module() }
        val response = client.post("/tasks") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"   "}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertContains(response.bodyAsText(), "bad_request")
    }

    @Test
    fun missingTaskReturnsNotFound() = testApplication {
        application { module() }
        val response = client.get("/tasks/999999")
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertContains(response.bodyAsText(), "task_not_found")
    }

    @Test
    fun deleteTaskReturnsNoContent() = testApplication {
        application { module() }
        val created = client.post("/tasks") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Temporary"}""")
        }
        val id = Regex(""""id"\s*:\s*(\d+)""").find(created.bodyAsText())!!.groupValues[1]
        val response = client.delete("/tasks/$id")
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun swaggerAndOpenApiAreAvailable() = testApplication {
        application { module() }
        assertEquals(HttpStatusCode.OK, client.get("/swagger").status)
        val spec = client.get("/openapi.yaml")
        assertEquals(HttpStatusCode.OK, spec.status)
        assertContains(spec.bodyAsText(), "/tasks")
    }
}
