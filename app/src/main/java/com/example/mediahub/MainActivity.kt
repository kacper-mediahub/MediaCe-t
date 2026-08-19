package com.example.mediahub

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONArray
import org.json.JSONObject

data class MediaItemData(
    val id: Long,
    val title: String,
    val type: String,
    val source: String,
    val uri: String? = null,
    val link: String? = null
)

class MainActivity : ComponentActivity() {

    private val imported = mutableStateListOf<MediaItemData>()

    private val picker = registerForActivityResult(
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

            val mime = contentResolver.getType(uri) ?: ""

            val type =
                if (mime.startsWith("audio/")) {
                    "Audio"
                } else {
                    "Film"
                }

            imported.add(
                MediaItemData(
                    id = System.currentTimeMillis() + index,
                    title = uri.lastPathSegment
                        ?.substringAfterLast("/")
                        ?: "Nowy materiał",
                    type = type,
                    source = "Lokalny plik",
                    uri = uri.toString()
                )
            )
        }

        saveLibrary()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadLibrary()

        setContent {
            MediaHubTheme {
                MediaHubApp(
                    items = imported,
                    onImport = {
                        picker.launch(
                            arrayOf("video/*", "audio/*")
                        )
                    },
                    onAddExternal = { item ->
                        imported.add(item)
                        saveLibrary()
                    },
                    onDelete = { item ->
                        imported.removeAll {
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

        imported.forEach { item ->

            val obj = JSONObject()

            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("type", item.type)
            obj.put("source", item.source)
            obj.put("uri", item.uri ?: JSONObject.NULL)
            obj.put("link", item.link ?: JSONObject.NULL)

            array.put(obj)
        }

        getSharedPreferences(
            "mediahub",
            Context.MODE_PRIVATE
        )
            .edit()
            .putString("library", array.toString())
            .apply()
    }

    private fun loadLibrary() {

        val saved = getSharedPreferences(
            "mediahub",
            Context.MODE_PRIVATE
        )
            .getString("library", null)
            ?: return

        try {

            val array = JSONArray(saved)

            for (i in 0 until array.length()) {

                val obj = array.getJSONObject(i)

                imported.add(
                    MediaItemData(
                        id = obj.getLong("id"),
                        title = obj.getString("title"),
                        type = obj.getString("type"),
                        source = obj.getString("source"),
                        uri = if (
                            obj.isNull("uri")
                        ) null
                        else obj.getString("uri"),
                        link = if (
                            obj.isNull("link")
                        ) null
                        else obj.getString("link")
                    )
                )
            }

        } catch (_: Exception) {
        }
    }
}

@Composable
fun MediaHubTheme(
    content: @Composable () -> Unit
) {

    MaterialTheme(
        colorScheme = lightColorScheme(),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaHubApp(
    items: List<MediaItemData>,
    onImport: () -> Unit,
    onAddExternal: (MediaItemData) -> Unit,
    onDelete: (MediaItemData) -> Unit
) {

    var selected by remember {
        mutableStateOf<MediaItemData?>(null)
    }

    var selectedExternal by remember {
        mutableStateOf<MediaItemData?>(null)
    }

    var showAddDialog by remember {
        mutableStateOf(false)
    }

    var search by remember {
        mutableStateOf("")
    }

    if (selected != null) {

        PlayerScreen(
            item = selected!!,
            onBack = {
                selected = null
            }
        )

        return
    }

    val filtered = items.filter {
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

                    IconButton(
                        onClick = {
                            showAddDialog = true
                        }
                    ) {
                        Icon(
                            Icons.Default.AddLink,
                            contentDescription = "Dodaj materiał"
                        )
                    }

                    IconButton(
                        onClick = onImport
                    ) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = "Dodaj pliki"
                        )
                    }
                }
            )
        },

        floatingActionButton = {

            FloatingActionButton(
                onClick = onImport
            ) {

                Icon(
                    Icons.Default.Add,
                    contentDescription = "Dodaj plik"
                )
            }
        }

    ) { padding ->

        Column(

            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(20.dp)
        ) {

            Text(
                "Twoja biblioteka",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(
                Modifier.height(16.dp)
            )

            OutlinedTextField(

                value = search,

                onValueChange = {
                    search = it
                },

                modifier = Modifier.fillMaxWidth(),

                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null
                    )
                },

                placeholder = {
                    Text("Szukaj materiałów...")
                },

                singleLine = true
            )

            Spacer(
                Modifier.height(20.dp)
            )

            if (filtered.isEmpty()) {

                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {

                    Column(
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        Icon(
                            Icons.Default.Movie,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp)
                        )

                        Spacer(
                            Modifier.height(12.dp)
                        )

                        Text(
                            if (items.isEmpty())
                                "Biblioteka jest pusta"
                            else
                                "Nie znaleziono materiału"
                        )

                        Spacer(
                            Modifier.height(8.dp)
                        )

                        Text(
                            "Dodaj plik lub materiał z serwisu."
                        )
                    }
                }

            } else {

                LazyVerticalGrid(

                    columns =
                        GridCells.Adaptive(
                            minSize = 230.dp
                        ),

                    horizontalArrangement =
                        Arrangement.spacedBy(14.dp),

                    verticalArrangement =
                        Arrangement.spacedBy(14.dp)
                ) {

                    items(filtered) { item ->

                        MediaCard(

                            item = item,

                            onClick = {

                                if (
                                    item.source ==
                                    "Lokalny plik"
                                ) {
                                    selected = item
                                } else {
                                    selectedExternal = item
                                }
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

        AddExternalDialog(

            onDismiss = {
                showAddDialog = false
            },

            onAdd = { item ->

                onAddExternal(item)

                showAddDialog = false
            }
        )
    }

    selectedExternal?.let { item ->

        OpenChoiceDialog(

            item = item,

            onClose = {
                selectedExternal = null
            }
        )
    }
}

@Composable
fun MediaCard(
    item: MediaItemData,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {

    var menu by remember {
        mutableStateOf(false)
    }

    ElevatedCard(
        onClick = onClick
    ) {

        Column(
            Modifier.padding(18.dp)
        ) {

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    if (item.type == "Audio")
                        Icons.Default.MusicNote
                    else
                        Icons.Default.Movie,

                    contentDescription = null,

                    modifier =
                        Modifier.size(46.dp)
                )

                Box {

                    IconButton(
                        onClick = {
                            menu = true
                        }
                    ) {

                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Więcej"
                        )
                    }

                    DropdownMenu(
                        expanded = menu,
                        onDismissRequest = {
                            menu = false
                        }
                    ) {

                        DropdownMenuItem(
                            text = {
                                Text("Usuń")
                            },
                            onClick = {
                                menu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            }

            Spacer(
                Modifier.height(12.dp)
            )

            Text(
                item.title,
                style =
                    MaterialTheme.typography.titleMedium
            )

            Spacer(
                Modifier.height(5.dp)
            )

            Text(
                "${item.type} • ${item.source}",
                style =
                    MaterialTheme.typography.bodyMedium
            )

            if (
                item.source != "Lokalny plik"
            ) {

                Spacer(
                    Modifier.height(10.dp)
                )

                Text(
                    "🌐 Materiał zewnętrzny"
                )
            }
        }
    }
}

@Composable
fun AddExternalDialog(
    onDismiss: () -> Unit,
    onAdd: (MediaItemData) -> Unit
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

    val sources = listOf(
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

                Box {

                    OutlinedButton(
                        onClick = {
                            expanded = true
                        },
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Źródło: $source"
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
                        Text("Link do materiału")
                    },
                    placeholder = {
                        Text("https://...")
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
                        MediaItemData(
                            id =
                                System.currentTimeMillis(),
                            title =
                                title.trim(),
                            type = "Film",
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
fun OpenChoiceDialog(
    item: MediaItemData,
    onClose: () -> Unit
) {

    val context =
        LocalContext.current

    AlertDialog(

        onDismissRequest = onClose,

        title = {
            Text(
                "Jak chcesz otworzyć materiał?"
            )
        },

        text = {
            Column {

                Text(
                    item.title,
                    style =
                        MaterialTheme.typography.titleMedium
                )

                Spacer(
                    Modifier.height(5.dp)
                )

                Text(
                    "Źródło: ${item.source}"
                )

                Spacer(
                    Modifier.height(12.dp)
                )

                Text(
                    "Online otworzy zapisany link. " +
                    "Offline uruchomi aplikację źródłową."
                )
            }
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

    } catch (_: ActivityNotFoundException) {
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
                "com.disney.dis
