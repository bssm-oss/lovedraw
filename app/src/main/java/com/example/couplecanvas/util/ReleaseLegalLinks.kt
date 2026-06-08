package com.example.couplecanvas.util

import com.example.couplecanvas.BuildConfig

data class ReleaseLegalLinks(
    val privacyPolicyUrl: String,
    val accountDeletionUrl: String,
    val supportEmail: String,
) {
    val hasPrivacyPolicyUrl: Boolean
        get() = privacyPolicyUrl.isConfiguredReleaseWebUrl()

    val hasAccountDeletionUrl: Boolean
        get() = accountDeletionUrl.isConfiguredReleaseWebUrl()

    val hasSupportEmail: Boolean
        get() = supportEmail.isConfiguredReleaseEmail()

    val isReleaseReady: Boolean
        get() = hasPrivacyPolicyUrl && hasAccountDeletionUrl && hasSupportEmail
}

object ReleaseLegalConfig {
    fun current(): ReleaseLegalLinks = ReleaseLegalLinks(
        privacyPolicyUrl = BuildConfig.PRIVACY_POLICY_URL.trim(),
        accountDeletionUrl = BuildConfig.ACCOUNT_DELETION_URL.trim(),
        supportEmail = BuildConfig.SUPPORT_EMAIL.trim(),
    )
}

fun String.isValidWebUrl(): Boolean {
    val value = trim()
    return (value.startsWith("https://") || value.startsWith("http://")) &&
        "." in value &&
        !value.contains(" ")
}

fun String.isValidEmail(): Boolean {
    val value = trim()
    return value.contains("@") &&
        value.substringAfter("@").contains(".") &&
        !value.contains(" ")
}

fun String.isConfiguredReleaseWebUrl(): Boolean {
    val value = trim()
    return value.isValidWebUrl() &&
        value.startsWith("https://") &&
        !value.hasPlaceholderToken()
}

fun String.isConfiguredReleaseEmail(): Boolean {
    val value = trim()
    return value.isValidEmail() && !value.hasPlaceholderToken()
}

private fun String.hasPlaceholderToken(): Boolean {
    val normalized = lowercase()
    return listOf(
        "your-",
        "example.",
        "localhost",
        "127.0.0.1",
        "10.0.2.2",
    ).any { it in normalized }
}
