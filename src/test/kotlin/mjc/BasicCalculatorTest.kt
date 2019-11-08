package mjc

import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import kotlin.test.assertEquals

class BasicCalculatorTest {
    @ExperimentalCoroutinesApi
    @Test
    fun `should sum three calculations`() = runBlockingTest {
        val actual: Int = BasicCalculator().calculate(x = 300, y = 200, z = 100)
        assertEquals(expected = 600, actual = actual)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `should create 3 coroutines`() = runBlockingTest {
        val basicCalculator: BasicCalculator = spyk(BasicCalculator())
        basicCalculator.calculate(x = 300, y = 200, z = 100)
        coVerify {
            basicCalculator.calc(x = 300, time = any())
            basicCalculator.calc(x = 200, time = any())
            basicCalculator.calc(x = 100, time = any())
        }
    }

    @ExperimentalCoroutinesApi
    @Test(expected = TestException::class)
    fun `should throw exception if a coroutine fails`() = runBlockingTest {
        val mockCalculator: BasicCalculator = mockk<BasicCalculator>()

        coEvery {
            mockCalculator.calculate(x = 300, y = 200, z = 100)
        } coAnswers {
            mockCalculator.calc(x = 300, time = 5)
            mockCalculator.calc(x = 200, time = 5)
            mockCalculator.calc(x = 100, time = 5)
        }

        coEvery { mockCalculator.calc(x = 300, time = any()) } returns 300
        coEvery { mockCalculator.calc(x = 200, time = any()) } returns 200
        coEvery {
            mockCalculator.calc(x = 100, time = any())
        } throws TestException("this should be thrown")

        mockCalculator.calculate(x = 300, y = 200, z = 100)
    }

    private class TestException(message: String) : Throwable(message)
}





