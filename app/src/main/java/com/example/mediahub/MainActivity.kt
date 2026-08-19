package com.example.mediahub

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

data class MediaItem(
    val id: Long,
    val name: String,
    val uri: Uri,
    val source: String
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                MediaHubApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaHubApp() {

    val context = androidx.compose.ui.platform.LocalContext.current

    val mediaItems = remember {
        mutableStateListOf<MediaItem>()
    }

    var selectedSource by remember {
        mutableStateOf("Netflix")
    }

    var showAddDialog by remember {
        mutableStateOf(false)
    }

    var selectedItem by remember {
        mutableStateOf<MediaItem?>(null)
    }

    val filePicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenMultipleDocuments()
        ) { uris ->

            uris.forEach { uri ->

                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                    // Nie wszystkie urządzenia pozwalają na persistable permission.
                }

                val fileName =
                    getFileName(context, uri) ?: "Nieznany plik"

                mediaItems.add(
                    MediaItem(
                        id = System.currentTimeMillis() + mediaItems.size,
                        name = fileName,
                        uri = uri,
                        source = selectedSource
                    )
                )
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "MediaHub",
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Twoje multimedia",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {

            FloatingActionButton(
                onClick = {
                    showAddDialog = true
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Dodaj"
                )
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Biblioteka",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${mediaItems.size} materiałów",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (mediaItems.isEmpty()) {

                EmptyLibrary(
                    onAdd = {
                        showAddDialog = true
                    }
                )

            } else {

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {

                    items(
                        items = mediaItems,
                        key = { it.id }
                    ) { item ->

                        MediaCard(
                            item = item,
                            onClick = {
                                selectedItem = item
                            },
                            onDelete = {
                                mediaItems.remove(item)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {

        AddMediaDialog(
            selectedSource = selectedSource,
            onSourceChange = {
                selectedSource = it
            },
            onDismiss = {
                showAddDialog = false
            },
            onChooseFiles = {

                showAddDialog = false

                filePicker.launch(
                    arrayOf(
                        "video/*",
                        "audio/*"
                    )
                )
            }
        )
    }

    selectedItem?.let { item ->

        MediaActionDialog(
            item = item,
            onDismiss = {
                selectedItem = null
            },
            onOnline = {

                selectedItem = null

                openOnlineMaterial(
                    context = context,
                    item = item
                )
            },
            onOffline = {

                selectedItem = null

                openOfflineApp(
                    context = context,
                    source = item.source
                )
            },
            onLocal = {

                selectedItem = null

                openLocalFile(
                    context = context,
                    uri = item.uri
                )
            }
        )
    }
}

@Composable
fun EmptyLibrary(
    onAdd: () -> Unit
) {

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Icon(
                imageVector = Icons.Default.VideoLibrary,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Biblioteka jest pusta",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Dodaj swoje pliki audio lub wideo.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onAdd
            ) {

                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text("Dodaj materiał")
            }
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector =
                        if (isAudioFile(item.name))
                            Icons.Default.MusicNote
                        else
                            Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = item.name,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.source,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(
                onClick = onDelete
            ) {

                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Usuń"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMediaDialog(
    selectedSource: String,
    onSourceChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onChooseFiles: () -> Unit
) {

    val sources = listOf(
        "Netflix",
        "YouTube",
        "Amazon Prime",
        "Spotify",
        "Disney+",
        "Inne"
    )

    var expanded by remember {
        mutableStateOf(false)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Dodaj materiał")
        },
        text = {

            Column {

                Text(
                    text = "Wybierz usługę, z której pochodzi materiał:"
                )

                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = {
                        expanded = !expanded
                    }
                ) {

                    OutlinedTextField(
                        value = selectedSource,
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text("Źródło")
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = expanded
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = {
                            expanded = false
                        }
                    ) {

                        sources.forEach { source ->

                            DropdownMenuItem(
                                text = {
                                    Text(source)
                                },
                                onClick = {

                                    onSourceChange(source)

                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Następnie wybierz pliki audio lub wideo znajdujące się na tablecie.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {

            Button(
                onClick = onChooseFiles
            ) {

                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text("Wybierz pliki")
            }
        },
        dismissButton = {

            OutlinedButton(
                onClick = onDismiss
            ) {
                Text("Anuluj")
            }
        }
    )
}

@Composable
fun MediaActionDialog(
    item: MediaItem,
    onDismiss: () -> Unit,
    onOnline: () -> Unit,
    onOffline: () -> Unit,
    onLocal: () -> Unit
) {

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text(
                text = item.name,
                maxLines = 2
            )
        },

        text = {

            Column {

                Text(
                    text = "Źródło: ${item.source}"
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Jak chcesz otworzyć materiał?"
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onLocal,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Odtwórz plik lokalnie")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onOnline,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Jestem online")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onOffline,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Jestem offline")
                }
            }
        },

        confirmButton = {},

        dismissButton = {
            Text(
                text = "Zamknij",
                modifier = Modifier
                    .padding(8.dp)
                    .clickable {
                        onDismiss()
                    }
            )
        }
    )
}

fun openLocalFile(
    context: Context,
    uri: Uri
) {

    val intent = Intent(Intent.ACTION_VIEW).apply {

        setDataAndType(
            uri,
            context.contentResolver.getType(uri) ?: "*/*"
        )

        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    try {

        context.startActivity(intent)

    } catch (_: Exception) {
        // Brak aplikacji mogącej odtworzyć plik.
    }
}

fun openOnlineMaterial(
    context: Context,
    item: MediaItem
) {

    /*
     * V1:
     * Jeżeli użytkownik ma lokalny plik, otwieramy go w systemowym
     * odtwarzaczu. Później możemy dodać osobne pole na URL materiału.
     *
     * Dla usług takich jak Netflix/Prime/Spotify nie można
     * zamienić lokalnego pobranego pliku w publiczny URL.
     */

    val packageName = getPackageNameForSource(item.source)

    if (packageName != null) {

        openApplication(
            context,
            packageName
        )
    } else {

        openLocalFile(
            context,
            item.uri
        )
    }
}

fun openOfflineApp(
    context: Context,
    source: String
) {

    val packageName = getPackageNameForSource(source)

    if (packageName != null) {

        openApplication(
            context,
            packageName
        )
    } else {

        openLocalFileFromLastItem(
            context
        )
    }
}

fun openApplication(
    context: Context,
    packageName: String
) {

    try {

        val launchIntent =
            context.packageManager.getLaunchIntentForPackage(
                packageName
            )

        if (launchIntent != null) {

            context.startActivity(launchIntent)

        } else {

            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(
                    "market://details?id=$packageName"
                )
            )

            context.startActivity(intent)
        }

    } catch (_: Exception) {
        // Aplikacja nie jest zainstalowana.
    }
}

fun openLocalFileFromLastItem(
    context: Context
) {
    // Rezerwowane dla kolejnej wersji.
}

fun getPackageNameForSource(
    source: String
): String? {

    return when (source) {

        "Netflix" ->
            "com.netflix.mediaclient"

        "YouTube" ->
            "com.google.android.youtube"

        "Spotify" ->
            "com.spotify.music"

        "Amazon Prime" ->
            "com.amazon.avod.thirdpartyclient"

        "Disney+" ->
            "com.disney.disneyplus"

        else ->
            null
    }
}

fun getFileName(
    context: Context,
    uri: Uri
): String? {

    var name: String? = null

    context.contentResolver.query(
        uri,
        arrayOf(
            android.provider.OpenableColumns.DISPLAY_NAME
        ),
        null,
        null,
        null
    )?.use { cursor ->

        if (cursor.moveToFirst()) {

            val index =
                cursor.getColumnIndex(
                    android.provider.OpenableColumns.DISPLAY_NAME
                )

            if (index >= 0) {
                name = cursor.getString(index)
            }
        }
    }

    return name
}

fun isAudioFile(
    name: String
): Boolean {

    val lower = name.lowercase()

    return lower.endsWith(".mp3") ||
            lower.endsWith(".m4a") ||
            lower.endsWith(".aac") ||
            lower.endsWith(".flac") ||
            lower.endsWith(".wav") ||
            lower.endsWith(".ogg")
}
