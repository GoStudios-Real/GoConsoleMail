package com.gostudios.mail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.gostudios.mail.ui.HomeScreen
import com.gostudios.mail.ui.LoginScreen
import com.gostudios.mail.ui.MessageView
import com.gostudios.mail.ui.ComposeView
import kotlinx.coroutines.launch

@Composable
fun App() {
    val connected by MailStore.connected.collectAsState()
    val current by MailStore.current.collectAsState()
    val demo = MailStore.demoMode.collectAsState().value
    val scope = rememberCoroutineScope()

    MaterialTheme(colorScheme = GoColors) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize()) {
                when {
                    current != null -> {
                        MessageView(current!!, onBack = { MailStore.setCurrent(null) },
                            onReply = {
                                MailStore.setCurrent(null)
                                MailStore.setCompose(
                                    MailComposeState(
                                        to = current!!.from,
                                        subject = if (current!!.subject.startsWith("Re:")) current!!.subject else "Re: ${current!!.subject}",
                                    )
                                )
                                MailStore.openCompose()
                            },
                            onDelete = {
                                val m = current!!
                                scope.launch { runCatching { backend?.markRead(m.uid, false) } }
                                MailStore.setCurrent(null)
                            })
                    }
                    MailStore.composeOpen.collectAsState().value -> {
                        ComposeView(onBack = { MailStore.closeCompose() })
                    }
                    !connected && !demo -> LoginScreen()
                    else -> HomeScreen()
                }
            }
        }
    }
}