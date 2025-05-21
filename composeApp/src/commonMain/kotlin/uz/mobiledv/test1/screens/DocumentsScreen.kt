@file:OptIn(ExperimentalMaterial3Api::class)

package uz.mobiledv.test1.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import uz.mobiledv.test1.model.Document
import uz.mobiledv.test1.repository.DocumentRepository

@Composable
fun DocumentsScreen(
    folderId: String,
    onBackClick: () -> Unit,
    onDocumentClick: (Document) -> Unit,
    documentRepository: DocumentRepository
) {
    var documents by remember { mutableStateOf<List<Document>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Document?>(null) }
    var showDeleteDialog by remember { mutableStateOf<Document?>(null) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(folderId) {
        documents = documentRepository.getDocumentsByFolder(folderId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Documents") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Filled.Add, "Add Document")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            items(documents) { document ->
                DocumentItem(
                    document = document,
                    onClick = { onDocumentClick(document) },
                    onEdit = { showEditDialog = document },
                    onDelete = { showDeleteDialog = document }
                )
            }
        }
    }

    if (showAddDialog) {
        DocumentDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, content ->
                scope.launch {
                    documentRepository.createDocument(folderId, name, content)
                    documents = documentRepository.getDocumentsByFolder(folderId)
                    showAddDialog = false
                }
            }
        )
    }

    showEditDialog?.let { document ->
        DocumentDialog(
            document = document,
            onDismiss = { showEditDialog = null },
            onConfirm = { name, content ->
                scope.launch {
                    documentRepository.updateDocument(document.id, name, content)
                    documents = documentRepository.getDocumentsByFolder(folderId)
                    showEditDialog = null
                }
            }
        )
    }

    showDeleteDialog?.let { document ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Document") },
            text = { Text("Are you sure you want to delete document ${document.name}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            documentRepository.deleteDocument(document.id)
                            documents = documentRepository.getDocumentsByFolder(folderId)
                            showDeleteDialog = null
                        }
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DocumentItem(
    document: Document,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Person, contentDescription = null)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = document.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = document.content,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, "Delete")
                }
            }
        }
    }
}

@Composable
private fun DocumentDialog(
    document: Document? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, content: String) -> Unit
) {
    var name by remember { mutableStateOf(document?.name ?: "") }
    var content by remember { mutableStateOf(document?.content ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (document == null) "Add Document" else "Edit Document") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Document Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, content)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
} 