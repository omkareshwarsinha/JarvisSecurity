package com.example.jarvis.domain

import android.net.Uri
import java.net.URI
import java.util.Locale

class PhishingDetector {

    data class PhishingEvaluation(
        val url: String,
        val domain: String,
        val isPhishing: Boolean,
        val riskScore: Int, // 0 to 100
        val severity: String, // SAFE, SUSPICIOUS, HIGH_RISK, PHISHING_ATTACK
        val threatReason: String,
        val brandImpersonated: String = "",
        val indicators: List<String> = emptyList(),
        val recommendedAction: String = "Open in Browser"
    )

    private val targetBrands = mapOf(
        "paypal" to listOf("paypal.com"),
        "google" to listOf("google.com", "accounts.google.com", "gmail.com"),
        "apple" to listOf("apple.com", "icloud.com", "appleid.apple.com"),
        "microsoft" to listOf("microsoft.com", "live.com", "login.microsoftonline.com", "outlook.com"),
        "facebook" to listOf("facebook.com", "fb.com", "meta.com"),
        "amazon" to listOf("amazon.com"),
        "netflix" to listOf("netflix.com"),
        "chase" to listOf("chase.com"),
        "bankofamerica" to listOf("bankofamerica.com"),
        "wellsfargo" to listOf("wellsfargo.com"),
        "binance" to listOf("binance.com"),
        "coinbase" to listOf("coinbase.com"),
        "metamask" to listOf("metamask.io"),
        "instagram" to listOf("instagram.com"),
        "whatsapp" to listOf("whatsapp.com")
    )

    private val highRiskTlds = setOf(
        "xyz", "top", "click", "cam", "buzz", "work", "monster",
        "club", "surf", "stream", "gq", "cf", "ml", "ga", "tk", "support", "rest"
    )

    private val credentialKeywords = listOf(
        "login", "signin", "sign-in", "log-in", "verify", "verification",
        "account", "auth", "security", "wallet", "seed", "phrase", "banking",
        "suspended", "unlock", "update-billing", "password", "reset-pass", "confirm"
    )

    private val urlShorteners = setOf(
        "bit.ly", "tinyurl.com", "t.co", "cutt.ly", "is.gd", "ow.ly", "goo.gl", "buff.ly"
    )

    fun evaluateUrl(rawUrl: String): PhishingEvaluation {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) {
            return PhishingEvaluation(
                url = "",
                domain = "",
                isPhishing = false,
                riskScore = 0,
                severity = "SAFE",
                threatReason = "No URL provided."
            )
        }

        val formattedUrl = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            "https://$trimmed"
        } else {
            trimmed
        }

        val uri = try {
            Uri.parse(formattedUrl)
        } catch (e: Exception) {
            return PhishingEvaluation(
                url = trimmed,
                domain = "malformed",
                isPhishing = true,
                riskScore = 85,
                severity = "HIGH_RISK",
                threatReason = "Malformed URL structure. Cannot safely parse domain."
            )
        }

        val host = uri.host?.lowercase(Locale.ROOT) ?: ""
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: "https"
        val pathAndQuery = "${uri.path ?: ""} ${uri.query ?: ""}".lowercase(Locale.ROOT)

        val indicators = mutableListOf<String>()
        var calculatedRisk = 0
        var impersonatedBrand = ""

        // 1. IP Address as Host Check
        val isIpHost = host.matches(Regex("^(\\d{1,3}\\.){3}\\d{1,3}$"))
        if (isIpHost) {
            calculatedRisk += 65
            indicators.add("Host is a direct raw IP address instead of a registered domain (common malicious dropper tactic).")
        }

        // 2. Insecure HTTP check on authentication keywords
        if (scheme == "http") {
            calculatedRisk += 20
            indicators.add("Unencrypted HTTP transmission: Credentials or data sent over this link are exposed in plaintext.")
        }

        // 3. Brand Impersonation & Typosquatting Check
        for ((brand, legitDomains) in targetBrands) {
            val isLegit = legitDomains.any { host == it || host.endsWith(".$it") }
            if (!isLegit) {
                // Check if brand is contained in host
                if (host.contains(brand)) {
                    calculatedRisk += 70
                    impersonatedBrand = brand.replaceFirstChar { it.uppercase() }
                    indicators.add("Potential Brand Impersonation: Domain contains '$brand' but is NOT an official $brand domain!")
                }

                // Check common typosquat substitutions (0 for o, 1 for l, rn for m)
                val typoVariants = listOf(
                    brand.replace("o", "0"),
                    brand.replace("l", "1"),
                    brand.replace("i", "1"),
                    brand.replace("m", "rn")
                )
                for (variant in typoVariants) {
                    if (variant != brand && host.contains(variant)) {
                        calculatedRisk += 80
                        impersonatedBrand = brand.replaceFirstChar { it.uppercase() }
                        indicators.add("Homoglyph / Typosquatting detected: Spoofing '$brand' using deceptive characters.")
                    }
                }
            }
        }

        // 4. Subdomain Stacking / Deceptive Prefixing
        val parts = host.split(".")
        if (parts.size >= 4) {
            calculatedRisk += 25
            indicators.add("Excessive subdomain depth (${parts.size} levels): Often used to mask real domain authority.")
        }

        // Check if official brand name is a subdomain of an attacker domain (e.g. paypal.com.attacker.xyz)
        for (legit in targetBrands.values.flatten()) {
            if (host.contains(legit) && !host.endsWith(legit)) {
                calculatedRisk += 75
                indicators.add("Deceptive Subdomain Masking: Uses '$legit' as a subdomain to trick users into trusting a foreign root domain.")
            }
        }

        // 5. High-risk TLD analysis
        val tld = host.substringAfterLast(".", "")
        if (highRiskTlds.contains(tld)) {
            calculatedRisk += 30
            indicators.add("Suspicious top-level domain (.$tld): High statistical prevalence for ephemeral phishing deployments.")
        }

        // 6. Credential harvesting keywords in path or query
        val foundKeywords = credentialKeywords.filter { pathAndQuery.contains(it) }
        if (foundKeywords.isNotEmpty()) {
            if (calculatedRisk > 30) {
                calculatedRisk += 25
                indicators.add("Contains sensitive credential harvesting paths: ${foundKeywords.joinToString(", ")}")
            }
        }

        // 7. URL Shortener detection
        if (urlShorteners.contains(host)) {
            indicators.add("URL Shortener detected: The ultimate destination of this link is hidden until redirect.")
            calculatedRisk += 15
        }

        val finalScore = calculatedRisk.coerceIn(0, 100)
        val isPhishing = finalScore >= 50
        val severity = when {
            finalScore >= 70 -> "PHISHING_ATTACK"
            finalScore >= 45 -> "HIGH_RISK"
            finalScore >= 20 -> "SUSPICIOUS"
            else -> "SAFE"
        }

        val threatReason = when {
            finalScore >= 70 && impersonatedBrand.isNotEmpty() ->
                "CRITICAL: This link appears to be a fraudulent phishing site impersonating $impersonatedBrand. Do NOT enter passwords, OTP codes, or personal credentials."
            finalScore >= 70 ->
                "CRITICAL: Highly deceptive URL structure flagged for malicious distribution or credential harvesting."
            finalScore >= 45 ->
                "WARNING: This link displays characteristics commonly associated with deceptive or insecure web destinations."
            finalScore >= 20 ->
                "NOTICE: Minor risk factors detected (unencrypted protocol or obscure domain suffix)."
            else ->
                "VERIFIED: No phishing patterns, typosquatting, or deceptive redirects detected."
        }

        val recommendedAction = when {
            isPhishing -> "Abort to Safety (Do Not Visit)"
            finalScore >= 20 -> "Proceed with Caution"
            else -> "Safe to Visit"
        }

        return PhishingEvaluation(
            url = formattedUrl,
            domain = host,
            isPhishing = isPhishing,
            riskScore = finalScore,
            severity = severity,
            threatReason = threatReason,
            brandImpersonated = impersonatedBrand,
            indicators = indicators,
            recommendedAction = recommendedAction
        )
    }
}
