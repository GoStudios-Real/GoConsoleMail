package com.gostudios.mail.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gostudios.mail.MailFolder
import com.gostudios.mail.MailMessage
import com.gostudios.mail.MailResult
import com.gostudios.mail.MailStore
import com.gostudios.mail.backend
import kotlinx.coroutines.launch

@Composable
fun HomeScreen() {
    val folders by MailStore.folders.collectAsState()
    val messages by MailStore.messages.collectAsState()
    val busy by MailStore.busy.collectAsState()
    val status by MailStore.status.collectAsState()
    var selectedFolder by remember { mutableStateOf("INBOX") }
    val scope = rememberCoroutineScope()

    Row(Modifier.fillMaxSize()) {
        Column(
            Modifier.width(210.dp).fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant).padding(10.dp)
        ) {
            Text("Folders", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp))
            folders.forEach { f ->
                val active = f.name == selectedFolder
                Row(
                    Modifier.fillMaxWidth()
                        .background(if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.Transparent, RoundedCornerShape(10.dp))
                        .clickable {
                            selectedFolder = f.name
                            scope.launch {
                                MailStore.setBusy(true)
                                val res = backend?.messages(f.name, 50)
                                MailStore.setBusy(false)
                                if (res is MailResult.Ok) MailStore.setMessages(res.value)
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(f.name, color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f))
                    if (f.unread > 0) {
                        Text("${f.unread}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                                .padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("GoMail by GoConsoleOS", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
        }

        Column(Modifier.weight(1f).fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(status, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 13.sp,
                    modifier = Modifier.weight(1f))
                IconButton(onClick = { MailStore.openCompose() }) {
                    Icon(Icons.Default.Add, contentDescription = "Compose", tint = MaterialTheme.colorScheme.primary)
                }
            }
            if (busy) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(messages, key = { it.uid }) { msg ->
                        MessageRow(msg, onClick = {
                            scope.launch {
                                MailStore.setBusy(true)
                                val res = backend?.message(msg.uid)
                                MailStore.setBusy(false)
                                if (res is MailResult.Ok) MailStore.setCurrent(res.value)
                            }
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun MessageRow(msg: MailMessage, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).background(if (msg.read) Color.Transparent else MaterialTheme.colorScheme.primary, CircleShape)) { }
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier.size(36.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(initialOf(msg.from), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(msg.subject.ifBlank { "(no subject)" }, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp,
                fontWeight = if (msg.read) FontWeight.Normal else FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val sub = msg.preview.ifBlank { msg.body.lineSequence().firstOrNull()?.trim() }
            Text("${msg.from}  ·  ${sub.orEmpty()}", color = Color(0xFF94A3B8), fontSize = 13.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Text(friendlyDate(msg.date), color = Color(0xFF64748B), fontSize = 12.sp)
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
}

private fun initialOf(from: String): String {
    val c = from.trim().firstOrNull()?.uppercaseChar()
    return c?.toString() ?: "?"
}

private fun friendlyDate(ts: Long): String {
    if (ts <= 0) return ""
    val now = System.currentTimeMillis()
    val delta = now - ts
    if (delta < 60_000) return "now"
    if (delta < 3600_000) return "${delta / 60_000}m"
    if (delta < 24 * 3600_000) return "${delta / 3600_000}h"
    return "${delta / (24 * 3600_000)}d"
}