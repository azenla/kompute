package org.kompute.runtime

import io.github.resilience4j.kotlin.retry.executeSuspendFunction
import io.github.resilience4j.retry.Retry
import io.ktor.application.Application
import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.coroutines.coroutineContext

abstract class KomputeModule<I, O> {
  open val globalModuleName: String = javaClass.kotlin.simpleName!!
  open val logger: Logger = LoggerFactory.getLogger(javaClass)

  abstract val requestSerializer: KSerializer<I>
  abstract val responseSerializer: KSerializer<O>

  private val retry: Retry by lazy {
    Retry.ofDefaults("kompute-${globalModuleName}")
  }

  abstract suspend fun execute(context: KomputeRequest<I>): O

  fun applyModuleRoute(komputeContext: KomputeContext, routing: Routing) {
    routing.post("/$globalModuleName") {
      val content = context.receiveText()
      val request = komputeContext.json.decodeFromString(requestSerializer, content)

      if (komputeContext.isDevelopmentMode) {
        val requestSerialized = komputeContext.json.encodeToString(requestSerializer, request)
        logger.info("Call Start $globalModuleName($requestSerialized)")
      }

      val response: O?

      try {
        response = execute(KomputeRequest(request, komputeContext))
      } catch (e: Exception) {
        val requestSerialized = komputeContext.json.encodeToString(requestSerializer, request)
        logger.warn("Call failed for module $globalModuleName: $requestSerialized", e)
        if (komputeContext.isDevelopmentMode) {
          context.respond(HttpStatusCode.InternalServerError, e.stackTraceToString())
        }
        return@post
      }

      val responseSerialized = komputeContext.json.encodeToString(responseSerializer, response)
      if (komputeContext.isDevelopmentMode) {
        val requestSerialized = komputeContext.json.encodeToString(requestSerializer, request)
        logger.info("Call End $globalModuleName($requestSerialized) = $responseSerialized")
      }
      context.respond(responseSerialized)
    }
  }

  suspend fun call(context: KomputeContext, request: I): O {
    return retry.executeSuspendFunction {
      callDirect(context, request)
    }
  }

  suspend fun callDirect(context: KomputeContext, request: I): O {
    val url = "http://${context.komputeServiceHost}:${context.komputeServicePort}/$globalModuleName"
    val response = context.httpClient.post<HttpResponse>(url) {
      body = context.json.encodeToString(requestSerializer, request)
    }

    if (response.status != HttpStatusCode.OK) {
      throw RuntimeException(
        "Failed to call $globalModuleName: Status Code = ${response.status.value}" +
          " (${response.status.description})")
    }

    val responseString = response.receive<String>()
    return context.json.decodeFromString(responseSerializer, responseString)
  }
}

class KomputeRequest<R>(
  val request: R,
  val kompute: KomputeContext
)

class KomputeContext {
  val logger = LoggerFactory.getLogger(KomputeContext::class.java)
  val json = Json {}
  var applicationEngine: ApplicationEngine? = null
  var httpClient = HttpClient(Apache) {
    engine {
      connectTimeout = 0
      connectionRequestTimeout = 0
      socketTimeout = 0
      @Suppress("EXPERIMENTAL_API_USAGE")
      threadsCount = 8
    }
  }

  val komputeServiceHost: String
    get() {
      var host = System.getenv("KOMPUTE_SERVICE_HOST")
      if (host == null) {
        host = "kompute.svc.cluster.local"
      }
      return host
    }

  val komputeServicePort: Int
    get() {
      var port = System.getenv("KOMPUTE_SERVICE_PORT")
      if (port == null) {
        port = "8080"
      }
      return port.toInt()
    }

  val isDevelopmentMode: Boolean by lazy {
    val devModeString = System.getenv().getOrDefault("DEV_MODE", "0")
    listOf(
      "1",
      "on",
      "yes",
      "true",
      "enable",
      "enabled"
    ).contains(devModeString.toLowerCase().trim())
  }
}

class KomputeWorkload(
  val modules: List<KomputeModule<*, *>>,
  contextConfigure: KomputeContext.() -> Unit = {}
) {
  val context: KomputeContext = KomputeContext()

  init {
    contextConfigure(context)
  }

  fun server(host: String = "0.0.0.0", port: Int = 8080): ApplicationEngine {
    val engine = embeddedServer(Netty, applicationEngineEnvironment {
      module {
        configure()
      }

      connector {
        this.host = host
        this.port = port
      }
    }) {
      requestQueueLimit = 512
      runningLimit = 30
      requestReadTimeoutSeconds = 0
      responseWriteTimeoutSeconds = 0
    }
    context.applicationEngine = engine
    return engine
  }

  private fun Application.configure() {
    routing {
      get("/healthz") {
        context.respond(HttpStatusCode.OK, "Success.")
      }

      for (module in modules) {
        module.applyModuleRoute(context, this)
      }
    }
  }
}

fun workload(
  vararg modules: KomputeModule<*, *>,
  contextConfigure: KomputeContext.() -> Unit = {}
): KomputeWorkload =
  KomputeWorkload(modules.toList(), contextConfigure = contextConfigure)

fun serve(workload: KomputeWorkload) {
  workload.modules.forEach { module ->
    workload.context.logger.info("Workload Module: ${module.globalModuleName}")
  }
  val server = workload.server()
  server.start(true)
}
