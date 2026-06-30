package com.example.service

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

// --- Gemini Request/Response Models ---

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: ContentResponse?
)

@JsonClass(generateAdapter = true)
data class ContentResponse(
    @Json(name = "parts") val parts: List<PartResponse>?
)

@JsonClass(generateAdapter = true)
data class PartResponse(
    @Json(name = "text") val text: String?
)

@JsonClass(generateAdapter = true)
data class AnalysisResult(
    @Json(name = "sentiment") val sentiment: String,
    @Json(name = "summary") val summary: String,
    @Json(name = "advice") val advice: String
)

interface GeminiApi {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiService {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val api: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApi::class.java)
    }

    suspend fun analyzeEntry(content: String): AnalysisResult {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return AnalysisResult(
                sentiment = "Neutral",
                summary = "Análisis no disponible (Falta configurar API Key de Gemini)",
                advice = "Escribe tus pensamientos libremente. Para recibir consejos de la IA, configura tu API Key en la barra lateral de Google AI Studio."
            )
        }

        val prompt = "Analiza el siguiente texto de diario y responde en JSON:\n\"\"\"\n$content\n\"\"\""

        val systemPrompt = """
            Eres un psicólogo y consejero motivacional experto. Analiza el diario del usuario y responde estrictamente con un objeto JSON que contenga exactamente estas claves:
            - 'sentiment': una de estas palabras en español (Excelente, Bueno, Neutral, Triste, Estresado)
            - 'summary': un breve resumen de 1 oración en español sobre cómo se siente el usuario.
            - 'advice': un consejo empático, motivador y reflexivo en español de 2 a 3 oraciones adaptado a sus sentimientos para ayudarle o mantener su actitud positiva.
            
            IMPORTANTE: Tu respuesta debe ser EXCLUSIVAMENTE el objeto JSON válido, sin bloques de código markdown como ```json, sin saltos de línea extraños y sin texto introductorio.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        val modelsToTry = listOf(
            "gemini-3.5-flash",
            "gemini-2.5-flash",
            "gemini-1.5-flash",
            "gemini-flash-latest"
        )

        var lastException: Exception? = null

        for (model in modelsToTry) {
            try {
                val response = api.generateContent(model, apiKey, request)
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: throw Exception("No response text from model $model")

                // Clean markdown blocks if Gemini still adds them
                val cleanedJson = responseText
                    .replace("```json", "")
                    .replace("```", "")
                    .trim()

                val adapter = moshi.adapter(AnalysisResult::class.java)
                val result = adapter.fromJson(cleanedJson) ?: throw Exception("Failed to parse analysis JSON from model $model")
                
                // Return immediately if successful
                return result
            } catch (e: Exception) {
                e.printStackTrace()
                lastException = e
            }
        }

        // If all models failed, fall back to offline simulation but append the errors
        val words = content.lowercase()
        val sentiment = when {
            words.contains("feliz") || words.contains("alegre") || words.contains("excelente") -> "Excelente"
            words.contains("bien") || words.contains("gusto") || words.contains("lindo") -> "Bueno"
            words.contains("triste") || words.contains("llor") || words.contains("mal") -> "Triste"
            words.contains("estres") || words.contains("ansio") || words.contains("nervio") -> "Estresado"
            else -> "Neutral"
        }
        val advice = when (sentiment) {
            "Excelente" -> "¡Me alegro muchísimo! Aprovecha esta maravillosa energía para realizar tus actividades preferidas y comparte tu felicidad con quienes te rodean."
            "Bueno" -> "Un gran día. Mantener una actitud positiva atrae mejores oportunidades. Sigue cultivando momentos agradables."
            "Triste" -> "Lamento que te sientas así hoy. Recuerda que está bien estar triste. Tómate el tiempo para descansar, respirar profundo y hablar con un ser querido."
            "Estresado" -> "El estrés puede ser abrumador. Trata de desconectarte unos minutos, realiza ejercicios de respiración y recuerda avanzar un paso a la vez."
            else -> "Cada día es una nueva oportunidad. Tómate un momento para agradecer algo pequeño de hoy y prepárate para un gran mañana."
        }
        
        val errorMsg = lastException?.message ?: lastException?.toString() ?: "Error desconocido"
        return AnalysisResult(
            sentiment = sentiment,
            summary = "Error de IA (Todos los modelos fallaron): $errorMsg",
            advice = advice
        )
    }
}
