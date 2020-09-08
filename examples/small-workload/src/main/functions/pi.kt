import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.KSerializer
import org.kompute.runtime.KomputeModule
import org.kompute.runtime.KomputeRequest
import kotlin.math.pow

object Pi: KomputeModule<PiRequest, PiResponse>() {
  override val globalModuleName = "pi"

  override val requestSerializer: KSerializer<PiRequest>
    get() = PiRequest.serializer()

  override val responseSerializer: KSerializer<PiResponse>
    get() = PiResponse.serializer()

  override suspend fun execute(context: KomputeRequest<PiRequest>): PiResponse {
    return coroutineScope {
      var n = 0
      val calls = (0..context.request.workers).map { worker ->
        val deferred = async {
          PiWorker.call(context.kompute, PiWorkerRequest(
            n,
            context.request.iterationsPerWorker
          ))
        }

        n += context.request.iterationsPerWorker
        deferred
      }
      PiResponse(
        awaitAll(*calls.toTypedArray()).map {
          it.result
        }.sum()
      )
    }
  }
}

object PiWorker: KomputeModule<PiWorkerRequest, PiWorkerResponse>() {
  override val globalModuleName = "pi_worker"

  override val requestSerializer: KSerializer<PiWorkerRequest>
    get() = PiWorkerRequest.serializer()

  override val responseSerializer: KSerializer<PiWorkerResponse>
    get() = PiWorkerResponse.serializer()

  override suspend fun execute(context: KomputeRequest<PiWorkerRequest>): PiWorkerResponse {
    var result = 0.0
    repeat(context.request.count) {
      val x = context.request.start + it
      result += 4 * (-1.0).pow(x.toDouble()) / ((2 * x) + 1)
    }
    return PiWorkerResponse(
      result
    )
  }
}
