/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.tests

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.tests.utils.*
import io.ktor.http.*
import kotlin.test.*

class HeadersTest : ClientLoader() {

    /**
     * URLConnection can't set the Content-Length header for get requests.
     */
    @Test
    fun testHeadersReturnNullWhenMissing() = clientTests(listOf("Android")) {
        test { client ->
            client.get<HttpResponse>("$TEST_SERVER/headers").let {
                assertEquals(HttpStatusCode.OK, it.status)
                assertEquals("0", it.readText())

                assertNull(it.headers["X-Nonexistent-Header"])
                assertNull(it.headers.getAll("X-Nonexistent-Header"))
            }
        }
    }

    @Test
    fun testHeadersMerge() = clientTests(listOf("Js")) {
        test { client ->
            client.get<HttpResponse>("$TEST_SERVER/headers-merge") {
                accept(ContentType.Text.Html)
                accept(ContentType.Application.Json)
            }.let {
                assertEquals(HttpStatusCode.OK, it.status)
                assertEquals("JSON", it.readText())
                assertEquals("application/json; charset=UTF-8", it.headers[HttpHeaders.ContentType])
            }

            client.get<HttpResponse>("$TEST_SERVER/headers-merge") {
                accept(ContentType.Text.Html)
                accept(ContentType.Application.Xml)
            }.let {
                assertEquals("XML", it.readText())
                assertEquals("application/xml; charset=UTF-8", it.headers[HttpHeaders.ContentType])
            }
        }
    }

    @Test
    fun testAcceptMerge() = clientTests(listOf("Js")) {
        test { client ->
            val lines = client.get<String>("$TCP_SERVER/headers-merge") {
                accept(ContentType.Application.Xml)
                accept(ContentType.Application.Json)
            }.split("\n")

            val acceptHeaderLine = lines.first { it.startsWith("Accept:") }
            assertEquals("Accept: application/xml,application/json", acceptHeaderLine)
        }
    }

    @Test
    fun testSingleHostHeader() = clientTests(listOf("Js", "Android", "Java")) {
        test { client ->
            client.get("$TEST_SERVER/headers/host") {
                header(HttpHeaders.Host, "CustomHost")
            }
        }
    }

    @Test
    fun testUnsafeHeaders() = clientTests {
        test { client ->
            var message = ""
            try {
                client.get<HttpResponse>("$TEST_SERVER/headers") {
                    header(HttpHeaders.ContentLength, 0)
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                    header(HttpHeaders.TransferEncoding, "chunked")
                    header(HttpHeaders.Upgrade, "upgrade")
                }
            } catch (cause: UnsafeHeaderException) {
                message = cause.message ?: ""
            }

            val expected =
                "Header(s) ${HttpHeaders.UnsafeHeadersList} are controlled by the engine and cannot be set explicitly"

            assertEquals(expected, message)
        }
    }

    @Test
    fun testRequestHasContentLength() = clientTests {
        test { client ->
            val first = client.put<String>("$TEST_SERVER/headers")
            assertEquals("0", first)

            val second = client.post<String>("$TEST_SERVER/headers")
            assertEquals("0", second)
        }
    }
}
