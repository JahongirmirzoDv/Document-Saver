package uz.mobiledv.test1.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uz.mobiledv.test1.model.Folder

@Composable
fun FolderItem(
    folder: Folder,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(folder.name, style = MaterialTheme.typography.titleMedium)
            if (folder.description.isNotEmpty()) {
                Text(folder.description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
} 