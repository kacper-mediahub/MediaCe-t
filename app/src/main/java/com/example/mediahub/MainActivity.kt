package com.example.mediahub

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

data class MediaItem(
    val id: Long,
    val title: String,
    val source: String,
    val uri: String? = null,
    val link: String? = null
)

class MainActivity : ComponentActivity() {

    private val library =
        mutableStateListOf<MediaItem>()

    private val picker =
        registerForActivityResult(
            ActivityResultContracts.OpenMultipleDocuments()
        ) { uris ->

            uris?.forEachIndexed { index, uri ->

                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                }

                library.add(
                    MediaItem(
                        id =
                            System.currentTimeMillis() + index,
                        title =
                            uri.lastPathSegment
                                ?: "Materiał",
                        source =
                            "Lokalny plik",
                        uri =
                            uri.toString()
                    )
                )
            }

            saveLibrary()
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        loadLibrary()

        setContent {

            MaterialTheme {

                MediaHubScreen(
                    items = library,

                    onImport = {
                        picker.launch(
                            arrayOf(
                                "video/*",
                                "audio/*"
                            )
                        )
                    },

                    onAdd = { item ->
                        library.add(item)
                        saveLibrary()
                    },

                    onDelete = { item ->
                        library.removeAll {
                            it.id == item.id
                        }
                        saveLibrary()
                    }
                )
            }
        }
    }

    private fun saveLibrary() {

        val array = JSONArray()

        library.forEach { item ->

            val obj = JSONObject()

            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("source", item.source)

            if (item.uri == null) {
                obj.put("uri", JSONObject.NULL)
            } else {
                obj.put("uri", item.uri)
            }

            if (item.link == null) {
                obj.put("link", JSONObject.NULL)
            } else {
                obj.put("link", item.link)
            }

            array.put(obj)
        }

        getSharedPreferences(
            "mediahub",
            Context.MODE_PRIVATE
        )
            .edit()
            .putString(
                "library",
                array.toString()
            )
            .apply()
    }

    private fun loadLibrary() {

        val saved =
            getSharedPreferences(
                "mediahub",
                Context.MODE_PRIVATE
            )
                .getString(
                    "library",
                    null
                )
                ?: return

        try {

            val array =
                JSONArray(saved)

            for (
                i in 0 until array.length()
            ) {

                val obj =
                    array.getJSONObject(i)

                library.add(
                    MediaItem(

                        id =
                            obj.getLong("id"),

                        title =
                            obj.getString("title"),

                        source =
                            obj.getString("source"),

                        uri =
                            if (
                                obj.isNull("uri")
                            ) {
                                null
                            } else {
                                obj.getString("uri")
                            },

                        link =
                            if (
                                obj.isNull("link")
                            ) {
                                null
                            } else {
                                obj.getString("link")
                            }
                    )
                )
            }

        } catch (_: Exception) {
        }
    }
}

@Composable
fun MediaHubScreen(
    items: List<MediaItem>,
    onImport: () -> Unit,
    onAdd: (MediaItem) -> Unit,
    onDelete: (MediaItem) -> Unit
) {

    val context =
        LocalContext.current

    var search by remember {
        mutableStateOf("")
    }

    var showAddDialog by remember {
        mutableStateOf(false)
    }

    var selected by remember {
        mutableStateOf<MediaItem?>(null)
    }

    val filtered =
        items.filter {
            it.title.contains(
                search,
                ignoreCase = true
            )
        }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Text("MediaHub")
                },

                actions = {

                    TextButton(
                        onClick = {
                            showAddDialog = true
                        }
                    ) {
                        Text("Dodaj link")
                    }
                }
            )
        }

    ) { padding ->

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
        ) {

            Text(
                text = "Twoja biblioteka",
                style =
                    MaterialTheme
                        .typography
                        .headlineMedium
            )

            Spacer(
                Modifier.height(12.dp)
            )

            OutlinedTextField(

                value = search,

                onValueChange = {
                    search = it
                },

                modifier =
                    Modifier.fillMaxWidth(),

                singleLine = true,

                label = {
                    Text("Szukaj")
                }
            )

            Spacer(
                Modifier.height(16.dp)
            )

            Button(
                onClick = onImport,
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text(
                    "Dodaj film lub audio"
                )
            }

            Spacer(
                Modifier.height(16.dp)
            )

            if (filtered.isEmpty()) {

                Text(
                    "Biblioteka jest pusta."
                )

            } else {

                LazyColumn(

                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    items(
                        items = filtered,
                        key = {
                            it.id
                        }
                    ) { item ->

                        MediaCard(
                            item = item,

                            onClick = {
                                selected = item
                            },

                            onDelete = {
                                onDelete(item)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {

        AddLinkDialog(

            onDismiss = {
                showAddDialog = false
            },

            onAdd = {

                onAdd(it)

                showAddDialog = false
            }
        )
    }

    selected?.let { item ->

        if (item.uri != null) {

            openLocalFile(
                context = context,
                uri = item.uri
            )

            selected = null

        } else {

            ExternalDialog(

                item = item,

                onClose = {
                    selected = null
                }
            )
        }
    }
}

@Composable
fun MediaCard(
    item: MediaItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {

    Card(

        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onClick()
                }
    ) {

        Column(
            Modifier.padding(16.dp)
        ) {

            Text(
                text = item.title,
                style =
                    MaterialTheme
                        .typography
                        .titleLarge
            )

            Spacer(
                Modifier.height(6.dp)
            )

            Text(
                text =
                    "Źródło: ${item.source}"
            )

            Spacer(
                Modifier.height(8.dp)
            )

            TextButton(
                onClick = onDelete
            ) {

                Text("Usuń")
            }
        }
    }
}

@Composable
fun AddLinkDialog(
    onDismiss: () -> Unit,
    onAdd: (MediaItem) -> Unit
) {

    var title by remember {
        mutableStateOf("")
    }

    var link by remember {
        mutableStateOf("")
    }

    var source by remember {
        mutableStateOf("Netflix")
    }

    var expanded by remember {
        mutableStateOf(false)
    }

    val sources =
        listOf(
            "Netflix",
            "YouTube",
            "Amazon Prime",
            "Spotify",
            "Disney+",
            "Inne"
        )

    AlertDialog(

        onDismissRequest = onDismiss,

        title = {
            Text("Dodaj materiał")
        },

        text = {

            Column {

                OutlinedTextField(

                    value = title,

                    onValueChange = {
                        title = it
                    },

                    label = {
                        Text("Nazwa")
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                )

                Spacer(
                    Modifier.height(10.dp)
                )

                OutlinedButton(
                    onClick = {
                        expanded = true
                    }
                ) {

                    Text(
                        "Usługa: $source"
                    )
                }

                DropdownMenu(

                    expanded = expanded,

                    onDismissRequest = {
                        expanded = false
                    }
                ) {

                    sources.forEach { option ->

                        DropdownMenuItem(

                            text = {
                                Text(option)
                            },

                            onClick = {

                                source = option

                                expanded = false
                            }
                        )
                    }
                }

                Spacer(
                    Modifier.height(10.dp)
                )

                OutlinedTextField(

                    value = link,

                    onValueChange = {
                        link = it
                    },

                    label = {
                        Text("Link")
                    },

                    modifier =
                        Modifier.fillMaxWidth()
                )
            }
        },

        confirmButton = {

            Button(

                enabled =
                    title.isNotBlank() &&
                    link.isNotBlank(),

                onClick = {

                    onAdd(
                        MediaItem(
                            id =
                                System.currentTimeMillis(),

                            title =
                                title.trim(),

                            source = source,

                            link =
                                link.trim()
                        )
                    )
                }
            ) {

                Text("Dodaj")
            }
        },

        dismissButton = {

            TextButton(
                onClick = onDismiss
            ) {

                Text("Anuluj")
            }
        }
    )
}

@Composable
fun ExternalDialog(
    item: MediaItem,
    onClose: () -> Unit
) {

    val context =
        LocalContext.current

    AlertDialog(

        onDismissRequest = onClose,

        title = {
            Text(item.title)
        },

        text = {

            Text(
                "Źródło: ${item.source}\n\n" +
                    "Wybierz sposób otwarcia."
            )
        },

        confirmButton = {

            Button(

                onClick = {

                    openOnline(
                        context,
                        item.link
                    )

                    onClose()
                }
            ) {

                Text("Jestem online")
            }
        },

        dismissButton = {

            TextButton(

                onClick = {

                    openOfflineApp(
                        context,
                        item.source
                    )

                    onClose()
                }
            ) {

                Text("Jestem offline")
            }
        }
    )
}

fun openLocalFile(
    context: Context,
    uri: String
) {

    try {

        val intent =
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(uri)
            )

        intent.addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )

        context.startActivity(intent)

    } catch (_: Exception) {
    }
}

fun openOnline(
    context: Context,
    link: String?
) {

    if (link.isNullOrBlank()) {
        return
    }

    try {

        val intent =
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(link)
            )

        context.startActivity(intent)

    } catch (_: Exception) {
    }
}

fun openOfflineApp(
    context: Context,
    source: String
) {

    val packageName =
        when (source) {

            "Netflix" ->
                "com.netflix.mediaclient"

            "YouTube" ->
                "com.google.android.youtube"

            "Amazon Prime" ->
                "com.amazon.avod.thirdpartyclient"

            "Spotify" ->
                "com.spotify.music"

            "Disney+" ->
                "com.disney.disneyplus"

            else ->
                null
        }

    if (packageName == null) {
        return
    }

    try {

        val intent =
            context.packageManager
                .getLaunchIntentForPackage(
                    packageName
                )

        if (intent != null) {

            context.startActivity(
                intent
            )
        }

    } catch (_: Exception) {
    }
}
