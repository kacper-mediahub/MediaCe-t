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
import androidx.media3.common.MediaItem
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

    private val library = mutableStateListOf<MediaItemData>()

    private val picker =
        registerForActivityResult(
            ActivityResultContracts.OpenMultipleDocuments()
        ) { uris ->

            uris?.forEach { uri ->

                val mime =
                    contentResolver.getType(uri) ?: ""

                val type =
                    if (mime.startsWith("audio/"))
                        "Audio"
                    else
                        "Film"

                library.add(
                    MediaItemData(
                        id = System.currentTimeMillis(),
                        title =
                            uri.lastPathSegment
                                ?: "Nowy materiał",
                        type = type,
                        source = "Lokalny plik",
                        uri = uri.toString()
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

                MediaHubApp(
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
                        library.remove(item)
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
            obj.put("type", item.type)
            obj.put("source", item.source)
            obj.put(
                "uri",
                item.uri ?: JSONObject.NULL
            )
            obj.put(
                "link",
                item.link ?: JSONObject.NULL
            )

            array.put(obj)
        }

        getSharedPreferences(
            "mediahub",
            MODE_PRIVATE
        )
            .edit()
            .putString(
                "library",
                array.toString()
            )
            .apply()
    }

    private fun loadLibrary() {

        val text =
            getSharedPreferences(
                "mediahub",
                MODE_PRIVATE
            )
                .getString(
                    "library",
                    null
                )
                ?: return

        try {

            val array = JSONArray(text)

            for (i in 0 until array.length()) {

                val obj =
                    array.getJSONObject(i)

                library.add(
                    MediaItemData(
                        id =
                            obj.getLong("id"),

                        title =
                            obj.getString("title"),

                        type =
                            obj.getString("type"),

                        source =
                            obj.getString("source"),

                        uri =
                            if (obj.isNull("uri"))
                                null
                            else
                                obj.getString("uri"),

                        link =
                            if (obj.isNull("link"))
                                null
                            else
                                obj.getString("link")
                    )
                )
            }

        } catch (_: Exception) {
        }
    }
}

@Composable
fun MediaHubApp(
    items: List<MediaItemData>,
    onImport: () -> Unit,
    onAdd: (MediaItemData) -> Unit,
    onDelete: (MediaItemData) -> Unit
) {

    var search by remember {
        mutableStateOf("")
    }

    var selected by remember {
        mutableStateOf<MediaItemData?>(null)
    }

    var external by remember {
        mutableStateOf<MediaItemData?>(null)
    }

    var addDialog by remember {
        mutableStateOf(false)
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

                    IconButton(
                        onClick = {
                            addDialog = true
                        }
                    ) {

                        Icon(
                            Icons.Default.AddLink,
                            contentDescription =
                                "Dodaj link"
                        )
                    }

                    IconButton(
                        onClick = onImport
                    ) {

                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription =
                                "Dodaj pliki"
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
                    contentDescription =
                        "Dodaj plik"
                )
            }
        }

    ) { padding ->

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp)
        ) {

            Text(
                "Twoja biblioteka",
                style =
                    MaterialTheme.typography.headlineMedium
            )

            Spacer(
                Modifier.height(16.dp)
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
                },

                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null
                    )
                }
            )

            Spacer(
                Modifier.height(20.dp)
            )

            if (filtered.isEmpty()) {

                Box(
                    modifier =
                        Modifier.fillMaxSize(),

                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        "Biblioteka jest pusta"
                    )
                }

            } else {

                LazyVerticalGrid(

                    columns =
                        GridCells.Adaptive(
                            minSize = 220.dp
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

                                    external = item
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

    if (addDialog) {

        AddMediaDialog(

            onDismiss = {
                addDialog = false
            },

            onAdd = {

                onAdd(it)

                addDialog = false
            }
        )
    }

    external?.let { item ->

        OpenChoiceDialog(

            item = item,

            onClose = {
                external = null
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
                modifier =
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
                        Modifier.size(42.dp)
                )

                Box {

                    IconButton(
                        onClick = {
                            menu = true
                        }
                    ) {

                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription =
                                "Menu"
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
                                    contentDescription =
                                        null
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
                "${item.type} • ${item.source}"
            )
        }
    }
}

@Composable
fun AddMediaDialog(
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

                    modifier =
                        Modifier.fillMaxWidth(),

                    label = {
                        Text("Nazwa")
                    }
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

                    modifier =
                        Modifier.fillMaxWidth(),

                    label = {
                        Text("Link")
                    }
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

            Text(
                "${item.title}\n\nŹródło: ${item.source}"
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
            context.startActivity(intent)
        }

    } catch (_: Exception) {
    }
}

@Composable
fun PlayerScreen(
    item: MediaItemData,
    onBack: () -> Unit
) {

    val context =
        LocalContext.current

    val player =
        remember {

            ExoPlayer
                .Builder(context)
                .build()
                .apply {

                    setMediaItem(
                        MediaItem.fromUri(
                            Uri.parse(
                                item.uri
                            )
                        )
                    )

                    prepare()

                    playWhenReady = true
                }
        }

    DisposableEffect(Unit) {

        onDispose {
            player.release()
        }
    }

    Column(
        Modifier.fillMaxSize()
    ) {

        Row(

            Modifier
                .fillMaxWidth()
                .padding(12.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBack
            ) {

                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription =
                        "Wstecz"
                )
            }

            Text(
                item.title,
                style =
                    MaterialTheme.typography.titleLarge
            )
        }

        AndroidView(

            factory = {
                PlayerView(it).apply {
                    player = player
                }
            },

            modifier =
                Modifier.fillMaxSize()
        )
    }
}
