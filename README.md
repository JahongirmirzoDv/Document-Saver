# Doc Saver - Secure Document Management (Android & Desktop)

**Doc Saver** is a Kotlin Multiplatform application designed for secure and organized document management across Android and Desktop (Windows, macOS, Linux) platforms. It allows users to create folders (including nested subfolders), upload various file types, and access them seamlessly, with a focus on providing distinct functionalities for regular users and administrative managers.

## ✨ Features

* **Cross-Platform:**
  * Native Android application.
  * Native Desktop application (powered by Compose for Desktop).
* **User Authentication:**
  * Secure login system.
  * "Remember me" functionality for convenience.
  * Desktop administrators can create and manage user accounts.
* **Hierarchical Folder Management:**
  * Create, edit, and delete folders and subfolders to organize documents.
  * Navigate through nested folder structures.
  * Folder operations (create, edit, delete) are typically manager-focused.
  * Folders are visible to all authenticated users based on permissions (RLS recommended).
* **Document Management:**
  * **Upload:** Managers (or configured users) can upload various file types (PDF, PNG, JPEG, DOC, DOCX, XLS, XLSX) into any selected folder.
  * **View/Open:** All users can click on a document to download it to a temporary cache and open it with the appropriate system application.
  * **Download to Public Folder:** All users can explicitly download documents to their device's public "Downloads" folder (or choose a location on Desktop).
  * **Delete (Admin/Manager Focused):** Managers can delete documents from folders.
* **Backend Integration:**
  * Utilizes **Supabase** for:
    * User authentication (custom table-based).
    * Database storage (PostgreSQL for folder and document metadata, supporting parent-child folder relationships).
    * File storage (Supabase Storage for actual document files).
* **Modern UI:**
  * Built with Jetpack Compose for Android and Compose Multiplatform for Desktop.
  * Material 3 design principles.
  * Light and Dark theme support.
* **File Handling:**
  * Platform-specific file pickers for uploading.
  * Platform-specific file savers for downloading.
  * MIME type detection and appropriate icon display.
* **(Android) In-App Updates:**
  * Checks for new app versions from a Supabase table.
  * Downloads and prompts for installation of APK updates.

## 🛠️ Tech Stack

* **Kotlin Multiplatform:** Core logic sharing.
* **Jetpack Compose / Compose Multiplatform:** UI framework.
* **Supabase:** Backend-as-a-Service.
  * `supabase-kt` library.
* **Koin:** Dependency Injection.
* **Ktor Client:** HTTP requests.
* **Kotlinx Serialization:** JSON handling.
* **Coroutines:** Asynchronous programming.
* **Multiplatform Settings:** Storing user preferences.
* **Okio:** Efficient I/O.
* **UUID:** Generating unique identifiers.

## 🚀 Getting Started

### Prerequisites

* Java Development Kit (JDK) 11 or higher.
* Android Studio (latest).
* IntelliJ IDEA (latest).
* A Supabase project.

### Supabase Database Setup:

Ensure your Supabase project has the following tables (SQL DDL examples provided for guidance, adapt as needed):

* **`folders` table:**
  * `id` (UUID, Primary Key, default `uuid_generate_v4()`)
  * `name` (TEXT, not null)
  * `description` (TEXT, nullable)
  * `user_id` (UUID, Foreign Key to `auth.users(id)` or your custom users table `id`, not null)
  * `parent_id` (UUID, Foreign Key to `folders(id)` on delete cascade or set null, nullable for root folders)
  * `created_at` (TIMESTAMPTZ, default `now()`)
    ```sql
    CREATE TABLE folders (
        id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        name TEXT NOT NULL,
        description TEXT,
        user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
        parent_id UUID REFERENCES folders(id) ON DELETE CASCADE, -- Adjust ON DELETE behavior as needed
        created_at TIMESTAMPTZ DEFAULT now()
    );
    -- Index for faster parent_id lookups
    CREATE INDEX idx_folders_parent_id ON folders(parent_id);
    CREATE INDEX idx_folders_user_id ON folders(user_id);
    ```

* **`documents` table:**
  * `id` (UUID, Primary Key, default `uuid_generate_v4()`)
  * `folder_id` (UUID, Foreign Key to `folders(id)` on delete cascade, not null)
  * `name` (TEXT, not null)
  * `storage_file_path` (TEXT, not null, unique)
  * `user_id` (UUID, Foreign Key to `auth.users(id)` or your custom users table `id`, not null)
  * `mime_type` (TEXT, nullable)
  * `created_at` (TIMESTAMPTZ, default `now()`)
    ```sql
    CREATE TABLE documents (
        id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        folder_id UUID NOT NULL REFERENCES folders(id) ON DELETE CASCADE,
        name TEXT NOT NULL,
        storage_file_path TEXT NOT NULL UNIQUE,
        user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
        mime_type TEXT,
        created_at TIMESTAMPTZ DEFAULT now()
    );
    -- Index for faster folder_id lookups
    CREATE INDEX idx_documents_folder_id ON documents(folder_id);
    CREATE INDEX idx_documents_user_id ON documents(user_id);
    ```
* **`users` table:** (If using custom table, otherwise Supabase `auth.users` is used)
  * Your existing schema. Ensure `id` is UUID if referenced by `user_id` above.
  * Add `is_admin` (BOOLEAN, default `false`) if not already present.

* **Supabase Storage Bucket:**
  * Create a bucket (e.g., "files", "user_documents"). Update `BUCKET` constant in `di/supabaseModule.kt`.
  * Configure Storage policies (e.g., users can upload to paths prefixed with their `user_id`, users can download files they own or are shared with them).

### Configuration

1.  **Supabase Credentials:**
  * Update URL, Key, and Bucket name in `composeApp/src/commonMain/kotlin/uz/mobiledv/test1/di/supabaseModule.kt`.
2.  **Android `FileProvider` Authority:**
  * Verify in `composeApp/src/androidMain/AndroidManifest.xml`.

### Building and Running
(Instructions remain the same)

... (rest of README.md can remain similar, update security section about RLS for hierarchical data) ...

## 🔐 Security Considerations

* **Password Hashing:** The current authentication mechanism (`AppViewModel.kt`) uses **plain text password storage and comparison**. This is **EXTREMELY INSECURE**. **In a production environment, you MUST implement strong password hashing (e.g., bcrypt, scrypt, or Argon2) or, ideally, use Supabase's built-in GoTrue authentication.**
* **Supabase Row Level Security (RLS):** Crucial for hierarchical data.
  * Users should only be able to select/insert/update/delete folders where `parent_id` they have access to, or their own `user_id` matches.
  * Root folder creation might be restricted to admins.
  * Document access should be tied to folder access or user ownership.
  * Storage access rules are also critical.
* **Input Validation:** Validate all inputs.
* **Recursive Operations:** Be cautious with recursive delete operations. Test thoroughly.