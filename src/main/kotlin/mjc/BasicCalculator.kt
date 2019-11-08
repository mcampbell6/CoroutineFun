package mjc
import kotlinx.coroutines.*
import java.time.Instant
fun main() {
    runBlocking {
        BasicCalculator().calculate(x = 30, y = 20, z = 10)
    }
}

class BasicCalculator {
    suspend fun calculate(x: Int, y: Int, z: Int): Int {
        val start: Long = Instant.now().toEpochMilli()
        val value: Int = coroutineScope {
            val slow: Deferred<Int> = async { calc(x = x, time = 3000) }
            val med: Deferred<Int> = async { calc( x = y, time = 1000) }
            val fast: Deferred<Int> = async { calc(x = z, time = 200) }
            slow.await() + med.await() + fast.await()
        }
        val end: Long = Instant.now().toEpochMilli()
        println("value: $value \nduration: ${end - start}")
        return value
    }

    suspend fun calc(x: Int, time: Long): Int {
        delay(timeMillis = time)
        println("calc: $x -- thread: ${Thread.currentThread().name}")
        return x
    }
}

