package com.example.jarvis.domain

sealed class ParsedSmsCommand {
    data class Lockdown(val pinOrNonce: String? = null) : ParsedSmsCommand()
    data class Siren(val pinOrNonce: String? = null) : ParsedSmsCommand()
    data class Location(val pinOrNonce: String? = null) : ParsedSmsCommand()
    data class Silence(val pinOrNonce: String? = null) : ParsedSmsCommand()
    data object Unknown : ParsedSmsCommand()
}

object SmsCommandParser {
    private val validPrefixes = setOf("/", "#", "!")

    /**
     * Parses an incoming SMS message body into a recognized security command.
     * Prevents false-positive triggering from conversational texts (e.g., "The city is in lockdown").
     * Supports optional PIN/passcode or nonce as the second token (e.g., "#lockdown 9821").
     */
    fun parse(messageBody: String): ParsedSmsCommand {
        val trimmed = messageBody.trim()
        if (trimmed.isEmpty()) return ParsedSmsCommand.Unknown

        val tokens = trimmed.split("\\s+".toRegex())
        val firstToken = tokens[0]
        val pinOrNonce = if (tokens.size > 1) tokens[1].trim() else null

        val isPrefixed = validPrefixes.any { firstToken.startsWith(it) }
        val rawCommand = if (isPrefixed) {
            firstToken.substring(1).lowercase()
        } else {
            // For non-prefixed, only allow if it's an exact single-word command
            if (tokens.size <= 2) firstToken.lowercase() else return ParsedSmsCommand.Unknown
        }

        return when (rawCommand) {
            "lockdown" -> ParsedSmsCommand.Lockdown(pinOrNonce)
            "siren" -> ParsedSmsCommand.Siren(pinOrNonce)
            "location", "loc", "gps" -> ParsedSmsCommand.Location(pinOrNonce)
            "silence", "stop" -> ParsedSmsCommand.Silence(pinOrNonce)
            else -> ParsedSmsCommand.Unknown
        }
    }
}
