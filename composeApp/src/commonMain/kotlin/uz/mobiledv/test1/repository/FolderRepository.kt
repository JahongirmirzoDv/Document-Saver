package uz.mobiledv.test1.repository

import io.appwrite.services.Databases
import uz.mobiledv.test1.appwrite.APPWRITE_DATABASE_ID
import uz.mobiledv.test1.appwrite.APPWRITE_FOLDERS_COLLECTION_ID
import uz.mobiledv.test1.model.AppwriteFolderList
import uz.mobiledv.test1.model.Folder
import io.appwrite.ID

interface FolderRepository {
    suspend fun getAllFolders(): Result<List<Folder>> // Changed to Result for consistency
    suspend fun getFolderById(id: String): Result<Folder?>
    suspend fun createFolder(name: String, createdBy: String, description: String): Result<Folder> // Modified to return created folder
    suspend fun updateFolder(id: String, name: String, description: String): Result<Folder>
    suspend fun deleteFolder(id: String): Result<Unit>
}

class FolderRepositoryImpl(
    private val databases: Databases // Injected via Koin
) : FolderRepository {

    override suspend fun getAllFolders(): Result<List<Folder>> = runCatching {
        val response = databases.listDocuments(
            databaseId = APPWRITE_DATABASE_ID,
            collectionId = APPWRITE_FOLDERS_COLLECTION_ID
        )

        response.convertTo(AppwriteFolderList::class.java)?.documents ?: emptyList()
        // If not using convertTo or it's not KMP friendly for some Appwrite SDK versions:
        // response.documents.map { it.data.toDomainModel() } // Manual mapping
    }

    override suspend fun getFolderById(id: String): Result<Folder?> = runCatching {
        val response = databases.getDocument(
            databaseId = APPWRITE_DATABASE_ID,
            collectionId = APPWRITE_FOLDERS_COLLECTION_ID,
            documentId = id
        )
        response.convertTo(Folder::class.java)
    }

    override suspend fun createFolder(name: String, createdBy: String, description: String): Result<Folder> = runCatching {
        val folderData = Folder(
            name = name,
            createdBy = createdBy,
            description = description
        )
        val response = databases.createDocument(
            databaseId = APPWRITE_DATABASE_ID,
            collectionId = APPWRITE_FOLDERS_COLLECTION_ID,
            documentId = ID.unique(), // Appwrite generates unique ID
            data = folderData.toAppwriteCreateData(),
            permissions = null // Optional: Define permissions here e.g., listOf(Permission.read(Role.any()))
        )
        response.convertTo(Folder::class.java)!! // Should not be null if creation succeeds
    }

    override suspend fun updateFolder(id: String, name: String, description: String): Result<Folder> = runCatching {
        val dataToUpdate = mapOf(
            "name" to name,
            "description" to description
        ).filterValues { it != null }

        val response = databases.updateDocument(
            databaseId = APPWRITE_DATABASE_ID,
            collectionId = APPWRITE_FOLDERS_COLLECTION_ID,
            documentId = id,
            data = dataToUpdate
        )
        response.convertTo(Folder::class.java)!!
    }

    override suspend fun deleteFolder(id: String): Result<Unit> = runCatching {
        databases.deleteDocument(
            databaseId = APPWRITE_DATABASE_ID,
            collectionId = APPWRITE_FOLDERS_COLLECTION_ID,
            documentId = id
        )
        Unit
    }
}