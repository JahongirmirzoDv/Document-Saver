// File: jahongirmirzodv/test.1.2/Test.1.2-fcc101c924a3dcb58258c4f63c298289470731ad/composeApp/src/commonMain/kotlin/uz/mobiledv/test1/components/DocumentItem.kt
package uz.mobiledv.test1.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import uz.mobiledv.test1.model.Document

@Composable
fun DocumentItem(
    document: Document,
    isManager: Boolean,      // To control visibility of delete button
    onClick: () -> Unit,      // Primary action: view/open (which includes download to cache)
    onDeleteClick: () -> Unit, // For the delete icon button
    onDownloadToPublicClick: () -> Unit // New: For saving to public Downloads folder
) {
    val mimeType = document.mimeType?.lowercase()
    val type = mimeType?.substringBefore('/')

    val fileIcon = when {
        type == "image" -> Icons.Filled.Image
        mimeType == "application/pdf" -> Icons.Filled.PictureAsPdf
        type == "video" -> Icons.Filled.Movie
        type == "audio" -> Icons.Filled.MusicNote
        mimeType?.contains("word") == true -> Icons.Filled.Description // Word document
        mimeType?.contains("excel") == true || mimeType?.contains("spreadsheet") == true -> Icons.AutoMirrored.Filled.Article // Excel
        else -> Icons.AutoMirrored.Filled.Article // Generic icon
    }

    val fileType = when {
        mimeType?.contains("word") == true -> "Word Document"
        mimeType?.contains("excel") == true || mimeType?.contains("spreadsheet") == true -> "Excel Document"
        mimeType == "application/pdf" -> "PDF Document"
        type == "image" -> "Image"
        type == "video" -> "Video"
        type == "audio" -> "Audio"
        else -> {
            // Fallback to generic description if no specific type matches
            "Document"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }, // Card click triggers the main action (open/view)
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f) // Allow text content to take available space
            ) {
                Icon(
                    imageVector = fileIcon,
                    contentDescription = "File type: ${document.mimeType ?: "Unknown"}",
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(
                    modifier = Modifier.weight(1f) // Allow text to shrink/grow but not push actions out
                ) {
                    Text(
                        text = document.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    val details = mutableListOf<String>()
                    fileType.let { details.add(it) }
                    document.createdAt?.let {
                        details.add("Added: ${it.take(10)}")
                    }

                    if (details.isNotEmpty()) {
                        Text(
                            text = details.joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Actions column
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(8.dp)) // Space before action buttons
                IconButton( // New Download Button
                    onClick = onDownloadToPublicClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Filled.Download,
                        contentDescription = "Download to Public Folder",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                if (isManager) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete Document",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}