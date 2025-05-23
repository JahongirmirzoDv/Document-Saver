package uz.mobiledv.test1.repository

import io.appwrite.Client
import io.appwrite.ID
import io.appwrite.exceptions.AppwriteException
import io.appwrite.models.InputFile
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Storage
import io.appwrite.services.Users
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.formData
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import uz.mobiledv.test1.appwrite.APPWRITE_DATABASE_ID
import uz.mobiledv.test1.appwrite.APPWRITE_DOCUMENTS_COLLECTION_ID
import uz.mobiledv.test1.appwrite.APPWRITE_FILES_BUCKET_ID
import uz.mobiledv.test1.appwrite.APPWRITE_FOLDERS_COLLECTION_ID
import uz.mobiledv.test1.model.Document
import uz.mobiledv.test1.model.Folder
import uz.mobiledv.test1.model.User
import uz.mobiledv.test1.model.UserPrefs
import io.appwrite.Query
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.http.ContentType
import kotlinx.coroutines.delay
import uz.mobiledv.test1.appwrite.APPWRITE_ENDPOINT
import uz.mobiledv.test1.appwrite.APPWRITE_PROJECT_ID
import uz.mobiledv.test1.mapper.toKmpDocument
import uz.mobiledv.test1.mapper.toKmpDocumentList
import uz.mobiledv.test1.mapper.toKmpFolder
import uz.mobiledv.test1.mapper.toKmpFolderList
import uz.mobiledv.test1.model.AppwriteDocumentList

interface DocumentRepository {
    // USER OPERATIONS
    // Assuming 'emailOrUsername' is used for login, and password.
    // The original 'phoneNumber' in login is ambiguous for typical Appwrite email/password.
    suspend fun login(
        email: String,
        password: String
    ): Result<User> // Changed phoneNumber to email for clarity

    suspend fun getCurrentUser(): Result<User?> // New: to get currently logged-in user
    suspend fun logout(): Result<Unit> // New: to log out
    suspend fun createUser(user: User): Result<User> // Return created user
    suspend fun deleteUser(userId: String): Result<Unit> // Use Appwrite User ID
    suspend fun updateUser(
        userId: String,
        name: String?,
        email: String?,
        phoneNumber: String?
    ): Result<User> // Update specific fields

    suspend fun updateUserPrefs(
        userId: String,
        prefs: uz.mobiledv.test1.model.UserPrefs
    ): Result<User>

    fun getUsers(): Flow<List<User>> // Or suspend fun getUsers(): Result<List<User>> for non-realtime

    // FOLDER OPERATIONS (Consider if these should solely be in FolderRepository)
    suspend fun createFolder(folder: Folder): Result<Folder>
    suspend fun updateFolder(folder: Folder): Result<Folder>
    suspend fun deleteFolder(folderId: String): Result<Unit>
    fun getFolders(): Flow<List<Folder>> // Or suspend fun getFolders(): Result<List<Folder>>

    // DOCUMENT OPERATIONS
    // Changed 'File' from GitLive to ByteArray for KMP, and added fileName, mimeType
    suspend fun uploadDocument(
        documentMetadata: Document, // folderId, name, createdBy, content (optional text)
        fileBytes: ByteArray,
        fileName: String, // for InputFile
        mimeType: String? = null // for InputFile
    ): Result<Document> // Return created document metadata with Appwrite file ID

    suspend fun updateDocumentMetadata(document: Document): Result<Document> // For metadata changes
    fun getDocuments(folderId: String): Flow<List<Document>> // Or suspend fun
    suspend fun downloadDocument(appwriteFileId: String): Result<ByteArray> // Use Appwrite File ID

    suspend fun getDocumentsByFolder(folderId: String): Result<List<Document>> // Changed to Result
    suspend fun createDocumentMetadata(document: Document): Result<Document> // For metadata-only docs

    // updateDocument with content might imply replacing file or updating text content
    suspend fun updateTextDocumentContent(
        documentId: String,
        newName: String,
        newContent: String
    ): Result<Document>

    suspend fun deleteDocument(
        documentId: String,
        appwriteFileId: String?
    ): Result<Unit> // Needs Appwrite file ID for storage deletion

    suspend fun getDocumentById(documentId: String): Result<Document?>

    suspend fun getSessions()
}

class DocumentRepositoryImpl(
    private val account: Account,
    private val databases: Databases,
    private val storage: Storage,
    private val usersApi: Users, // For admin operations like getUsers, deleteUser
    private val ktorHttpClient: HttpClient, // For direct REST calls if needed
    private val appwriteClient: Client // To get JWT for Ktor calls
) : DocumentRepository {

    override suspend fun getSessions() {
//        val sessions = account.getSession(APPWRITE_PROJECT_ID)
//        val listSessions = account.listSessions()
//        println("Appwrite Sessions: ${sessions}")
//        println("Appwrite Sessions List: ${listSessions.sessions}")
    }

    // --- USER OPERATIONS ---
    override suspend fun login(email: String, password: String): Result<User> = runCatching {
        try {
            val session = account.createEmailPasswordSession(email,password)
            println(session.id + "session id")
            // Session created, now get user details
            delay(10_000)
            val appwriteUser = account.get() // Fetches io.appwrite.models.User
            // Map to your app's User model
            User(
                id = appwriteUser.id,
                username = appwriteUser.name ?: "",
                email = appwriteUser.email,
                isAdmin = appwriteUser.prefs?.data?.get("isAdmin") as? Boolean ?: false,
                phoneNumber = appwriteUser.phone
                // prefs = appwriteUser.prefs.data as? UserPrefs // More robust mapping needed
            )
        } catch (e: AppwriteException) {
            // Log e.message, e.code, e.type, e.response
            throw Exception("Login failed: ${e.message}", e)
        }
    }

    override suspend fun getCurrentUser(): Result<User?> = runCatching {
        try {
            val appwriteUser = account.get()
            User(
                id = appwriteUser.id,
                username = appwriteUser.name ?: "",
                email = appwriteUser.email,
                isAdmin = appwriteUser.prefs?.data?.get("isAdmin") as? Boolean ?: false,
                phoneNumber = appwriteUser.phone
            )
        } catch (e: AppwriteException) {
            if (e.code == 401) return@runCatching null // Not logged in
            throw Exception("Failed to get current user: ${e.message}", e)
        }
    }

    override suspend fun logout(): Result<Unit> = runCatching {
        try {
            account.deleteSession("current")
            Unit
        } catch (e: AppwriteException) {
            throw Exception("Logout failed: ${e.message}", e)
        }
    }


    override suspend fun createUser(user: User): Result<User> = runCatching {
        // Create Appwrite auth user
        val createdAppwriteUser = account.create(
            userId = user.id.ifBlank { ID.unique() }, // Use provided ID or generate
            email = user.email
                ?: throw IllegalArgumentException("Email is required for user creation"),
            password = user.password ?: throw IllegalArgumentException("Password is required"),
            name = user.username.ifBlank { null }
        )

        // Update preferences like isAdmin if needed
        if (user.isAdmin) {
            val prefsToUpdate = UserPrefs(
                isAdmin = true,
                customUsername = user.username.takeIf { it != createdAppwriteUser.name })
            account.updatePrefs(prefs = prefsToUpdate)
        }

        // Return mapped user
        User(
            id = createdAppwriteUser.id,
            username = createdAppwriteUser.name ?: user.username,
            email = createdAppwriteUser.email,
            isAdmin = user.isAdmin, // Reflecting the intended state
            phoneNumber = createdAppwriteUser.phone
        )
    }

    // This requires admin privileges / API key with users.write scope
    override suspend fun deleteUser(userId: String): Result<Unit> = runCatching {
        usersApi.delete(userId = userId) // This uses the Users service, not Account
        Unit
    }

    override suspend fun updateUser(
        userId: String,
        name: String?,
        email: String?,
        phoneNumber: String?
    ): Result<User> = runCatching {
        // Note: Updating email via SDK might require user to re-verify.
        // These are admin operations if updating other users. If current user, use account.updateName etc.
        var updatedUser = usersApi.get(userId) // Get current state

        if (name != null && name != updatedUser.name) {
            updatedUser = usersApi.updateName(userId, name)
        }
        if (email != null && email != updatedUser.email) {
            // usersApi.updateEmail(userId, email) // check SDK, might trigger verification
        }
        if (phoneNumber != null && phoneNumber != updatedUser.phone) {
            // usersApi.updatePhone(userId, phoneNumber) // check SDK
        }
        // Map back to your User model
        User(
            id = updatedUser.id,
            username = updatedUser.name ?: "",
            email = updatedUser.email,
            phoneNumber = updatedUser.phone
        )
    }

    override suspend fun updateUserPrefs(userId: String, prefs: UserPrefs): Result<User> =
        runCatching {
            // If updating current user's prefs:
            // val currentAppwriteUser = account.updatePrefs(prefs)
            // For other users (admin):
            usersApi.updatePrefs(userId, prefs)
            val currentUser = usersApi.get(userId)
            User(
                id = currentUser.id,
                username = currentUser.name ?: "",
                email = currentUser.email,
                isAdmin = currentUser.prefs.data["isAdmin"] as? Boolean ?: false,
                phoneNumber = currentUser.phone
            )
        }


    // Requires Admin/Server API Key or specific permissions
    override fun getUsers(): Flow<List<User>> = flow {
        try {
            val userList = usersApi.list() // Fetches io.appwrite.models.UserList
            emit(userList.users.map { appwriteUser ->
                User(
                    id = appwriteUser.id,
                    username = appwriteUser.name ?: "",
                    email = appwriteUser.email,
                    isAdmin = appwriteUser.prefs.data["isAdmin"] as? Boolean
                        ?: false, // Adjust path to isAdmin
                    phoneNumber = appwriteUser.phone
                )
            })
        } catch (e: AppwriteException) {
            throw Exception("Failed to get users: ${e.message}", e)
        }
    }

    // --- FOLDER OPERATIONS (delegating to FolderRepository or implementing directly) ---
    // Assuming these are implemented similar to FolderRepositoryImpl if not delegating
    override suspend fun createFolder(folder: Folder): Result<Folder> = runCatching {
        val response = databases.createDocument(
            databaseId = APPWRITE_DATABASE_ID,
            collectionId = APPWRITE_FOLDERS_COLLECTION_ID,
            documentId = folder.id.ifBlank { ID.unique() },
            data = folder.toAppwriteCreateData(),
            permissions = folder.permissions // Pass permissions if defined in model
        )

        response.toKmpFolder()
    }

    override suspend fun updateFolder(folder: Folder): Result<Folder> = runCatching {
        val response = databases.updateDocument(
            databaseId = APPWRITE_DATABASE_ID,
            collectionId = APPWRITE_FOLDERS_COLLECTION_ID,
            documentId = folder.id,
            data = mapOf("name" to folder.name, "description" to folder.description),
            permissions = folder.permissions
        )
        response.toKmpFolder()
    }

    override suspend fun deleteFolder(folderId: String): Result<Unit> = runCatching {
        databases.deleteDocument(APPWRITE_DATABASE_ID, APPWRITE_FOLDERS_COLLECTION_ID, folderId)
        // Consider deleting all documents within this folder in Appwrite Storage and Databases
        Unit
    }

    override fun getFolders(): Flow<List<Folder>> = flow {
        try {
            val response = databases.listDocuments(
                databaseId = APPWRITE_DATABASE_ID,
                collectionId = APPWRITE_FOLDERS_COLLECTION_ID
            )
            // Manually map if convertTo is problematic or for specific model control
            val folders = response.documents.map { doc ->
                Folder(
                    id = doc.id,
                    name = doc.data["name"] as String,
                    createdBy = doc.data["createdBy"] as String,
                    createdAt = doc.data["\$createdAt"] as String?,
                    description = doc.data["description"] as? String
                        ?: "", // Example: mapping permissions
                )
            }
            emit(folders)
        } catch (e: AppwriteException) {
            throw Exception("Failed to get folders: ${e.message}", e)
        }
    }

    // --- DOCUMENT OPERATIONS ---
    override suspend fun uploadDocument(
        documentMetadata: Document,
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String?
    ): Result<Document> = runCatching {
        // 1. Upload file to Appwrite Storage
        val inputFile =
            InputFile.fromBytes(fileBytes, filename = fileName, mimeType = mimeType.orEmpty())
        val uploadedFile = storage.createFile(
            bucketId = APPWRITE_FILES_BUCKET_ID,
            fileId = ID.unique(),
            file = inputFile,
            permissions = null // Define who can read/write this file
        )

        // 2. Create document metadata in Appwrite Databases
        val metadataToSave = documentMetadata.copy(
            appwriteFileId = uploadedFile.id, // Store the Appwrite file ID
            mimeType = mimeType ?: uploadedFile.mimeType
        )

        val dbDocument = databases.createDocument(
            databaseId = APPWRITE_DATABASE_ID,
            collectionId = APPWRITE_DOCUMENTS_COLLECTION_ID,
            documentId = metadataToSave.id.ifBlank { ID.unique() },
            data = metadataToSave.toAppwriteCreateData(),
            permissions = null // Define who can read/write this metadata
        )
        // Return the full metadata including Appwrite generated IDs
        dbDocument.toKmpDocument()
    }

    // Example of file upload using Ktor (more direct REST, bypasses Appwrite SDK's InputFile)
    // This can be an alternative if InputFile is tricky or for specific header control.

    override suspend fun updateDocumentMetadata(document: Document): Result<Document> =
        runCatching {
            val response = databases.updateDocument(
                databaseId = APPWRITE_DATABASE_ID,
                collectionId = APPWRITE_DOCUMENTS_COLLECTION_ID,
                documentId = document.id,
                data = document.toAppwriteCreateData(), // Only send fields to update
                permissions = document.permissions
            )
            response.toKmpDocument()
        }

    override fun getDocuments(folderId: String): Flow<List<Document>> = flow {
        try {
            val response = databases.listDocuments(
                databaseId = APPWRITE_DATABASE_ID,
                collectionId = APPWRITE_DOCUMENTS_COLLECTION_ID,
                queries = listOf(Query.equal("folderId", folderId))
            )
            emit(response.toKmpDocumentList())
        } catch (e: AppwriteException) {
            throw Exception("Failed to get documents for folder $folderId: ${e.message}", e)
        }
    }

    override suspend fun downloadDocument(appwriteFileId: String): Result<ByteArray> = runCatching {
        storage.getFileDownload(
            bucketId = APPWRITE_FILES_BUCKET_ID,
            fileId = appwriteFileId
        )
    }

    override suspend fun getDocumentsByFolder(folderId: String): Result<List<Document>> =
        runCatching {
            val response = databases.listDocuments(
                databaseId = APPWRITE_DATABASE_ID,
                collectionId = APPWRITE_DOCUMENTS_COLLECTION_ID,
                queries = listOf(Query.equal("folderId", folderId))
            )
            response.toKmpDocumentList()
        }

    override suspend fun createDocumentMetadata(document: Document): Result<Document> =
        runCatching {
            val response = databases.createDocument(
                databaseId = APPWRITE_DATABASE_ID,
                collectionId = APPWRITE_DOCUMENTS_COLLECTION_ID,
                documentId = document.id.ifBlank { ID.unique() },
                data = document.toAppwriteCreateData(),
                permissions = document.permissions
            )
            response.toKmpDocument()
        }

    override suspend fun updateTextDocumentContent(
        documentId: String,
        newName: String,
        newContent: String
    ): Result<Document> = runCatching {
        val dataToUpdate = mapOf(
            "name" to newName,
            "content" to newContent
        )
        val response = databases.updateDocument(
            databaseId = APPWRITE_DATABASE_ID,
            collectionId = APPWRITE_DOCUMENTS_COLLECTION_ID,
            documentId = documentId,
            data = dataToUpdate
        )
        response.toKmpDocument()
    }

    override suspend fun deleteDocument(documentId: String, appwriteFileId: String?): Result<Unit> =
        runCatching {
            // 1. Delete metadata from Databases
            databases.deleteDocument(
                databaseId = APPWRITE_DATABASE_ID,
                collectionId = APPWRITE_DOCUMENTS_COLLECTION_ID,
                documentId = documentId
            )

            // 2. Delete file from Storage if appwriteFileId is provided
            if (!appwriteFileId.isNullOrBlank()) {
                try {
                    storage.deleteFile(
                        bucketId = APPWRITE_FILES_BUCKET_ID,
                        fileId = appwriteFileId
                    )
                } catch (e: AppwriteException) {
                    // Log error or handle (e.g., file might already be deleted or permissions issue)
                    System.err.println("Failed to delete file $appwriteFileId from storage: ${e.message}")
                }
            }
            Unit
        }

    override suspend fun getDocumentById(documentId: String): Result<Document?> = runCatching {
        try {
            val response = databases.getDocument(
                databaseId = APPWRITE_DATABASE_ID,
                collectionId = APPWRITE_DOCUMENTS_COLLECTION_ID,
                documentId = documentId
            )
            response.toKmpDocument()
        } catch (e: AppwriteException) {
            if (e.code == 404) return@runCatching null // Not found
            throw e
        }
    }
}


