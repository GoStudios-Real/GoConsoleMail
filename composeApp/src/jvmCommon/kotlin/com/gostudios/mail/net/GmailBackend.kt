package com.gostudios.mail.net

import com.gostudios.mail.MailAccount
import com.gostudios.mail.MailBackend
import com.gostudios.mail.MailFolder
import com.gostudios.mail.MailMessage
import com.gostudios.mail.MailResult
import com.gostudios.mail.MailStore
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Multipart
import javax.mail.Session
import javax.mail.Store
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import javax.mail.search.FlagTerm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Flags

/** Real Gmail backend using Jakarta Mail over IMAPS (993) and SMTPS (465). Works with Gmail app passwords. */
class GmailBackend(private val account: MailAccount) : MailBackend {

    override val isDemo: Boolean = false

    private var store: Store? = null

    private fun session(): Session {
        val props = Properties().apply {
            put("mail.store.protocol", "imaps")
            put("mail.imaps.host", account.imapHost)
            put("mail.imaps.port", account.imapPort)
            put("mail.imaps.ssl.enable", "true")
            put("mail.imaps.ssl.trust", "*")
            put("mail.imaps.timeout", "30000")
            put("mail.imaps.connectiontimeout", "30000")
            put("mail.smtp.host", account.smtpHost)
            put("mail.smtp.port", account.smtpPort)
            put("mail.smtp.ssl.enable", "true")
            put("mail.smtp.auth", "true")
            put("mail.smtp.ssl.trust", "*")
        }
        return Session.getInstance(props, null)
    }

    override suspend fun connect(): MailResult<List<MailFolder>> = withContext(Dispatchers.IO) {
        runCatching {
            val s = session().getStore("imaps")
            s.connect(account.imapHost, account.imapPort, account.email, account.password)
            store = s
            MailResult.Ok(readFolders())
        }.getOrElse { e ->
            runCatching { store?.close() }
            store = null
            MailResult.Err(niceError(e))
        }
    }

    override suspend fun folders(): MailResult<List<MailFolder>> = withContext(Dispatchers.IO) {
        if (store == null) MailResult.Err("Not connected. Tap Connect first.")
        else runCatching { MailResult.Ok(readFolders()) }.getOrElse { MailResult.Err(niceError(it)) }
    }

    override suspend fun messages(folderName: String, limit: Int): MailResult<List<MailMessage>> = withContext(Dispatchers.IO) {
        val s = store ?: return@withContext MailResult.Err("Not connected")
        try {
            val folder = s.getFolder(folderName)
            folder.open(Folder.READ_WRITE)
            val all = folder.messages
            val take = minOf(limit, all.size)
            val list = all.takeLast(take).map { it.toMessage(folderName).copy(body = readBody(it).take(160)) }
            folder.close(false)
            MailResult.Ok(list.sortedByDescending { it.date })
        } catch (e: Exception) {
            MailResult.Err(niceError(e))
        }
    }

    override suspend fun message(uid: Long): MailResult<MailMessage> = withContext(Dispatchers.IO) {
        val s = store ?: return@withContext MailResult.Err("Not connected")
        try {
            val folder = s.getFolder("INBOX")
            folder.open(Folder.READ_WRITE)
            val msg = folder.messages.firstOrNull { it.toMessage("INBOX").uid == uid }
            val model = msg?.toMessage("INBOX")
            val body = if (msg != null) readBody(msg) else ""
            folder.close(false)
            if (model != null) MailResult.Ok(model.copy(body = body)) else MailResult.Err("Message not found")
        } catch (e: Exception) { MailResult.Err(niceError(e)) }
    }

    override suspend fun send(to: List<String>, subject: String, body: String): MailResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val appMsg = MimeMessage(session()).apply {
                setFrom(InternetAddress(account.email))
                setRecipients(Message.RecipientType.TO, to.map { InternetAddress(it.trim()) }.toTypedArray())
                setSubject(subject, "UTF-8")
                setText(body, "UTF-8")
            }
            Transport.send(appMsg)
            MailStore.setStatus("Message sent to ${to.size} recipient(s)")
            MailResult.Ok(Unit)
        } catch (e: Exception) { MailResult.Err(e.message ?: "SMTP send failed") }
    }

    override suspend fun markRead(uid: Long, read: Boolean): MailResult<Unit> = withContext(Dispatchers.IO) {
        val s = store ?: return@withContext MailResult.Err("Not connected")
        try {
            val folder = s.getFolder("INBOX")
            folder.open(Folder.READ_WRITE)
            val target = folder.messages.firstOrNull { it.toMessage("INBOX").uid == uid }
            if (target != null) target.setFlags(Flags(Flags.Flag.SEEN), read)
            folder.close(false)
            MailResult.Ok(Unit)
        } catch (e: Exception) { MailResult.Err(niceError(e)) }
    }

    override suspend fun logout() {
        runCatching { store?.close() }
        store = null
        MailStore.setConnected(false, false)
    }

    // ---------- helpers ----------

    private fun readFolders(): List<MailFolder> {
        val s = store ?: return emptyList()
        val out = mutableListOf<MailFolder>()
        val inbox = s.getFolder("INBOX")
        inbox.open(Folder.READ_ONLY)
        val unseen = inbox.search(UNSEEN).size
        out += MailFolder(
            name = "INBOX",
            fullName = "INBOX",
            unread = unseen,
            total = inbox.messageCount,
        )
        inbox.close(false)

        runCatching {
            val def = s.defaultFolder
            val list = def.list("*")
            list.forEach {
                val full = it.fullName
                if (full == "INBOX") return@forEach
                val name = full.removePrefix("INBOX.").removePrefix("[Gmail]/")
                if (name.isBlank() || name.startsWith("[")) return@forEach
                val unread = runCatching {
                    it.open(Folder.READ_ONLY)
                    val u = it.search(UNSEEN).size
                    it.close(false)
                    u
                }.getOrDefault(0)
                out += MailFolder(name, full, unread, it.messageCount)
            }
        }
        return out
    }

    private fun Message.toMessage(folderName: String): MailMessage {
        val from = runCatching { getFrom()?.joinToString(", ") { a -> (a as? InternetAddress)?.address ?: a.toString() } ?: "" }.getOrDefault("")
        val subject = runCatching { getSubject() ?: "" }.getOrDefault("")
        val date = runCatching { sentDate?.time ?: receivedDate?.time ?: System.currentTimeMillis() }.getOrDefault(System.currentTimeMillis())
        val hasAtt = runCatching { getContent() is Multipart }.getOrDefault(false)
        return MailMessage(
            uid = hashCode().toLong(),
            folder = folderName,
            subject = subject,
            from = from,
            date = date,
            read = runCatching { isSet(Flags.Flag.SEEN) }.getOrDefault(false),
            hasAttachments = hasAtt,
            preview = subject,
        )
    }

    private fun readBody(msg: Message): String {
        return try {
            val c = msg.content
            when (c) {
                is String -> c
                is Multipart -> {
                    val builder = StringBuilder()
                    for (i in 0 until c.count) {
                        val bp = c.getBodyPart(i)
                        if (bp.isMimeType("text/plain")) builder.append(bp.content as? String ?: "")
                    }
                    if (builder.isNotBlank()) builder.toString()
                    else {
                        val builderHtml = StringBuilder()
                        for (i in 0 until c.count) {
                            val bp = c.getBodyPart(i)
                            if (bp.isMimeType("text/html")) builderHtml.append(bp.content as? String ?: "")
                        }
                        builderHtml.toString()
                    }
                }
                else -> ""
            }
        } catch (e: Exception) { "" }
    }

    private fun niceError(e: Throwable): String {
        val m = e.message ?: "unknown error"
        return when {
            m.contains("AUTHENTICATIONFAILED") -> "Authentication failed. Use a Gmail App Password (requires 2-Step Verification), not your normal password."
            m.contains("UnknownHost", true) -> "Cannot resolve ${account.imapHost}. Check host / network."
            m.contains("ConnectException", true) -> "Connection refused on ${account.imapHost}:${account.imapPort}. Check host & port."
            else -> m
        }
    }

    companion object {
        private val UNSEEN = FlagTerm(Flags(Flags.Flag.SEEN), false)
    }
}