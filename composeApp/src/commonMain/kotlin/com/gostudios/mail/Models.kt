package com.gostudios.mail

import kotlinx.serialization.Serializable

@Serializable
data class MailAccount(
    val email: String = "",
    val displayName: String = "",
    val password: String = "",
    val imapHost: String = "imap.gmail.com",
    val imapPort: Int = 993,
    val smtpHost: String = "smtp.gmail.com",
    val smtpPort: Int = 465,
    val useSsl: Boolean = true,
)

@Serializable
data class MailFolder(
    val name: String = "",
    val fullName: String = "",
    val unread: Int = 0,
    val total: Int = 0,
)

@Serializable
data class MailMessage(
    val uid: Long = 0,
    val folder: String = "INBOX",
    val subject: String = "",
    val from: String = "",
    val to: List<String> = emptyList(),
    val body: String = "",
    val isHtml: Boolean = false,
    val date: Long = 0,
    val read: Boolean = false,
    val hasAttachments: Boolean = false,
    val preview: String = "",
)

sealed class MailResult<out T> {
    data class Ok<T>(val value: T) : MailResult<T>()
    data class Err(val message: String) : MailResult<Nothing>()
}

/** Backend interface implemented by Gmail/IMAP (JVM) and demo storage. */
interface MailBackend {
    suspend fun connect(): MailResult<List<MailFolder>>
    suspend fun folders(): MailResult<List<MailFolder>>
    suspend fun messages(folder: String, limit: Int = 50): MailResult<List<MailMessage>>
    suspend fun message(uid: Long): MailResult<MailMessage>
    suspend fun send(to: List<String>, subject: String, body: String): MailResult<Unit>
    suspend fun markRead(uid: Long, read: Boolean): MailResult<Unit>
    suspend fun logout()
    val isDemo: Boolean
}