package org.kaloscope.tv.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseVersionTest {
    @Test
    fun `stable tags compare numerically across component boundaries`() {
        val versions = listOf("0.3.9", "v0.3.10", "0.3.22", "0.4.0", "1.0.0", "10.0.0")
            .map { requireNotNull(ReleaseVersion.parse(it)) }
        versions.zipWithNext().forEach { (old, new) -> assertTrue(new > old) }
        assertEquals(ReleaseVersion.parse("0.3.22"), ReleaseVersion.parse("v0.3.22"))
    }

    @Test
    fun `malformed and prerelease tags cannot be offered as stable updates`() {
        listOf("", "v", "1.2", "1.2.3.4", "1.2.3-beta.1", "01.2.3", "1.-2.3", "99999999999999999999.0.0")
            .forEach { assertNull(it, ReleaseVersion.parse(it)) }
    }
}
