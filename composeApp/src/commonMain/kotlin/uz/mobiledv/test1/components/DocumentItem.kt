// commonMain/kotlin/uz/mobiledv/test1/components/DocumentItem.kt
package uz.mobiledv.test1.components

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
    isManager: Boolean, // To control visibility of delete button
    onClick: () -> Unit, // General click on the card
    onDeleteClick: () -> Unit, // For the delete icon button
    onDownloadFile: () -> Unit // For the download icon button
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp) // Added horizontal padding
            .clickable { onClick() } // Card click can trigger download or view based on role
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(document.name, style = MaterialTheme.typography.titleMedium)
                // document.userId?.let {
                //    Text("Uploader: ${it.take(8)}...", style = MaterialTheme.typography.bodySmall)
                // }
                document.mimeType?.let {
                    Text("Type: $it", style = MaterialTheme.typography.bodySmall)
                }
                 document.createdAt?.let {
                    Text("Created: $it", style = MaterialTheme.typography.labelSmall) // Format date appropriately
                 }
            }
            Spacer(modifier = Modifier.width(8.dp))

            // Download button is available for all users
            IconButton(onClick = {
                onDownloadFile()
            }) {
                Icon(Icons.Filled.Download, contentDescription = "Download Document")
            }

            if (isManager) { // Delete button only for managers
                IconButton(onClick = onDeleteClick) {
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