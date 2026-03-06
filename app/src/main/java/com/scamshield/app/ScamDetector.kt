package com.scamshield.app

data class ScanResult(
    val score: Int,
    val label: String,
    val reasons: List<String>
)

class ScamDetector {

    // =========================
    // 🔥 Core Detection Patterns
    // =========================

    private val legitHeaderRegex =
        Regex("^[A-Z]{2}-[A-Z0-9]{5,9}-[A-Z]$", RegexOption.IGNORE_CASE)

    private val trustedBrandRegex = Regex(
        "\\b(domino|shein|swiggy|zomato|amazon|flipkart|hdfc|sbi|icici|axis)\\b",
        RegexOption.IGNORE_CASE
    )

    private val urgencyRegex = Regex(
        "\\b(urgent|immediately|asap|expire[d]?|limited\\s*time|final\\s*warning|last\\s*chance)\\b",
        RegexOption.IGNORE_CASE
    )

    private val sensitiveRegex = Regex(
        "\\b(verify|kyc|update\\s*account|bank|atm|card\\s*blocked|pin)\\b",
        RegexOption.IGNORE_CASE
    )

    private val rewardRegex = Regex(
        "\\b(won|winner|prize|lottery|reward|cashback|free|congratulations|gift)\\b",
        RegexOption.IGNORE_CASE
    )

    private val genericDomainRegex = Regex(
        "\\b[a-zA-Z0-9-]+\\.[a-zA-Z]{2,}\\b",
        RegexOption.IGNORE_CASE
    )

    private val suspiciousTldRegex = Regex(
        "\\.(ru|xyz|top|click|gq|tk|ml|ga|cf)\\b",
        RegexOption.IGNORE_CASE
    )

    private val shortenerRegex = Regex(
        "\\b(bit\\.ly|tinyurl\\.com|t\\.co|goo\\.gl|shorturl\\.at|rebrand\\.ly)\\b",
        RegexOption.IGNORE_CASE
    )

    private val moneyRegex = Regex(
        "(₹|rs\\.?\\s?)?\\s?\\d{1,3}(,\\d{3})+|\\b\\d{5,}\\b",
        RegexOption.IGNORE_CASE
    )

    private val digitHeavyRegex = Regex("\\b\\d{6,}\\b")

    private val clickRegex = Regex(
        "\\b(cl[i1]ck|tap|open|visit)\\b",
        RegexOption.IGNORE_CASE
    )

    // =========================
    // 🔍 Analyzer
    // =========================

    fun analyze(text: String?, sender: String? = null): ScanResult {

        if (text.isNullOrBlank()) {
            return ScanResult(0, "No Content", emptyList())
        }

        val lower = text.lowercase()
        val trimmedSender = sender?.trim() ?: ""

        val isLegitHeader = legitHeaderRegex.containsMatchIn(trimmedSender)
        val trustedBrandHit = trustedBrandRegex.containsMatchIn(lower)

        val isOtpMessage =
            (lower.contains("otp") || lower.contains("one time password")) &&
                    digitHeavyRegex.containsMatchIn(lower)

        val linkHit = genericDomainRegex.containsMatchIn(lower)
        val rewardHit = rewardRegex.containsMatchIn(lower)
        val urgencyHit = urgencyRegex.containsMatchIn(lower)
        val moneyHit = moneyRegex.containsMatchIn(lower)
        val shortenerHit = shortenerRegex.containsMatchIn(lower)

        val looksTransactional =
            !linkHit &&
                    !rewardHit &&
                    !urgencyHit &&
                    !moneyHit &&
                    !shortenerHit

        // =========================
        // 🛡 EARLY WHITELIST (Before Scoring)
        // =========================

        if (isOtpMessage && looksTransactional && (isLegitHeader || trustedBrandHit)) {
            return ScanResult(
                score = 5,
                label = "✔ Likely Safe",
                reasons = listOf("Verified transactional OTP")
            )
        }

        // =========================
        // 🧠 Scoring
        // =========================

        var score = 0
        val reasons = mutableListOf<String>()

        val sensitiveHit = sensitiveRegex.containsMatchIn(lower)
        val suspiciousTldHit = suspiciousTldRegex.containsMatchIn(lower)
        val digitHeavyHit = digitHeavyRegex.containsMatchIn(lower)
        val clickHit = clickRegex.containsMatchIn(lower)

        if (urgencyHit) {
            score += 25
            reasons.add("Urgency language detected")
        }

        if (sensitiveHit) {
            score += 35
            reasons.add("Sensitive information request detected")
        }

        if (rewardHit) {
            score += 25
            reasons.add("Reward bait language detected")
        }

        if (linkHit) {
            score += 20
            reasons.add("Link detected")
        }

        if (suspiciousTldHit) {
            score += 55
            reasons.add("Suspicious domain extension detected")
        }

        if (shortenerHit) {
            score += 50
            reasons.add("Shortened URL detected")
        }

        if (moneyHit) {
            score += 25
            reasons.add("Large money amount mentioned")
        }

        if (digitHeavyHit && !isOtpMessage) {
            score += 10
            reasons.add("Unusual numeric sequence detected")
        }

        if (clickHit) {
            score += 15
            reasons.add("Call-to-action language detected")
        }

        // =========================
        // 🚨 Smart Combo Escalation
        // =========================

        if (rewardHit && moneyHit && linkHit) {
            score += 70
            reasons.add("High-risk combo: Reward + Money + Link")
        }

        if (urgencyHit && sensitiveHit) {
            score += 50
            reasons.add("High-risk combo: Urgency + Sensitive request")
        }

        if (shortenerHit && rewardHit) {
            score += 60
            reasons.add("High-risk combo: Short link + Reward bait")
        }

        if (clickHit && linkHit && moneyHit) {
            score += 50
            reasons.add("High-risk combo: Click + Link + Money")
        }

        // =========================
        // 🏷 Final Classification
        // =========================

        val label = when {
            score >= 150 -> "🚨 High Scam Risk"
            score >= 80 -> "⚠ Suspicious"
            else -> "✔ Likely Safe"
        }

        return ScanResult(score, label, reasons)
    }
}
