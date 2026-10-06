package pro.nitron.zencall

import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun package_name_matches() {
        val expectedPackage = "pro.nitron.zencall"
        assertEquals("pro.nitron.zencall", expectedPackage)
    }
}
