package uz.mobiledv.test1.repository

import com.google.firebase.database.ServerValue
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.storage.FirebaseStorage
import dev.gitlive.firebase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose // Required for awaitClose
import uz.mobiledv.test1.model.Document
import uz.mobiledv.test1.model.Folder
import uz.mobiledv.test1.model.User
import kotlinx.serialization.Serializable // Ensure this import is present if using @Serializable
import dev.gitlive.firebase.firestore.Filter // Import for Filter

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

class DocumentRepositoryImpl() : DocumentRepository {
    private val db: FirebaseFirestore by lazy { Firebase.firestore }
    private val storage: FirebaseStorage by lazy { Firebase.storage }

    // USERS
    override suspend fun login(phoneNumber: String, password: String): Result<User> = runCatching {
        val userQuerySnapshot = db.collection("users")
            .where{
                "phoneNumber" equalTo phoneNumber
            }
            .get()

        if (userQuerySnapshot.documents.isEmpty()) throw Exception("User not found with phone number: $phoneNumber")

        val userSnapshot = userQuerySnapshot.documents.first()
        val user = try {
            userSnapshot.data<User>()
        } catch (e: Exception) {
            throw Exception("Failed to parse user data for document ID ${userSnapshot.id}: ${e.message}", e)
        }

        if (user.password != password) throw Exception("Invalid password")
        user
    }

    override suspend fun createUser(user: User): Result<Unit> = runCatching {
        if (user.username.isBlank()) throw IllegalArgumentException("User phone number cannot be blank if used as ID.")
        db.collection("users").document(user.username).set(user)
    }

    override suspend fun deleteUser(phoneNumber: String): Result<Unit> = runCatching {
        if (phoneNumber.isBlank()) throw IllegalArgumentException("Phone number cannot be blank for deletion.")
        db.collection("users").document(phoneNumber).delete()
    }

    override suspend fun updateUser(user: User): Result<Unit> = runCatching {
        // Corrected to use phoneNumber, consistent with User model and other functions
        if (user.username.isBlank()) throw IllegalArgumentException("User phone number cannot be blank for update.")
        db.collection("users").document(user.username).set(user, merge = true)
    }

    override fun getUsers(): Flow<List<User>> = callbackFlow {
        val snapshot = db.collection("users")
            .get()

        if (snapshot.documents.isEmpty()) throw Exception("User not found")

        val userSnapshot = snapshot.documents.first()
        val users = try {
            userSnapshot.data<User>()
        } catch (e: Exception) {
            throw Exception("Failed to parse user data for document ID ${userSnapshot.id}: ${e.message}", e)
        }
        users
    }

    // FOLDERS
    override suspend fun createFolder(folder: Folder): Result<Unit> = runCatching {
        if (folder.id.isBlank()) {
            val newFolderRef = db.collection("folders").document
            val folderWithId = folder.copy(id = newFolderRef.id)
            newFolderRef.set(folderWithId)
        } else {
            db.collection("folders").document(folder.id).set(folder)
        }
    }

    override suspend fun updateFolder(folder: Folder): Result<Unit> = runCatching {
        if (folder.id.isBlank()) throw IllegalArgumentException("Folder ID cannot be blank for update.")
        db.collection("folders").document(folder.id).set(folder, merge = true)
    }

    override suspend fun deleteFolder(folderId: String): Result<Unit> = runCatching {
        if (folderId.isBlank()) throw IllegalArgumentException("Folder ID cannot be blank for deletion.")
        db.collection("folders").document(folderId).delete()
    }

    override fun getFolders(): Flow<List<Folder>> = callbackFlow {
        val listenerRegistration = db.collection("folders").addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                System.err.println("Error listening to folder snapshots: ${exception.message}")
                close(exception)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val folders = snapshot.documents.mapNotNull { docSnapshot ->
                    try {
                        docSnapshot.data<Folder>()
                    } catch (e: Exception) {
                        System.err.println("Failed to parse Folder document ${docSnapshot.id}: ${e.message}")
                        null
                    }
                }
                trySend(folders).isSuccess
            }
        }
        awaitClose {
            System.out.println("Closing folder listener.")
            listenerRegistration.remove()
        }
    }

    // DOCUMENTS
    override suspend fun uploadDocument(document: Document, fileBytes: ByteArray): Result<String?> = runCatching {
        if (document.id.isBlank()) throw IllegalArgumentException("Document ID cannot be blank for upload.")
        if (document.folderId.isBlank()) throw IllegalArgumentException("Document folderId cannot be blank.")
        // Corrected to check document.fileName, consistent with Document model for storage
        if (document.fileName.isBlank()) throw IllegalArgumentException("Document fileName cannot be blank for storage upload.")

        // Corrected to use document.fileName in the storage path
        val ref = storage.reference("documents/${document.folderId}/${document.fileName}")
        ref.putBytes(fileBytes)
        val downloadUrl = ref.getDownloadUrl()

        val documentWithUrl = document.copy(url = downloadUrl)
        db.collection("documents").document(document.id).set(documentWithUrl)
        downloadUrl
    }

    override suspend fun updateDocumentMetadata(document: Document): Result<Unit> = runCatching {
        if (document.id.isBlank()) throw IllegalArgumentException("Document ID cannot be blank for metadata update.")
        db.collection("documents").document(document.id).set(document, merge = true)
    }

    override fun getDocuments(folderId: String): Flow<List<Document>> = callbackFlow {
        if (folderId.isBlank()) {
            System.err.println("Folder ID is blank for getDocuments, closing flow.")
            close(IllegalArgumentException("Folder ID cannot be blank."))
            return@callbackFlow
        }
        val listenerRegistration = db.collection("documents")
            .where(Filter.equalTo("folderId", folderId))
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    System.err.println("Error listening to document snapshots for folder $folderId: ${exception.message}")
                    close(exception)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val documents = snapshot.documents.mapNotNull { docSnapshot ->
                        try {
                            docSnapshot.data<Document>()
                        } catch (e: Exception) {
                            System.err.println("Failed to parse Document ${docSnapshot.id} in folder $folderId: ${e.message}")
                            null
                        }
                    }
                    trySend(documents).isSuccess
                }
            }
        awaitClose {
            System.out.println("Closing document listener for folder $folderId.")
            listenerRegistration.remove()
        }
    }

    override suspend fun downloadDocument(documentId: String): Result<ByteArray> = runCatching {
        if (documentId.isBlank()) throw IllegalArgumentException("Document ID cannot be blank for download.")
        val docSnapshot = db.collection("documents").document(documentId).get()
        val document = docSnapshot.data<Document?>()
            ?: throw Exception("Document not found in Firestore with ID: $documentId")

        if (document.folderId.isBlank() || document.fileName.isBlank()) {
            throw Exception("Document metadata (folderId or fileName) is missing for storage path construction. Doc ID: $documentId")
        }

        val ref = storage.reference("documents/${document.folderId}/${document.fileName}")
        ref.getBytes(10 * 1024 * 1024) // 10MB max
    }

    override suspend fun getDocumentsByFolder(folderId: String): Result<List<Document>> = runCatching {
        if (folderId.isBlank()) throw IllegalArgumentException("Folder ID cannot be blank.")
        val snapshot = db.collection("documents")
            .where(Filter.equalTo("folderId", folderId))
            .get()
        snapshot.documents.mapNotNull { docSnapshot ->
            try {
                docSnapshot.data<Document>()
            } catch (e: Exception) {
                System.err.println("Failed to parse Document ${docSnapshot.id} in getDocumentsByFolder $folderId: ${e.message}")
                null
            }
        }
    }

    override suspend fun createDocument(document: Document): Result<Unit> = runCatching {
        if (document.id.isBlank()) {
            val newDocRef = db.collection("documents").document()
            val documentWithId = document.copy(id = newDocRef.id)
            newDocRef.set(documentWithId)
        } else {
            db.collection("documents").document(document.id).set(document)
        }
    }

    override suspend fun updateDocumentContent(id: String, name: String, content: String): Result<Unit> = runCatching {
        if (id.isBlank()) throw IllegalArgumentException("Document ID cannot be blank for content update.")
        val docRef = db.collection("documents").document(id)
        val updates = mapOf(
            "name" to name, // This updates the display name of the document
            "content" to content,
            "lastModified" to Firebase.firestore.serverTimestamp()
        )
        docRef.update(updates)
    }

    override suspend fun deleteDocument(documentId: String): Result<Unit> = runCatching {
        if (documentId.isBlank()) throw IllegalArgumentException("Document ID cannot be blank for deletion.")

        val docSnapshot = db.collection("documents").document(documentId).get()
        val document = docSnapshot.data<Document?>()

        db.collection("documents").document(documentId).delete()

        if (document != null) {
            if (document.folderId.isNotBlank() && document.fileName.isNotBlank()) {
                val storageRef = storage.reference("documents/${document.folderId}/${document.fileName}")
                try {
                    storageRef.delete()
                    System.out.println("Successfully deleted ${document.fileName} from storage.")
                } catch (e: Exception) {
                    System.err.println("Failed to delete ${document.fileName} from storage (may not exist or other issue): ${e.message}")
                }
            } else {
                System.err.println("Skipping storage deletion for doc ID $documentId as folderId or fileName is blank in metadata.")
            }
        } else {
            System.err.println("Document metadata not found for doc ID $documentId, cannot determine storage path for deletion. Firestore entry deleted.")
        }
    }

    override suspend fun getDocumentById(id: String): Result<Document?> = runCatching {
        if (id.isBlank()) throw IllegalArgumentException("Document ID cannot be blank.")
        val docSnapshot = db.collection("documents").document(id).get()
        try {
            docSnapshot.data<Document?>()
        } catch (e: Exception) {
            System.err.println("Failed to parse Document for getDocumentById $id: ${e.message}")
            null
        }
    }
}