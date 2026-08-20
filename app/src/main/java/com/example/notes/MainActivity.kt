package com.example.notes

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private data class Note(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val text: String,
    val pinned: Boolean = false
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NotesApp(applicationContext) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.compose.runtime.Composable
private fun NotesApp(context: Context) {
    val store = remember { NoteStore(context) }
    var notes by remember { mutableStateOf(store.load()) }
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Note?>(null) }

    val filtered = notes
        .filter { it.title.contains(query, true) || it.text.contains(query, true) }
        .sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.id })

    Scaffold(
        topBar = { TopAppBar(title = { Text("Заметки") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = Note(title = "", text = "") }) {
                Icon(Icons.Default.Add, contentDescription = "Новая заметка")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Поиск") }
            )
            Spacer(Modifier.height(12.dp))

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (query.isEmpty()) "Заметок пока нет" else "Ничего не найдено")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filtered, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onOpen = { editing = note },
                            onPin = {
                                notes = notes.map { if (it.id == note.id) it.copy(pinned = !it.pinned) else it }
                                store.save(notes)
                            },
                            onDelete = {
                                notes = notes.filterNot { it.id == note.id }
                                store.save(notes)
                            }
                        )
                    }
                }
            }
        }
    }

    editing?.let { note ->
        NoteEditor(
            note = note,
            onDismiss = { editing = null },
            onSave = { updated ->
                notes = if (notes.any { it.id == updated.id }) {
                    notes.map { if (it.id == updated.id) updated else it }
                } else notes + updated
                store.save(notes)
                editing = null
            }
        )
    }
}

@androidx.compose.runtime.Composable
private fun NoteCard(
    note: Note,
    onOpen: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit
) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = note.title.ifBlank { "Без названия" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onPin) {
                    Icon(
                        if (note.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = if (note.pinned) "Открепить" else "Закрепить"
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить")
                }
            }
            if (note.text.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(note.text, maxLines = 4, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun NoteEditor(note: Note, onDismiss: () -> Unit, onSave: (Note) -> Unit) {
    var title by remember(note.id) { mutableStateOf(note.title) }
    var text by remember(note.id) { mutableStateOf(note.text) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (note.title.isBlank() && note.text.isBlank()) "Новая заметка" else "Редактировать") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Заголовок") }, singleLine = true)
                OutlinedTextField(text, { text = it }, label = { Text("Текст") }, minLines = 5)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(note.copy(title = title.trim(), text = text.trim())) }) {
                Text("Сохранить")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

private class NoteStore(context: Context) {
    private val prefs = context.getSharedPreferences("notes", Context.MODE_PRIVATE)

    fun load(): List<Note> = runCatching {
        val array = JSONArray(prefs.getString("items", "[]"))
        List(array.length()) { index ->
            val o = array.getJSONObject(index)
            Note(o.getString("id"), o.optString("title"), o.optString("text"), o.optBoolean("pinned"))
        }
    }.getOrDefault(emptyList())

    fun save(notes: List<Note>) {
        val array = JSONArray()
        notes.forEach { note ->
            array.put(JSONObject().apply {
                put("id", note.id)
                put("title", note.title)
                put("text", note.text)
                put("pinned", note.pinned)
            })
        }
        prefs.edit().putString("items", array.toString()).apply()
    }
}
