import kotlinx.serialization.Serializable

@Serializable
data class AckRequest(
  val m: Int,
  val n: Int
)

@Serializable
data class AckResponse(
  val result: Int
)
