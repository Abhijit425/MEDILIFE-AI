package com.medilife.ai.engine

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import java.io.File

/**
 * Optional on-device language model via MediaPipe LLM Inference. The user supplies a
 * compatible model file (e.g. a Gemma .task/.bin). Nothing is downloaded and nothing
 * leaves the device. When no model is present, MediLife uses its rule-based engine and
 * says so — it never pretends a model is running.
 */
class LocalLlm(private val context: Context) {
    @Volatile private var engine: LlmInference? = null
    @Volatile private var loadedPath: String? = null
    var lastError: String? = null
        private set

    fun modelDir(): File = File(context.filesDir, "models").apply { mkdirs() }

    fun isAvailable(path: String): Boolean = path.isNotBlank() && File(path).let { it.exists() && it.length() > 1_000_000 }

    @Synchronized
    fun ensureLoaded(path: String): Boolean {
        if (!isAvailable(path)) return false
        if (engine != null && loadedPath == path) return true
        return try {
            engine?.close()
            val opts = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(path)
                .setMaxTokens(768)
                .build()
            engine = LlmInference.createFromOptions(context, opts)
            loadedPath = path
            lastError = null
            true
        } catch (t: Throwable) {
            engine = null; loadedPath = null
            lastError = t.message ?: t.javaClass.simpleName
            false
        }
    }

    @Synchronized
    fun generate(prompt: String): String? = try {
        engine?.generateResponse(prompt)?.trim()?.takeIf { it.isNotBlank() }
    } catch (t: Throwable) {
        lastError = t.message; null
    }

    @Synchronized
    fun unload() { runCatching { engine?.close() }; engine = null; loadedPath = null }
}
