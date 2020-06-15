import kotlinx.serialization.KSerializer
import org.kompute.runtime.KomputeModule
import org.kompute.runtime.KomputeRequest

object Ack : KomputeModule<AckRequest, AckResponse>() {
  override val globalModuleName = "ack"

  override val requestSerializer: KSerializer<AckRequest>
    get() = AckRequest.serializer()

  override val responseSerializer: KSerializer<AckResponse>
    get() = AckResponse.serializer()

  override suspend fun execute(context: KomputeRequest<AckRequest>): AckResponse {
    val request = context.request

    if (request.m == 0) {
      return AckResponse(request.n + 1)
    }

    if (request.m > 0 && request.n == 0) {
      return AckResponse(call(context.kompute, AckRequest(request.m - 1, 1)).result)
    }

    return AckResponse(call(
      context.kompute,
      AckRequest(
        m = request.m - 1,
        n = call(context.kompute, AckRequest(
          m = request.m,
          n = request.n - 1
        )).result
      )).result)
  }
}
