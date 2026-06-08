package com.example.couplecanvas.util

import com.example.couplecanvas.BuildConfig

data class ReleaseLegalLinks(
    val privacyPolicyUrl: String,
    val accountDeletionUrl: String,
    val supportEmail: String,
) {
    val hasPrivacyPolicyUrl: Boolean
        get() = privacyPolicyUrl.isValidWebUrl()

    val hasAccountDeletionUrl: Boolean
        get() = accountDeletionUrl.isValidWebUrl()

    val hasSupportEmail: Boolean
        get() = supportEmail.isValidEmail()

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
