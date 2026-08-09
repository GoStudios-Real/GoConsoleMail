package com.gostudios.mail

import kotlinx.coroutines.delay

/**
 * A self-contained demo backend so GoConsoleMail works on every platform
 * (Android 13/16, Android TV, Google TV, Windows USB consoles) even without a
 * browser OAuth flow. Real Gmail/IMAP is provided by [com.gostudios.mail.net.GmailBackend].
 */
class DemoBackend : MailBackend {

    private val demoMessages = buildList {
        add(msg("Welcome to GoConsoleOS Mail", "GoConsoleOS Team", "Hello from GoStudios!\n\nThis is your GoMail demo inbox. Sign in with your Gmail app password using the <strong>Add account</strong> flow to read and send real mail.\n\nGoConsoleOS Mail runs on your USB console, Android phones and tablets (Android 13/16), and Android TV / Google TV.", true))
        add(msg("Your console shipped!", "GoStudios", "Your GoConsoleOS USB is ready to game. Plug it in to any PC, TV or Google TV to boot GoConsoleOS.\n\n-Releases".trimIndent(), true))
        add(msg("Update available: v1.8.0", "GoConsoleOS Update", "Version 1.8.0 adds the Account Center, GoAI assistant, lock screen and the new update API.\n\nApply it from Settings > System Update.", true))
        add(msg("You're all caught up", "Inbox", "No new messages.\n\nThis is where new mail will show up once you connect a real account.", false))
    }

    override val isDemo: Boolean = true

    override suspend fun connect() = connectInternal()
    override suspend fun folders() = connectInternal()
    private suspend fun connectInternal(): MailResult<List<MailFolder>> {
        delay(350)
        return MailResult.Ok(
            listOf(
                MailFolder("INBOX", "INBOX", 3, demoMessages.count { it.folder == "INBOX" }),
                MailFolder("Starred", "[Gmail]/Starred", 0, 2),
                MailFolder("Sent", "Sent", 0, 1),
                MailFolder("Drafts", "Drafts", 0, 0),
                MailFolder("Trash", "Trash", 0, 1),
            )
        )
    }

    override suspend fun messages(folder: String, limit: Int) = MailResult.Ok(demoMessages.filter { it.folder == folder })
    override suspend fun message(uid: Long): MailResult<MailMessage> {
        val m = demoMessages.firstOrNull { it.uid == uid } ?: demoMessages.first()
        return MailResult.Ok(m)
    }

    override suspend fun send(to: List<String>, subject: String, body: String): MailResult<Unit> {
        delay(250)
        MailStore.setStatus("Demo: message queued (no real SMTP connection). Connect a real account to send mail.")
        return MailResult.Ok(Unit)
    }

    override suspend fun markRead(uid: Long, read: Boolean): MailResult<Unit> = MailResult.Ok(Unit)
    override suspend fun logout() {
        MailStore.setStatus("Disconnected")
        MailStore.setConnected(false, false)
        MailStore.setFolders(emptyList())
        MailStore.setMessages(emptyList())
        MailStore.setCurrent(null)
    }

    private fun msg(subject: String, from: String, body: String, isHtml: Boolean) =
        MailMessage(
            uid = nextUid++,
            folder = "INBOX",
            subject = subject,
            from = from,
            body = body,
            isHtml = isHtml,
            date = System.currentTimeMillis() - (1000L * 60 * 60) * nextUid,
        )

    companion object {
        var nextUid = 1L
    }
}