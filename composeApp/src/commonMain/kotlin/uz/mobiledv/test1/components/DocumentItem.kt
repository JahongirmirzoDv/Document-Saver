// commonMain/kotlin/uz/mobiledv/test1/components/DocumentItem.kt
package uz.mobiledv.test1.components

// ... imports ...
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uz.mobiledv.test1.model.Document

@Composable
fun DocumentItem(
    document: Document,
    isManager: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit // Added for managers
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(document.name, style = MaterialTheme.typography.titleMedium)
                Text("Folder ID: ${document.folderId}", style = MaterialTheme.typography.bodySmall) // Example: show folderId or other metadata
                document.mimeType?.let {
                    Text("Type: $it", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (isManager) {
                IconButton(onClick = onDeleteClick) { // Delete button for manager
                    Icon(Icons.Filled.Delete, contentDescription = "Delete Document", tint = MaterialTheme.colorScheme.error)
                }
            } else {
                // Download icon for viewer (Android). The onClick for the Card will handle the download.
                Icon(Icons.Filled.Download, contentDescription = "Download Document")
            }
        }
    }
}