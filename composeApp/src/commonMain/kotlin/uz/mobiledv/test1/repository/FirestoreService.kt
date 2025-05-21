package uz.mobiledv.test1.repository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.FieldValue
import uz.mobiledv.test1.model.User

class FirestoreService {
    // Lazily initialize Firestore instance
    // Ensure FirebaseInitializer.initialize() is called before accessing this
    private val db: FirebaseFirestore by lazy {
        try {
            Firebase.firestore
        } catch (e: Exception) {
            println("Error getting Firestore instance. Ensure Firebase is initialized. Error: ${e.message} ${e.cause}")
            throw IllegalStateException("Firebase not initialized or Firestore not available.", e)
        }
    }

}