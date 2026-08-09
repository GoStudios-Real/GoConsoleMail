package com.gostudios.mail.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gostudios.mail.MailMessage
import com.gostudios.mail.MailStore

@Composable
fun MessageView(msg: MailMessage, onBack: () -> Unit, onReply: () -> Unit, onDelete: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Row(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text("Message", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                modifier = Modifier.weight(1f))
            IconButton(onClick = onReply) { Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = "Reply", tint = MaterialTheme.colorScheme.primary) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Mark spam", tint = MaterialTheme.colorScheme.error) }
        }

        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text(msg.subject.ifBlank { "(no subject)" }, color = MaterialTheme.colorScheme.onSurface,
                fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center) {
                    Text(msg.from.firstOrNull()?.uppercaseChar()?.toString() ?: "?", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(msg.from, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    Text(friendlyFullDate(msg.date), color = Color(0xFF94A3B8), fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(msg.body.ifBlank { "(empty message)" }, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f), fontSize = 15.sp, lineHeight = 22.sp)
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun friendlyFullDate(ts: Long): String {
    if (ts <= 0) return ""
    val delta = System.currentTimeMillis() - ts
    val v = when {
        delta < 60_000 -> "just now"
        delta < 3600_000 -> "${delta / 60_000} min ago"
        delta < 24 * 3600_000 -> "${delta / 3600_000} hours ago"
        delta < 7 * 24 * 3600_000 -> "${delta / (24 * 3600_000)} days ago"
        else -> ""
    }
    return v.ifBlank { "some time ago" }
}