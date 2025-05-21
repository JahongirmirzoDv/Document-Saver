package uz.mobiledv.test1.repository

import uz.mobiledv.test1.model.Folder

interface FolderRepository {
    suspend fun getAllFolders(): List<Folder>
    suspend fun getFolderById(id: String): Folder?
    suspend fun createFolder(name: String)
    suspend fun updateFolder(id: String, name: String)
    suspend fun deleteFolder(id: String)
}

class FolderRepositoryImpl : FolderRepository {
    private val folders = mutableListOf<Folder>()

    override suspend fun getAllFolders(): List<Folder> = folders

    override suspend fun createFolder(name: String) {
        folders.add(Folder((folders.size + 1).toString(), name = name, createdBy = "user"))
    }

    override suspend fun updateFolder(id: String, name: String) {
        folders.replaceAll { folder ->
            if (folder.id == id) folder.copy(name = name) else folder
        }
    }

    override suspend fun deleteFolder(id: String) {
        folders.removeAll { it.id == id }
    }

    override suspend fun getFolderById(id: String): Folder? =
        folders.find { it.id == id }
} 