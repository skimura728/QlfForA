package com.example.qlffora

import com.example.qlffora.util.logDebug
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive


typealias CategoryNewsMap = Map<String, List<NewsArticle>>

@Serializable
data class NewsArticle(
    val title: String,
    val thumbnail: String? = null,
    val published: String,
    val category: String,
    val link: String
)

class NewsArticleModel {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    suspend fun loadAllNews(): CategoryNewsMap? {
        val categories: List<String> = client
            .get("https://quicklearnfeed.onrender.com/api/categories")
            .body()
        logDebug("Get News Categories: $categories")

        return coroutineScope {
            val deferredMap = categories.associateWith { category ->
                async {
                    try {
                        getNewsByCategory(category)
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            }
            val resultMap = deferredMap.mapValues { it.value.await() }
            logDebug ("Get News: $resultMap")
            resultMap
        }
    }

    suspend fun getNewsByCategory(category: String): List<NewsArticle> {
        return try {
            val response: HttpResponse = client
                .get("https://quicklearnfeed.onrender.com/api/news/$category")
            logDebug("Status for $category: ${response.status}")

            if (response.status == HttpStatusCode.OK) {
                val rawText = response.bodyAsText()
                logDebug("RAW JSON for $category: $rawText")

                val articles: List<NewsArticle> = Json.decodeFromString(
                    ListSerializer(NewsArticle.serializer()),
                    rawText)
                logDebug("Fetched ${articles.size} articles for category: $category")
                articles
            } else {
                logDebug("Non-OK status for $category: ${response.status}")
                emptyList()
            }
        } catch (e: Exception) {
            logDebug("Error fetching news for category $category: ${e.localizedMessage}")
            emptyList()
        }
    }

    suspend fun getSummary(link: String): String {
        val encodedUrl = java.net.URLEncoder.encode(link, "UTF-8")
        val response = client.get("https://quicklearnfeed.onrender.com/api/scrape?url=$encodedUrl")
        if (response.status == HttpStatusCode.OK) {
            val json = Json.parseToJsonElement(response.bodyAsText())
            return json.jsonObject["summary"]?.jsonPrimitive?.content ?: "No summary available."
        } else {
            throw Exception("HTTP ${response.status}")
        }
    }
}