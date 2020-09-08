import kotlinx.coroutines.*
import kotlinx.serialization.KSerializer
import org.kompute.runtime.KomputeContext
import org.kompute.runtime.KomputeModule
import org.kompute.runtime.KomputeRequest
import java.util.concurrent.ConcurrentHashMap

object Fib : KomputeModule<FibRequest, FibResponse>() {
  override val globalModuleName = "fib"

  override val requestSerializer: KSerializer<FibRequest>
    get() = FibRequest.serializer()

  override val responseSerializer: KSerializer<FibResponse>
    get() = FibResponse.serializer()

  private val cache = ConcurrentHashMap<Int, Int>().apply {
    put(0, 0)
    put(1, 1)
  }

  override suspend fun execute(context: KomputeRequest<FibRequest>): FibResponse {
    val n = context.request.n
    return FibResponse(cache[n] ?: calculate(context.kompute, n))
  }

  private suspend fun calculate(context: KomputeContext, n: Int): Int {
    if (n == 0 || n == 1) {
      return n
    }

    logger.info("Cache: ${cache.size} entries")

    return coroutineScope {
      val results = awaitAll(
        async {
          call(context, FibRequest(
            n - 1
          )).result
        },
        async {
          call(context, FibRequest(
            n - 2
          )).result
        }
      )

      val result = results[0] + results[1]
      cache[n] = result
      result
    }
  }
}
