package com.example.swiftconvert.conversion

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reflection-based FFmpeg transcoder.
 * Compiles even if FFmpegKit is not available on the classpath (no direct imports).
 * At runtime:
 *  - If FFmpegKit exists, calls FFmpegKit.execute(command).
 *  - If not, returns an informative error.
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
     * Execute a full ffmpeg command string, e.g.:
     *  " -y -i /path/in.m4b -c:a aac -b:a 128k /path/out.m4a"
     */
    suspend fun execute(command: String): Result = withContext(Dispatchers.IO) {
        try {
            val ffmpegKitCls = Class.forName("com.arthenica.ffmpegkit.FFmpegKit")
            val executeMethod = ffmpegKitCls.getMethod("execute", String::class.java)
            val session = executeMethod.invoke(null, command)
                ?: return@withContext Result(false, error = "FFmpegKit session is null")

            val sessionCls = session.javaClass
            val getReturnCode = sessionCls.getMethod("getReturnCode")
            val returnCodeObj = getReturnCode.invoke(session)
            val returnCodeLabel = returnCodeObj?.toString()

            // Try read logs (best-effort)
            val stdout = extractLogs(session, wantError = false)
            val stderr = extractLogs(session, wantError = true)

            // Success heuristic without ReturnCode.isSuccess()
            val success = returnCodeLabel?.contains("SUCCESS", ignoreCase = true) == true

            Result(success, returnCodeLabel, stdout, stderr, null)
        } catch (_: ClassNotFoundException) {
            Result(
                success = false,
                error = "FFmpegKit not on classpath. Add app/libs/ffmpeg-kit.aar or set FFMPEGKIT_AAR_URL in CI."
            )
        } catch (t: Throwable) {
            Result(success = false, error = "FFmpeg error: ${t.message}")
        }
    }

    private fun extractLogs(session: Any, wantError: Boolean): String? = try {
        val sessionCls = session.javaClass
        val getAllLogs = sessionCls.methods.firstOrNull { it.name == "getAllLogs" && it.parameterCount == 0 }
        val rawLogs = getAllLogs?.invoke(session) as? List<*>
        if (rawLogs != null) {
            val msgs = rawLogs.mapNotNull { log ->
                val logCls = log?.javaClass ?: return@mapNotNull null
                val level = try {
                    logCls.methods.firstOrNull { it.name == "getLevel" }?.invoke(log)?.toString()
                } catch (_: Throwable) { null }
                val isErr = level?.contains("ERROR", ignoreCase = true) == true
                val msg = logCls.methods.firstOrNull { it.name == "getMessage" }?.invoke(log)?.toString()
                if (msg.isNullOrBlank()) null else if (wantError == isErr) msg else null
            }
            if (msgs.isNotEmpty()) msgs.joinToString("\n") else null
        } else null
    } catch (_: Throwable) { null }
}
