import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

fun main(args: Array<String>) {
    val port = args.find { it.startsWith("--port=") }
        ?.substringAfter("=")
        ?.toIntOrNull()
        ?: 8080

    with(HttpServer.create(InetSocketAddress(port), 0)) {
        executor = Executors.newCachedThreadPool()

        createContext("/run") { exchange ->
            with(exchange) {
                if (requestMethod != "GET") {
                    sendError(405, ErrorResponse("Method not allowed"))
                    return@createContext
                }
                val cmd = getQueryParam("cmd")
                if (cmd == null) {
                    sendError(400, ErrorResponse("Missing 'cmd' parameter"))
                    return@createContext
                }
                sendJson(executeSync(cmd).toJson())
            }
        }

        createContext("/start") { exchange ->
            with(exchange) {
                if (requestMethod != "POST") {
                    sendError(405, ErrorResponse("Method not allowed"))
                    return@createContext
                }
                val cmd = getQueryParam("cmd")
                if (cmd == null) {
                    sendError(400, ErrorResponse("Missing 'cmd' parameter"))
                    return@createContext
                }
                val pid = startAsync(cmd)
                sendText(pid.toString())
            }
        }

        createContext("/stop") { exchange ->
            with(exchange) {
                if (requestMethod != "POST") {
                    sendError(405, ErrorResponse("Method not allowed"))
                    return@createContext
                }
                val pidStr = getQueryParam("pid")
                if (pidStr == null) {
                    sendError(400, ErrorResponse("Missing 'pid' parameter"))
                    return@createContext
                }
                val pid = pidStr.toLongOrNull()
                if (pid == null) {
                    sendError(400, ErrorResponse("Invalid 'pid' parameter: $pidStr"))
                    return@createContext
                }
                val result = stopProcess(pid)
                if (result == null) {
                    sendError(404, ErrorResponse("Process not found: $pid"))
                    return@createContext
                }
                sendJson(result.toJson())
            }
        }

        start()
    }
    println("Shell server running on http://localhost:$port")
    println("Endpoints:")
    println("  GET  /run?cmd=<command>   - Run synchronously")
    println("  POST /start?cmd=<command> - Start async, returns PID")
    println("  POST /stop?pid=<pid>      - Stop async process by PID")
}

private data class RunningProcess(
    val process: Process,
    val stdout: ByteArrayOutputStream,
    val stderr: ByteArrayOutputStream,
)

private val runningProcesses = ConcurrentHashMap<Long, RunningProcess>()

private data class CommandResponse(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
) {
    fun toJson(): String = """{"exitCode":$exitCode,"stdout":${stdout.escapeJson()},"stderr":${stderr.escapeJson()}}"""
}

private data class ErrorResponse(
    val error: String,
) {
    fun toJson(): String = """{"error":${error.escapeJson()}}"""
}

private fun executeSync(cmd: String): CommandResponse {
    val process = ProcessBuilder("sh", "-c", cmd)
        .start()
    val stdout = process.inputStream.bufferedReader().readText()
    val stderr = process.errorStream.bufferedReader().readText()
    val exitCode = process.waitFor()
    return CommandResponse(exitCode, stdout, stderr)
}

private fun startAsync(cmd: String): Long {
    val stdout = ByteArrayOutputStream()
    val stderr = ByteArrayOutputStream()

    val process = ProcessBuilder("sh", "-c", cmd)
        .start()

    val pid = process.pid()

    Thread {
        process.inputStream.copyTo(stdout)
    }.start()
    Thread {
        process.errorStream.copyTo(stderr)
    }.start()

    runningProcesses[pid] = RunningProcess(process, stdout, stderr)
    return pid
}

private fun stopProcess(pid: Long): CommandResponse? {
    val running = runningProcesses.remove(pid) ?: return null
    running.process.destroy()
    val exitCode = running.process.waitFor()
    return CommandResponse(exitCode, running.stdout.toString(), running.stderr.toString())
}

private fun HttpExchange.getQueryParam(name: String): String? {
    val query = requestURI.query ?: return null
    return query.split("&")
        .map { it.split("=", limit = 2) }
        .find { it[0] == name }
        ?.getOrNull(1)
        ?.let { URLDecoder.decode(it, "UTF-8") }
}

private fun HttpExchange.sendText(text: String) {
    try {
        requestBody.readBytes()

        val bytes = text.toByteArray(Charsets.UTF_8)
        responseHeaders["Content-Type"] = "text/plain"
        sendResponseHeaders(200, bytes.size.toLong())
        responseBody.write(bytes)
    } finally {
        close()
    }
}

private fun HttpExchange.sendJson(json: String) {
    try {
        requestBody.readBytes()

        val bytes = json.toByteArray(Charsets.UTF_8)
        responseHeaders["Content-Type"] = "application/json"
        sendResponseHeaders(200, bytes.size.toLong())
        responseBody.write(bytes)
    } finally {
        close()
    }
}

private fun HttpExchange.sendError(code: Int, error: ErrorResponse) {
    try {
        requestBody.readBytes()

        val bytes = error.toJson().toByteArray(Charsets.UTF_8)
        responseHeaders["Content-Type"] = "application/json"
        sendResponseHeaders(code, bytes.size.toLong())
        responseBody.write(bytes)
    } finally {
        close()
    }
}

private fun String.escapeJson(): String {
    return "\"" + this
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t") + "\""
}
