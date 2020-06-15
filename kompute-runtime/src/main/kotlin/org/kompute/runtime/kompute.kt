package org.kompute.runtime

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
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

abstract class KomputeModule<I, O> {
  open val globalModuleName: String = javaClass.kotlin.simpleName!!

  abstract val requestSerializer: KSerializer<I>
  abstract val responseSerializer: KSerializer<O>

  abstract suspend fun execute(context: KomputeRequest<I>): O

  fun applyModuleRoute(komputeContext: KomputeContext, routing: Routing) {
    val json = Json(JsonConfiguration.Stable)
    routing.post("/$globalModuleName") {
      val content = context.receiveText()
      val request = json.parse(requestSerializer, content)

      val response: O?

      try {
        response = execute(KomputeRequest(request, komputeContext))
      } catch (e: Exception) {
        e.printStackTrace()
        context.respond(HttpStatusCode.InternalServerError)
        return@post
      }

      context.respond(json.stringify(responseSerializer, response))
    }
  }

  suspend fun call(context: KomputeContext, request: I): O {
    val url = "http://${context.komputeServiceHost}:8080/$globalModuleName"
    val response = context.httpClient.post<HttpResponse>(url) {
      body = context.json.stringify(requestSerializer, request)
    }

    if (response.status != HttpStatusCode.OK) {
      throw RuntimeException("Failed to call $globalModuleName.")
    }

    val responseString = response.receive<String>()
    return context.json.parse(responseSerializer, responseString)
  }
}

class KomputeRequest<R>(
  val request: R,
  val kompute: KomputeContext
)

class KomputeContext {
  val httpClient = HttpClient(Apache)
  val json = Json(JsonConfiguration.Stable)
  var applicationEngine: ApplicationEngine? = null

  val komputeServiceHost: String
    get() {
      var host = System.getenv("KOMPUTE_SERVICE_HOST")
      if (host == null) {
        host = "kompute.svc.cluster.local"
      }
      return host
    }
}

class KomputeWorkload(
  val modules: List<KomputeModule<*, *>>
) {
  val context: KomputeContext = KomputeContext()

  fun server(host: String = "127.0.0.1", port: Int = 8080): ApplicationEngine {
    val engine = embeddedServer(Netty, applicationEngineEnvironment {
      module {
        configure()
      }

      connector {
        this.host = host
        this.port = port
      }
    })
    context.applicationEngine = engine
    return engine
  }

  private fun Application.configure() {
    routing {
      for (module in modules) {
        module.applyModuleRoute(context, this)
      }
    }
  }
}

fun workload(vararg modules: KomputeModule<*, *>): KomputeWorkload =
  KomputeWorkload(modules.toList())

fun serve(workload: KomputeWorkload) {
  workload.server().start(true)
}
