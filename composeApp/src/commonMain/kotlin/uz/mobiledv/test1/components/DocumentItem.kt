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
import androidx.compose.material.icons.filled.Article // Default document icon
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description // Alternative for generic documents
import androidx.compose.material.icons.filled.Image // For common image types
import androidx.compose.material.icons.filled.Movie // For video
import androidx.compose.material.icons.filled.MusicNote // For audio
import androidx.compose.material.icons.filled.PictureAsPdf // For PDF
// For Word, Excel, etc., you might need to find more specific icons or use a generic one.
// Consider adding the material-icons-extended dependency if not already present for more options.
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import uz.mobiledv.test1.model.Document // Your Document model

@Composable
fun DocumentItem(
    document: Document,
    isManager: Boolean,      // To control visibility of delete button
    onClick: () -> Unit,      // Primary action: view/open (which includes download)
    onDeleteClick: () -> Unit // For the delete icon button
) {
    val mimeType = document.mimeType?.lowercase()
    val type = mimeType?.substringBefore('/')

    val fileIcon = when {
        type == "image" -> Icons.Filled.Image
        mimeType == "application/pdf" -> Icons.Filled.PictureAsPdf
        type == "video" -> Icons.Filled.Movie
        type == "audio" -> Icons.Filled.MusicNote
        mimeType?.contains("word") == true -> Icons.Filled.Description // Word document
        mimeType?.contains("excel") == true || mimeType?.contains("spreadsheet") == true -> Icons.Filled.Article // Excel
        else -> Icons.Filled.Article // Generic icon
    }


    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }, // Card click triggers the main action
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

                    // Secondary information with a more subtle style
                    val details = mutableListOf<String>()
                    document.mimeType?.let { details.add(it) }
                    // If you add fileSize to your Document model:
                    // document.fileSize?.let { details.add(formatFileSize(it)) } // You'd need a formatFileSize helper
                    document.createdAt?.let {
                        // You might want to format this date more nicely
                        details.add("Added: ${it.take(10)}")
                    }

                    if (details.isNotEmpty()) {
                        Text(
                            text = details.joinToString(" • "), // Separator for details
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Actions column (Delete button for manager)
            if (isManager) {
                Spacer(modifier = Modifier.width(8.dp)) // Space before action buttons
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(40.dp) // Consistent touch target size
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

// Optional: Helper function to format file size (if you add it to Document model)
// fun formatFileSize(bytes: Long): String {
//    if (bytes < 1024) return "$bytes B"
//    val kb = bytes / 1024
//    if (kb < 1024) return "$kb KB"
//    val mb = kb / 1024
//    return "$mb MB"
// }