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

@Serializable
data class FibRequest(
  val n: Int
)

@Serializable
data class FibResponse(
  val result: Int
)

@Serializable
data class PiRequest(
  val workers: Int,
  val iterationsPerWorker: Int
)

@Serializable
data class PiResponse(
  val result: Double
)

@Serializable
data class PiWorkerRequest(
  val start: Int,
  val count: Int
)

@Serializable
data class PiWorkerResponse(
  val result: Double
)
