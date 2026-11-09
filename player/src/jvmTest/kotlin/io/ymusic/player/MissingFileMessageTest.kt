package io.ymusic.player

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MissingFileMessageTest {
    @Test
    fun classifies_libav_and_os_missing_file_wording() {
        assertTrue(looksLikeMissingFile("No such file or directory"))
        assertTrue(looksLikeMissingFile("Error opening input: No such file or directory"))
        assertTrue(looksLikeMissingFile("ENOENT: path gone"))
        assertTrue(looksLikeMissingFile("File not found"))
        assertFalse(looksLikeMissingFile("codec not supported"))
        assertFalse(looksLikeMissingFile("Decoder error"))
    }
}
