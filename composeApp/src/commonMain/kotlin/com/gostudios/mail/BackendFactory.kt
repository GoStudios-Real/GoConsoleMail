package com.gostudios.mail

/** Creates the real Gmail/IMAP backend on JVM targets (Android + Desktop). */
expect fun createRealBackend(account: MailAccount): MailBackend?