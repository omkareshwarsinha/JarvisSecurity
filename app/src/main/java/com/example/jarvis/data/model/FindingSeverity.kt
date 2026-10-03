package com.example.jarvis.data.model

enum class FindingSeverity(val label: String, val weight: Int) {
    CRITICAL("Critical Risk", 4),
    WARNING("Warning", 3),
    INFORMATIONAL("Informational", 2),
    VERIFIED_SECURE("Verified Secure", 1),
    NOT_AVAILABLE("Not Available", 0)
}
