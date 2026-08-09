package com.gostudios.mail

import com.gostudios.mail.net.GmailBackend

actual fun createRealBackend(account: MailAccount): MailBackend? {
    return if (account.email.isNotBlank() && account.password.isNotBlank()) {
        GmailBackend(account)
    } else null
}