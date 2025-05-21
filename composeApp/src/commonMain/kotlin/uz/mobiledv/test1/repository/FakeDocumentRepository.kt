package uz.mobiledv.test1.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import uz.mobiledv.test1.model.Document
import uz.mobiledv.test1.model.Folder
import uz.mobiledv.test1.model.User

class FakeDocumentRepository : DocumentRepository {
    private val users = listOf(User("1234567890", "test", "test"))
    private val folders =
        listOf(Folder("1", "Sample Folder", "1234567890", 0L, "Sample description"))
    private val documents = mutableListOf<Document>(
        Document("1", "Sample.pdf", "url", "Sample file", "Sample content")
    )

    override suspend fun login(phoneNumber: String, password: String): Result<User> {
        val user = users.find { it.username == phoneNumber && it.password == password }
        return if (user != null) Result.success(user) else Result.failure(Exception("Invalid credentials"))
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

    override fun getFolders(): Flow<List<Folder>> = flowOf(folders)
    override suspend fun uploadDocument(
        document: Document,
        fileBytes: ByteArray
    ): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun updateDocument(document: Document): Result<Unit> {
        TODO("Not yet implemented")
    }

    override fun getDocuments(folderId: String): Flow<List<Document>> =
        flowOf(documents.filter { it.folderId == folderId })

    override suspend fun downloadDocument(documentId: String): Result<ByteArray> {
        TODO("Not yet implemented")
    }

    override suspend fun getDocumentsByFolder(folderId: String): List<Document> =
        documents.filter { it.folderId == folderId }

    override suspend fun createDocument(folderId: String, name: String, content: String) {
        documents.add(
            Document(
                (documents.size + 1).toString(),
                folderId,
                fileUrl = "",
                name,
                content
            )
        )
    }

    override suspend fun updateDocument(id: String, name: String, content: String) {
        documents.replaceAll { doc ->
            if (doc.id == id) doc.copy(name = name, content = content) else doc
        }
    }

    override suspend fun deleteDocument(id: String) {
        documents.removeAll { it.id == id }
    }

    override suspend fun getDocumentById(id: String): Document? =
        documents.find { it.id == id }
} 