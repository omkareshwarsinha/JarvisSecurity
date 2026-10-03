package com.example.jarvis.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsCommandParserTest {

    @Test
    fun testLockdownWithPrefixes() {
        val cmdSlash = SmsCommandParser.parse("/lockdown")
        assertTrue(cmdSlash is ParsedSmsCommand.Lockdown)
        assertNull((cmdSlash as ParsedSmsCommand.Lockdown).pinOrNonce)

        val cmdHash = SmsCommandParser.parse("#lockdown")
        assertTrue(cmdHash is ParsedSmsCommand.Lockdown)

        val cmdBang = SmsCommandParser.parse("!LOCKDOWN")
        assertTrue(cmdBang is ParsedSmsCommand.Lockdown)
    }

    @Test
    fun testLockdownWithPinOrNonce() {
        val result = SmsCommandParser.parse("#lockdown 49201")
        assertTrue(result is ParsedSmsCommand.Lockdown)
        assertEquals("49201", (result as ParsedSmsCommand.Lockdown).pinOrNonce)

        val resultSlash = SmsCommandParser.parse("/lockdown NONCE_998")
        assertTrue(resultSlash is ParsedSmsCommand.Lockdown)
        assertEquals("NONCE_998", (resultSlash as ParsedSmsCommand.Lockdown).pinOrNonce)
    }

    @Test
    fun testSirenCommand() {
        val result = SmsCommandParser.parse("#siren")
        assertTrue(result is ParsedSmsCommand.Siren)

        val resultWithPin = SmsCommandParser.parse("/SIREN 1234")
        assertTrue(resultWithPin is ParsedSmsCommand.Siren)
        assertEquals("1234", (resultWithPin as ParsedSmsCommand.Siren).pinOrNonce)
    }

    @Test
    fun testLocationAndGpsAliases() {
        assertTrue(SmsCommandParser.parse("/location") is ParsedSmsCommand.Location)
        assertTrue(SmsCommandParser.parse("#loc") is ParsedSmsCommand.Location)
        assertTrue(SmsCommandParser.parse("!gps") is ParsedSmsCommand.Location)
        assertTrue(SmsCommandParser.parse("location") is ParsedSmsCommand.Location)
        assertTrue(SmsCommandParser.parse("gps") is ParsedSmsCommand.Location)
    }

    @Test
    fun testSilenceAndStopAliases() {
        assertTrue(SmsCommandParser.parse("/silence") is ParsedSmsCommand.Silence)
        assertTrue(SmsCommandParser.parse("#stop") is ParsedSmsCommand.Silence)
        assertTrue(SmsCommandParser.parse("silence") is ParsedSmsCommand.Silence)
        assertTrue(SmsCommandParser.parse("stop") is ParsedSmsCommand.Silence)
    }

    @Test
    fun testRejectionOfConversationalSentences() {
        // Critical defense test: conversational text containing keywords MUST NOT trigger commands
        assertEquals(ParsedSmsCommand.Unknown, SmsCommandParser.parse("The entire city is now in lockdown due to weather"))
        assertEquals(ParsedSmsCommand.Unknown, SmsCommandParser.parse("I heard an emergency siren outside"))
        assertEquals(ParsedSmsCommand.Unknown, SmsCommandParser.parse("What is your current location right now?"))
        assertEquals(ParsedSmsCommand.Unknown, SmsCommandParser.parse("Please silence your phones before the movie"))
    }

    @Test
    fun testMalformedOrEmptyInput() {
        assertEquals(ParsedSmsCommand.Unknown, SmsCommandParser.parse(""))
        assertEquals(ParsedSmsCommand.Unknown, SmsCommandParser.parse("   "))
        assertEquals(ParsedSmsCommand.Unknown, SmsCommandParser.parse("hello world"))
        assertEquals(ParsedSmsCommand.Unknown, SmsCommandParser.parse("/unknowncommand"))
        assertEquals(ParsedSmsCommand.Unknown, SmsCommandParser.parse("#restart"))
    }
}
