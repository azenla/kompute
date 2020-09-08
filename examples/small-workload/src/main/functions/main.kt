import org.kompute.runtime.serve
import org.kompute.runtime.workload

fun main() = serve(workload(
  Ack,
  Fib,
  Pi,
  PiWorker
))
