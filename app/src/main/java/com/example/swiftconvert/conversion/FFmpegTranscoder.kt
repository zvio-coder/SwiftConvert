package com.example.swiftconvert.conversion

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reflection-based FFmpeg transcoder.
 * - Compiles even if FFmpegKit AAR is not present (no direct imports).
 * - At runtime, if FFmpegKit classes exist, executes the command.
 * - If not present, returns a clear "FFmpeg unavailable" result.
 */
object FFmpegTranscoder {

    data class Result(
        val success: Boolean,
        val returnCodeLabel: String? = null,
        val stdout: String? = null,
        val stderr: String? = null,
        val error: String? = null
    )

    /**
     * Execute a full ffmpeg command line (e.g. " -i input.mp4 -c:v libx264 out.mp4").
     * This calls com.arthenica.ffmpegkit.FFmpegKit via reflection when available.
     */
    suspend fun execute(command: String): Result = withContext(Dispatchers.IO) {
        try {
            val ffmpegKitCls = Class.forName("com.arthenica.ffmpegkit.FFmpegKit")
            val executeMethod = ffmpegKitCls.getMethod("execute", String::class.java)

            // session = FFmpegKit.execute(command)
            val session = executeMethod.invoke(null, command)
                ?: return@withContext Result(false, error = "FFmpegKit session was null")

            // Pull return code label without depending on ReturnCode enum
            val sessionCls = session.javaClass
            val getReturnCode = sessionCls.getMethod("getReturnCode")
            val returnCodeObj = getReturnCode.invoke(session)
            val returnCodeLabel = returnCodeObj?.toString()

            // Gather logs (best-effort)
            val stdout = extractLogs(session, wantError = false)
            val stderr = extractLogs(session, wantError = true)

            // Heuristic success check without ReturnCode.isSuccess()
            val success = returnCodeLabel?.contains("SUCCESS", ignoreCase = true) == true

            Result(
                success = success,
                returnCodeLabel = returnCodeLabel,
                stdout = stdout,
                stderr = stderr
            )
        } catch (cnfe: ClassNotFoundException) {
            Result(
                success = false,
                error = "FFmpegKit not on classpath. Add app/libs/ffmpeg-kit.aar or set FFMPEGKIT_AAR_URL in CI."
            )
        } catch (t: Throwable) {
            Result(
                success = false,
                error = "FFmpeg execution failed: ${t.message}"
            )
        }
    }

    /**
     * Try to fetch logs from the session without having FFmpegKit types at compile time.
     */
    private fun extractLogs(session: Any, wantError: Boolean): String? = try {
        val sessionCls = session.javaClass

        // Try getAllLogs(): List<Log> then Log.getMessage()
        val getAllLogs = sessionCls.methods.firstOrNull { it.name == "getAllLogs" && it.parameterCount == 0 }
        val rawLogs = getAllLogs?.invoke(session) as? List<*>
        if (rawLogs != null) {
            val messages = rawLogs.mapNotNull { log ->
                val logCls = log?.javaClass ?: return@mapNotNull null
                val isError = try {
                    val level = logCls.methods.firstOrNull { it.name == "getLevel" }?.invoke(log)
                    level?.toString()?.contains("ERROR", ignoreCase = true) ?: false
                } catch (_: Throwable) { false }
                val msg = logCls.methods.firstOrNull { it.name == "getMessage" }?.invoke(log)?.toString()
                if (wantError == isError) msg else null
            }.filterNot { it.isNullOrBlank() }
            if (messages.isNotEmpty()) messages.joinToString("\n") else null
        } else {
            null
        }
    } catch (_: Throwable) {
        null
    }
}
