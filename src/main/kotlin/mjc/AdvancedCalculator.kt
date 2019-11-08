package mjc

import kotlinx.coroutines.*
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.lens.Query
import org.http4k.lens.boolean
import org.http4k.lens.int
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.http4k.serverless.AppLoader
import java.time.Instant
import java.util.*

class AdvancedCalculator() {
    suspend fun doAsyncCalc(isCached: Boolean, x: Int, y: Int, z: Int): Int = coroutineScope {
        val sum: Int = x + y + z

        val calc: Deferred<Int> = async(CoroutineName("calcCoroutine")) { doCalc(sum = sum) }

        val cached: Deferred<Pair<String, Int>> = async(CoroutineName("queryCoroutine")) { queryCache(hash = "$x-$y-$z", success = isCached, sum = sum) }

        while (calc.isActive && cached.isActive) {
            yield()
        }

        when (true) {
            calc.isCompleted -> {
                GlobalScope.launch(CoroutineName("saveCacheCoroutine")) { saveToCache(hash = cached.await().first, value = calc.getCompleted()) }
                println("returning 0")
                calc.getCompleted()
            }

            cached.isCompleted -> {
                if (cached.getCompleted().second == -1) {
                    val result: Int = calc.await()
                    GlobalScope.launch(CoroutineName("saveCacheCoroutine")) { saveToCache(hash = cached.getCompleted().first, value = result) }
                    println("returning 1")
                    result
                } else {
                    calc.cancel()
                    println("returning 2")
                    cached.getCompleted().second
                }
            }

            else -> {
                println("returning fail")
                -1
            }
        }
    }
    suspend fun doSyncCalc(isCached: Boolean, x: Int, y: Int, z: Int): Int = coroutineScope {
        val sum: Int = x + y + z

        val cached: Pair<String, Int> = async(CoroutineName("queryCoroutine")) { queryCache(hash = "$x-$y-$z", success = isCached, sum = sum) }.await()

        if (cached.second !== -1) {
            println("returning 1")
            cached.second
        } else {
            val calc: Int = async(CoroutineName("calcCoroutine")) { doCalc(sum = sum) }.await()

            launch(CoroutineName("saveCacheCoroutine")) { saveToCache(hash = cached.first, value = calc) }.join()

            println("returning 2")
            calc
        }
    }

    suspend fun doCalc(sum: Int, time: Long = 5000): Int {
        println("start doCalc -- thread: ${Thread.currentThread().name}")
        delay(timeMillis = time)
        println("end doCalc: $sum")
        return sum
    }

    suspend fun queryCache(hash: String, time: Long = 1000, success: Boolean, sum: Int): Pair<String, Int> {
        println("start queryCache -- thread: ${Thread.currentThread().name}")
        delay(timeMillis = time)
        println("end queryCache: $hash")
        return if (success) Pair(hash, sum) else Pair(hash, -1)
    }

    suspend fun saveToCache(hash: String, value: Int, time: Long = 3000) {
        println("start saveToCache -- thread: ${Thread.currentThread().name}")
        delay(timeMillis = time)
        println("end saveToCache: $hash: $value")
    }
}


class Authorizer {
    suspend fun authorize(isAuthorized: Boolean, time: Long = 2000): Boolean {
        println("start authorize -- thread: ${Thread.currentThread().name}")
        delay(timeMillis = time)
        println("end authorize: $isAuthorized")
        return isAuthorized
    }
}

class Routes : AppLoader {
    override fun invoke(env: Map<String, String>): RoutingHttpHandler =
        routes(
            "/sum" bind Method.GET to getRateAsync(),
            "/sumSync" bind Method.GET to getRateSync()
        )

    fun getRateAsync(): HttpHandler = { req ->
        val start: Long = Instant.now().toEpochMilli()

        val isCached: Boolean = Query.boolean().optional(name = "isCached").extract(target = req) ?: false
        val isAuthorized: Boolean = Query.boolean().optional(name = "isAuthorized").extract(target = req) ?: true
        val x: Int = Query.int().optional(name = "x").extract(target = req) ?: 0
        val y: Int = Query.int().optional(name = "y").extract(target = req) ?: 0
        val z: Int = Query.int().optional(name = "z").extract(target = req) ?: 0
        val response: Response = runBlocking {
            coroutineScope {
                val authorizer: Authorizer = Authorizer()
                val authorized: Deferred<Boolean> = async(CoroutineName("authCoroutine")) { authorizer.authorize(isAuthorized = isAuthorized) }

                val calculator: AdvancedCalculator = AdvancedCalculator()

                val result: Deferred<Int> = async(CoroutineName("beginAsyncCalcCoroutine")) { calculator.doAsyncCalc(isCached = isCached, x = x, y = y, z = z) }

                if (!authorized.await()) {
                    result.cancel()
                    Response(UNAUTHORIZED)
                } else {
                    Response(OK).body("${result.await()}")
                }
            }
        }
        val end: Long = Instant.now().toEpochMilli()
        println("status: ${response.status} \nvalue: ${response.bodyString()} \nduration: ${end - start}")

        response
    }
    fun getRateSync(): HttpHandler = { req ->
        val start: Long = Instant.now().toEpochMilli()

        val isCached: Boolean = Query.boolean().optional(name = "isCached").extract(target = req) ?: false
        val isAuthorized: Boolean = Query.boolean().optional(name = "isAuthorized").extract(target = req) ?: true
        val x: Int = Query.int().optional(name = "x").extract(target = req) ?: 0
        val y: Int = Query.int().optional(name = "y").extract(target = req) ?: 0
        val z: Int = Query.int().optional(name = "z").extract(target = req) ?: 0

        val response: Response = runBlocking {
            coroutineScope {
                val authorizer: Authorizer = Authorizer()
                val authorized: Boolean = async(CoroutineName("authCoroutine")) { authorizer.authorize(isAuthorized = isAuthorized) }.await()

                val calculator: AdvancedCalculator = AdvancedCalculator()

                if (!authorized) {
                    Response(UNAUTHORIZED)
                } else {
                    val result: Int = calculator.doSyncCalc(isCached = isCached, x = x, y = y, z = z)
                    Response(OK).body("${result}")
                }
            }
        }
        val end: Long = Instant.now().toEpochMilli()
        println("status: ${response.status} \nvalue: ${response.bodyString()} \nduration: ${end - start}")

        response
    }
}

/*************************************************************************
 *
 *          BOILER PLATE FOR RUNNING LOCALLY WITH HTTP4K
 *
 *************************************************************************/
fun main() {
    runLocally()
}

fun runLocally() {
    routes(
        Routes()(Collections.emptyMap())
    ).asServer(config = SunHttp(port = 8010)).start()

    println("Running locally on port 8010")
}
