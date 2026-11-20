package com.medilife.ai.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.medilife.ai.data.ChatMessageEntity
import com.medilife.ai.engine.AiSource
import com.medilife.ai.ui.MediViewModel
import com.medilife.ai.ui.components.ConfirmDialog
import com.medilife.ai.ui.components.IconBubble
import com.medilife.ai.ui.components.Pill
import com.medilife.ai.ui.theme.LocalMedi
import com.medilife.ai.ui.theme.Palette

private val suggestions = listOf(
    "Explain my medical history",
    "What are the precautions for my current medicines?",
    "Explain my blood test report",
    "What does my blood pressure reading mean?",
    "What medicine should I take for my headache?",
    "Give me a healthy daily routine",
    "What questions should I ask my doctor?",
    "Summarize my health records"
)

@Composable
fun ChatScreen(vm: MediViewModel) {
    val msgs by vm.chat.collectAsState()
    val thinking by vm.thinking.collectAsState()
    val settings by vm.settings.collectAsState()
    var input by remember { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }
    val list = rememberLazyListState()
    LaunchedEffect(msgs.size, thinking) { if (msgs.isNotEmpty()) list.animateScrollToItem(msgs.size + if (thinking) 1 else 0) }
    val llmOn = settings?.llmEnabled == true && vm.llmAvailable(settings?.llmModelPath ?: "")

    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            AiOrb(thinking)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Medi AI", style = MaterialTheme.typography.headlineSmall)
                Text(if (llmOn) "Rule engine + on-device model · offline" else "Offline rule-based engine · no cloud AI", style = MaterialTheme.typography.labelSmall, color = LocalMedi.current.subtle)
            }
            IconButton(onClick = { confirmClear = true }) { Icon(Icons.Rounded.DeleteSweep, "Clear chat") }
        }
        LazyColumn(Modifier.weight(1f).padding(horizontal = 14.dp), state = list, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Bubble(ChatMessageEntity(role = "ASSISTANT", text = "Hi, I'm Medi AI. I explain your medicines, records, readings and lab results in plain language, using your profile. I don't diagnose, prescribe or change doses — and I'll always tell you which engine produced an answer.\n\nIf you have chest pain, trouble breathing, stroke signs or a severe allergic reaction, call emergency services now.", source = AiSource.SYSTEM))
                }
            }
            items(msgs, key = { it.id }) { Bubble(it) }
            if (thinking) item { Text("Medi AI is thinking…", style = MaterialTheme.typography.labelMedium, color = LocalMedi.current.subtle, modifier = Modifier.padding(8.dp)) }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            suggestions.forEach { s -> AssistChip(onClick = { vm.send(s) }, label = { Text(s) }) }
        }
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(input, { input = it }, placeholder = { Text("Ask about your health…") }, modifier = Modifier.weight(1f), maxLines = 4, shape = RoundedCornerShape(24.dp))
            Spacer(Modifier.width(8.dp))
            FilledIconButton(onClick = { vm.send(input.trim()); input = "" }, enabled = input.isNotBlank() && !thinking, modifier = Modifier.size(52.dp)) {
                Icon(Icons.AutoMirrored.Rounded.Send, "Send")
            }
        }
    }
    if (confirmClear) ConfirmDialog("Clear conversation?", "This deletes the chat history stored on this device.", "Clear", true, { confirmClear = false }) { vm.clearChat() }
}

@Composable
private fun AiOrb(thinking: Boolean) {
    val reduce = LocalMedi.current.reduceMotion
    val t = rememberInfiniteTransition(label = "orb")
    val s by t.animateFloat(1f, if (reduce) 1f else if (thinking) 1.2f else 1.07f, infiniteRepeatable(tween(if (thinking) 450 else 1400), RepeatMode.Reverse), label = "s")
    Box(contentAlignment = Alignment.Center) {
        Box(Modifier.size(54.dp).scale(s).clip(CircleShape).background(Brush.radialGradient(listOf(Palette.Cyan.copy(alpha = 0.45f), Color.Transparent))))
        IconBubble(Icons.Rounded.AutoAwesome, Palette.primaryGradient, 40.dp)
    }
}

@Composable
private fun Bubble(m: ChatMessageEntity) {
    val user = m.role == "USER"
    val glass = LocalMedi.current.glass
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (user) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier.widthIn(max = 330.dp)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = if (user) 20.dp else 4.dp, bottomEnd = if (user) 4.dp else 20.dp))
                .background(
                    when {
                        user -> Palette.primaryGradient
                        m.emergency -> Palette.dangerGradient
                        else -> Brush.linearGradient(listOf(glass, glass))
                    }
                )
                .padding(12.dp)
        ) {
            if (!user) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Pill(AiSource.label(m.source).uppercase(), when (m.source) { AiSource.LLM -> Palette.Purple; AiSource.SYSTEM -> Palette.Teal; else -> Palette.Cyan })
                }
                Spacer(Modifier.size(6.dp))
            }
            Text(m.text, style = MaterialTheme.typography.bodyMedium, color = if (user || m.emergency) Color.White else MaterialTheme.colorScheme.onBackground)
        }
    }
}
