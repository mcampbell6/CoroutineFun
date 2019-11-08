package mjc

import io.mockk.Called
import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.junit.Test
import kotlin.test.assertEquals

class AdvancedCalculatorTest {
    @Test
    fun `auth check should pass`() = runBlockingTest {
        val actual: Response = Routes().getRateAsync().invoke(Request(Method.GET, "/rate?isCached=true&x=300&y=200&z=100"))
        assertEquals(OK, actual.status)
    }

    @Test
    fun `doAsyncCalc should call the calculate and query cache coroutines`() = runBlocking {
        val calculator: AdvancedCalculator = spyk(AdvancedCalculator())
        calculator.doAsyncCalc(isCached = true, x = 100, y = 200, z = 300)
        coVerify {
            calculator.doAsyncCalc(isCached = true, x = 100, y = 200, z = 300)
            calculator.doCalc(sum = 600, time = any())
            calculator.queryCache(hash = "100-200-300", success = true, sum = 600, time = any())
        }
    }

    @Test
    fun `doAsyncCalc should NOT call the saveToCache coroutine if isCached is TRUE`() = runBlocking {
        val calculator: AdvancedCalculator = spyk(AdvancedCalculator())
        calculator.doAsyncCalc(isCached = true, x = 100, y = 200, z = 300)
        coVerify {
            calculator.saveToCache(hash = any(), value = any()) wasNot Called
        }
    }

    @Test
    fun `doAsyncCalc should call the saveToCache coroutine if isCached is FALSE`() = runBlocking {
        val calculator: AdvancedCalculator = spyk(AdvancedCalculator())
        calculator.doAsyncCalc(isCached = false, x = 100, y = 200, z = 300)
        coVerify {
            calculator.saveToCache(hash = any(), value = any())
        }
    }
}