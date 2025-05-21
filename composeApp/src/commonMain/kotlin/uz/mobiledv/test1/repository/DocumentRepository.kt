package uz.mobiledv.test1.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import uz.mobiledv.test1.model.Document
import uz.mobiledv.test1.model.Folder
import uz.mobiledv.test1.model.User
import kotlin.random.Random

interface DocumentRepository {
    suspend fun login(phoneNumber: String, password: String): Result<User>
    suspend fun createUser(user: User): Result<Unit>
    suspend fun deleteUser(phoneNumber: String): Result<Unit>
    suspend fun updateUser(user: User): Result<Unit>
    fun getUsers(): Flow<List<User>>
    
    suspend fun createFolder(folder: Folder): Result<Unit>
    suspend fun updateFolder(folder: Folder): Result<Unit>
    suspend fun deleteFolder(folderId: String): Result<Unit>
    fun getFolders(): Flow<List<Folder>>
    
    suspend fun uploadDocument(document: Document, fileBytes: ByteArray): Result<Unit>
    suspend fun updateDocument(document: Document): Result<Unit>
    fun getDocuments(folderId: String): Flow<List<Document>>
    suspend fun downloadDocument(documentId: String): Result<ByteArray>

    suspend fun getDocumentsByFolder(folderId: String): List<Document>
    suspend fun createDocument(folderId: String, name: String, content: String)
    suspend fun updateDocument(id: String, name: String, content: String)
    suspend fun deleteDocument(id: String)
    suspend fun getDocumentById(id: String): Document?
}

class DocumentRepositoryImpl : DocumentRepository {
    private val documents = MutableStateFlow<List<Document>>(emptyList())
    private val mutex = Mutex()

    override suspend fun login(phoneNumber: String, password: String): Result<User> {
        TODO("Not yet implemented")
    }

    override suspend fun createUser(user: User): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteUser(phoneNumber: String): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun updateUser(user: User): Result<Unit> {
        TODO("Not yet implemented")
    }

    override fun getUsers(): Flow<List<User>> {
        TODO("Not yet implemented")
    }

    override suspend fun createFolder(folder: Folder): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun updateFolder(folder: Folder): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteFolder(folderId: String): Result<Unit> {
        TODO("Not yet implemented")
    }

    override fun getFolders(): Flow<List<Folder>> {
        TODO("Not yet implemented")
    }

    override suspend fun uploadDocument(document: Document, fileBytes: ByteArray): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun updateDocument(document: Document): Result<Unit> {
        TODO("Not yet implemented")
    }

    override fun getDocuments(folderId: String): Flow<List<Document>> {
        TODO("Not yet implemented")
    }

    override suspend fun downloadDocument(documentId: String): Result<ByteArray> {
        TODO("Not yet implemented")
    }

    override suspend fun getDocumentsByFolder(folderId: String): List<Document> = mutex.withLock {
        documents.value.filter { it.folderId == folderId }
    }
    
    override suspend fun createDocument(folderId: String, name: String, content: String) = mutex.withLock {
        val newDocument = Document(
            id = Random.nextInt().toString(),
            folderId = folderId,
            name = name,
            content = content,
            fileUrl = ""
        )
        documents.value = documents.value + newDocument
    }
    
    override suspend fun updateDocument(id: String, name: String, content: String) = mutex.withLock {
        documents.value = documents.value.map { document ->
            if (document.id == id) {
                document.copy(
                    name = name,
                    content = content
                )
            } else document
        }
    }
    
    override suspend fun deleteDocument(id: String) = mutex.withLock {
        documents.value = documents.value.filter { it.id != id }
    }
    
    override suspend fun getDocumentById(id: String): Document? = mutex.withLock {
        documents.value.find { it.id == id }
    }
} 