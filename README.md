# Doc Saver - Secure Document Management (Android & Desktop)

**Doc Saver** is a Kotlin Multiplatform application designed for secure and organized document management across Android and Desktop (Windows, macOS, Linux) platforms. It allows users to create folders, upload various file types, and access them seamlessly, with a focus on providing distinct functionalities for regular users and administrative managers.

## ✨ Features

* **Cross-Platform:**
    * Native Android application.
    * Native Desktop application (powered by Compose for Desktop).
* **User Authentication:**
    * Secure login system.
    * "Remember me" functionality for convenience.
    * Desktop administrators can create and manage user accounts.
* **Folder Management (Admin/Manager Focused):**
    * Create, edit, and delete folders to organize documents.
    * Folders are visible to all authenticated users.
* **Document Management:**
    * **Upload:** Managers can upload various file types (PDF, PNG, JPEG, DOC, DOCX, XLS, XLSX) to specific folders.
    * **View/Open:** All users can click on a document to download it to a temporary cache and open it with the appropriate system application.
    * **Download to Public Folder:** All users can explicitly download documents to their device's public "Downloads" folder (or choose a location on Desktop).
    * **Delete (Admin/Manager Focused):** Managers can delete documents from folders.
* **Backend Integration:**
    * Utilizes **Supabase** for:
        * User authentication (custom table-based).
        * Database storage (PostgreSQL for folder and document metadata).
        * File storage (Supabase Storage for actual document files).
* **Modern UI:**
    * Built with Jetpack Compose for Android and Compose Multiplatform for Desktop, ensuring a consistent and modern user experience.
    * Material 3 design principles.
    * Light and Dark theme support.
* **File Handling:**
    * Platform-specific file pickers for uploading.
    * Platform-specific file savers for downloading to public directories or user-chosen locations.
    * MIME type detection and appropriate icon display for different file types.
* **(Android) In-App Updates:**
    * Checks for new app versions from a Supabase table.
    * Downloads and prompts for installation of APK updates (requires appropriate permissions).

## 🛠️ Tech Stack

* **Kotlin Multiplatform:** Core logic sharing.
* **Jetpack Compose / Compose Multiplatform:** UI framework.
* **Supabase:** Backend-as-a-Service (Authentication, Database, Storage).
    * `supabase-kt` library for client-side interaction.
* **Koin:** Dependency Injection.
* **Ktor Client:** HTTP requests (used by Supabase client and for app updates).
* **Kotlinx Serialization:** JSON handling.
* **Coroutines:** Asynchronous programming.
* **Multiplatform Settings:** Storing user preferences (e.g., last logged-in email).
* **Okio:** Efficient I/O operations.
* **UUID:** Generating unique identifiers.

## 🚀 Getting Started

### Prerequisites

* Java Development Kit (JDK) 11 or higher.
* Android Studio (latest stable version recommended for Android development).
* IntelliJ IDEA (latest stable version recommended for Desktop development).
* A Supabase project set up with:
    * A `users` table (see `model/User.kt` for schema hints - **IMPORTANT: Implement secure password hashing instead of plain text**).
    * A `folders` table (see `model/Folder.kt`).
    * A `documents` table (see `model/Document.kt`).
    * A Supabase Storage bucket (default name in code is "second", configurable in `di/supabaseModule.kt`).
    * (Optional for Android updates) An `app_releases` table (see `model/AppVersionInfo.kt`).
    * Row Level Security (RLS) policies configured appropriately for data access.

### Configuration

1.  **Supabase Credentials:**
    * Update the Supabase URL and Key in `composeApp/src/commonMain/kotlin/uz/mobiledv/test1/di/supabaseModule.kt`:
        ```kotlin
        const val URL = "YOUR_SUPABASE_URL"
        const val KEY = "YOUR_SUPABASE_ANON_KEY"
        const val BUCKET = "your_bucket_name" // e.g., "documents"
        ```
2.  **Android `FileProvider` Authority:**
    * Ensure the `authorities` attribute in `composeApp/src/androidMain/AndroidManifest.xml` for the `<provider>` tag matches your `applicationId`. It's currently set to `${applicationId}.provider`.

### Building and Running

#### Android

1.  Open the project in Android Studio.
2.  Let Gradle sync and download dependencies.
3.  Select the `composeApp` run configuration.
4.  Choose an emulator or connect a physical device.
5.  Click the "Run" button.

#### Desktop

1.  Open the project in IntelliJ IDEA.
2.  Let Gradle sync and download dependencies.
3.  Locate the `main` function in `composeApp/src/desktopMain/kotlin/uz/mobiledv/test1/main.kt`.
4.  Click the green play button next to the `main` function to run the desktop application.
5.  To build native distribution packages (MSI, DMG, DEB), use the Gradle tasks under `composeApp > Tasks > compose desktop > package[Format]`, e.g., `packageDmg`, `packageMsi`.

## 🔐 Security Considerations

* **Password Hashing:** The current authentication mechanism (`AppViewModel.kt`) uses **plain text password storage and comparison**. This is **EXTREMELY INSECURE** and is only for demonstration purposes. **In a production environment, you MUST implement strong password hashing (e.g., bcrypt, scrypt, or Argon2) on the client-side before sending to a custom auth endpoint, or ideally, use Supabase's built-in GoTrue authentication which handles this securely.**
* **Supabase Row Level Security (RLS):** It is crucial to configure RLS policies in your Supabase dashboard to control data access properly. For example:
    * Users should only be able to select their own data or data explicitly shared with them.
    * Admins/Managers might have broader permissions.
    * Ensure that file storage access rules are also appropriately set.
* **Input Validation:** Always validate user inputs on both client and server sides.
* **Error Handling:** Implement robust error handling to prevent crashes and provide informative messages to the user.

## 🏗️ Project Structure

* `composeApp/src/commonMain`: Shared Kotlin code (ViewModels, data models, DI, common UI components, Supabase logic).
* `composeApp/src/androidMain`: Android-specific code (MainActivity, platform-specific implementations for file handling, DI modules, AndroidManifest).
* `composeApp/src/desktopMain`: Desktop-specific code (main function, platform-specific implementations for file handling, DI modules).
* `composeApp/src/commonMain/composeResources`: Shared drawable resources.

## 🤝 Contributing

Contributions are welcome! If you'd like to contribute, please follow these steps:

1.  Fork the repository.
2.  Create a new branch (`git checkout -b feature/your-feature-name`).
3.  Make your changes.
4.  Commit your changes (`git commit -m 'Add some feature'`).
5.  Push to the branch (`git push origin feature/your-feature-name`).
6.  Open a Pull Request.

Please ensure your code adheres to the existing style and includes tests where appropriate.

## 📝 License

This project is licensed under the [MIT License](LICENSE.md) (You'll need to add a LICENSE.md file if you choose this).

---

*This README is a template. Please update it with specific details about your project, setup instructions, and any other relevant information.*
