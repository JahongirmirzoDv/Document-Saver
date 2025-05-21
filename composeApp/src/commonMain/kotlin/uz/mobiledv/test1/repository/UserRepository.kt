package uz.mobiledv.test1.repository

import uz.mobiledv.test1.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface UserRepository {
    suspend fun getAllUsers(): List<User>
    suspend fun createUser(username: String, password: String, isAdmin: Boolean)
    suspend fun updateUser(id: String, username: String, password: String, isAdmin: Boolean)
    suspend fun deleteUser(id: String)
    suspend fun getUserById(id: String): User?
    suspend fun getUserByCredentials(username: String, password: String): User?
    suspend fun login(username: String, password: String): User?
    suspend fun getCurrentUser(): User?
    fun observeCurrentUser(): Flow<User?>
}

class UserRepositoryImpl : UserRepository {
    private val users = mutableListOf<User>()
    private val currentUserFlow = MutableStateFlow<User?>(null)

    init {
        users.add(User("1", "admin", "admin"))
    }

    override suspend fun getAllUsers(): List<User> = users

    override suspend fun createUser(username: String, password: String, isAdmin: Boolean) {
        users.add(User((users.size + 1).toString(), username, password))
    }

    override suspend fun updateUser(id: String, username: String, password: String, isAdmin: Boolean) {
        users.replaceAll { user ->
            if (user.id == id) user.copy(username = username, password = password) else user
        }
    }

    override suspend fun deleteUser(id: String) {
        users.removeAll { it.id == id }
    }

    override suspend fun getUserById(id: String): User? =
        users.find { it.id == id }

    override suspend fun getUserByCredentials(username: String, password: String): User? =
        users.find { it.username == username && it.password == password }

    override suspend fun login(username: String, password: String): User? {
        // Simulate login - in real app, this would call an API
        return if (username == "test" && password == "test") {
            User("1", username, "test@example.com").also {
                currentUserFlow.value = it
            }
        } else null
    }

    override suspend fun getCurrentUser(): User? = currentUserFlow.value

    override fun observeCurrentUser() = currentUserFlow.asStateFlow()
} 