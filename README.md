# Document Saver

A Compose Multiplatform document-management prototype with shared Kotlin screens and state, Supabase metadata and file storage, and platform-specific file handling for Android and desktop.

The project explores a practical document workflow: browse a nested folder tree, upload files or a directory, open a document with a system application, and save a local copy. Desktop administrators also have user and folder management controls.

**Status:** a development prototype with significant authentication and platform gaps. Use a disposable backend and sample files when exploring it. The code is useful for studying shared UI, asynchronous file operations, and `expect`/`actual` boundaries; it needs further work before handling real private documents.

## Workflow and platform scope

| Area | Implementation in this repository |
| --- | --- |
| Shared UI | Compose Material 3 login and folder-content screens, navigation, themes, dialogs, operation feedback, and loading/error state. |
| Folder navigation | Parent/child folders, root-folder browsing, alphabetical contents, creation, rename, and deletion calls. |
| Upload | Multiple-file upload and recursive directory traversal; file bytes go to Supabase Storage and metadata goes to PostgREST. |
| Open and save | Download a stored object, then delegate local saving and opening to platform implementations. |
| Desktop administration | An `is_admin` user on the desktop gets management controls, including account creation. This is client-side behavior, not a backend authorization guarantee. |
| Android integration | System document pickers, cache/FileProvider opening, and public Downloads handling. An APK-update experiment is also present but incomplete. |
| iOS integration | Xcode project and Kotlin targets exist, but picker launchers return no selection and several operations remain partial. |

On **Android**, opening a document writes a cache file and launches an external viewer. Saving a copy uses MediaStore on Android 10+ and a public Downloads path on older versions. On **desktop**, the saver opens a Swing save dialog for a destination before the file is opened. The app does not render or edit office documents internally; an appropriate external application is required.

The default picker MIME types cover PDF, PNG/JPEG, Word, and Excel files. These are picker filters, not a guarantee that every selected file can be rendered on every platform.

## How the code fits together

`App.kt` observes the session state and switches between login and folder routes. `AppViewModel` handles the custom user-table login and management flag. `FoldersViewModel` loads folder contents, coordinates Storage and database operations, and exposes operation state to `FolderContentsScreen`.

The common source set declares file-picker, file-saver, opening, and platform APIs. Android and desktop provide different implementations behind those declarations. Koin supplies shared and platform dependencies.

```text
composeApp/
  src/commonMain/kotlin/uz/mobiledv/test1/
    App.kt, AppViewModel.kt
    screens/       Folder UI and folder/file operation state
    model/         Folder, Document, User, release metadata
    data/          Persisted custom session settings
    di/            Supabase and dependency configuration
    util/          Shared file models and expect declarations
  src/androidMain/ Android launchers, pickers, storage, FileProvider
  src/desktopMain/ JVM launcher, AWT/Swing dialogs, local file operations
  src/iosMain/     Partial UIKit/Foundation integration
iosApp/            Xcode wrapper
gradle/libs.versions.toml
```

Useful reading paths:

| Question | Start here |
| --- | --- |
| How does login change navigation? | [App.kt](composeApp/src/commonMain/kotlin/uz/mobiledv/test1/App.kt) and [AppViewModel.kt](composeApp/src/commonMain/kotlin/uz/mobiledv/test1/AppViewModel.kt) |
| How are folder/file actions coordinated? | [FoldersViewModel.kt](composeApp/src/commonMain/kotlin/uz/mobiledv/test1/screens/FoldersViewModel.kt) |
| Which actions does the UI expose? | [FolderContentScreen.kt](composeApp/src/commonMain/kotlin/uz/mobiledv/test1/screens/FolderContentScreen.kt) |
| What crosses the platform boundary? | [Shared file declarations](composeApp/src/commonMain/kotlin/uz/mobiledv/test1/util/util.kt), [Android implementation](composeApp/src/androidMain/kotlin/uz/mobiledv/test1/util/util.android.kt), and [desktop implementation](composeApp/src/desktopMain/kotlin/uz/mobiledv/test1/util/util.desktop.kt) |
| What is persisted locally? | [AuthSettings.kt](composeApp/src/commonMain/kotlin/uz/mobiledv/test1/data/AuthSettings.kt) |

## Technology

Kotlin Multiplatform and Compose Multiplatform provide the shared application. The code also uses Material 3, ViewModels, StateFlow, coroutines, Koin, kotlinx.serialization, Supabase PostgREST/Storage/Auth clients, Ktor, Multiplatform Settings, Okio, and UUID generation.

The checked-in configuration declares Kotlin 2.0.0, Compose plugin 1.7.0, Android Gradle Plugin 8.5.2, Gradle 8.11.1, Android compile/target SDK 35, and minimum SDK 24. JVM bytecode is targeted at Java 11. These are repository versions, not a claim that the dependency combination has been qualified on a current toolchain.

## Explore locally

1. Clone the repository and open it in Android Studio or IntelliJ IDEA with Kotlin Multiplatform support.
2. Use a Gradle JDK compatible with AGP 8.5.2, and install Android SDK 35 for Android builds.
3. Configure your own disposable Supabase project in [supabaseModule.kt](composeApp/src/commonMain/kotlin/uz/mobiledv/test1/di/supabaseModule.kt): replace the project URL, client key, bucket, and table constants. Do not put a service-role key in a distributed app.
4. Prepare tables and storage to match the model contract below. The repository does not include a complete migration or production policy bundle.
5. Create a test account in your own backend. The UI's administrator path requires `is_admin = true` and the desktop platform.

Use the checked-in Gradle wrapper:

```bash
git clone https://github.com/JahongirmirzoDv/Document-Saver.git
cd Document-Saver

# Desktop JVM application
./gradlew :composeApp:run

# Android debug APK
./gradlew :composeApp:assembleDebug
```

On Windows, use `gradlew.bat`. These are build/run entry points; this documentation update did not execute or certify them. The build file declares Android twice and mixes catalog Ktor 2.3.12 dependencies with explicit Ktor 3.0.0/Supabase 3.0.0 dependencies, so Gradle configuration or dependency alignment may be needed.

Desktop packaging targets for DMG, MSI, and DEB are declared in [composeApp/build.gradle.kts](composeApp/build.gradle.kts). Native installers and all three operating systems have not been verified by this review. iOS remains an unfinished integration target.

## Backend contract

The default constants refer to `users`, `folders`, `documents`, and the Storage bucket `second`. Table names and the bucket can be changed in the configuration.

| Resource | Fields used by the Kotlin models |
| --- | --- |
| `users` | `id`, `username`, `email`, `is_admin`, `password_hash`, `created_at` |
| `folders` | `id`, `name`, `description`, `parent_id`, `user_id`, `created_at` |
| `documents` | `id`, `folder_id`, `name`, `storage_file_path`, `content`, `user_id`, `mime_type`, `created_at` |
| Optional `app_releases` | `id`, `version_code`, `version_name`, `apk_storage_path`, `release_notes`, `published_at` |

A null `parent_id` represents a root folder. Documents refer to their folder and to the object path in Storage. File uploads create a Storage object and then a separate metadata row; directory upload recursively creates folder rows and their file contents.

The implemented login queries the custom `users` table. Merely installing Supabase's Auth client does not make that flow a Supabase Auth session. An `auth.users` foreign key or an `auth.uid()` policy is therefore not a drop-in setup for the current code. Authentication, schema relationships, and database/Storage policies must be designed together; no copy-and-run SQL migration is supplied here.

## Known limitations

- **Authentication is unsafe for deployment.** Despite the name `password_hash`, `AppViewModel.kt` stores and compares plaintext passwords. `AuthSettings.kt` serializes the user model into local settings, including that field. Replace this flow with proper server-backed authentication before using real accounts.
- **Management controls are not a security boundary.** The desktop/admin check mainly controls available UI; ViewModel checks are inconsistent across create, update, file-upload, and directory-upload paths. Enforce permissions on the backend.
- **Storage downloads currently use `downloadPublic`.** Private-document delivery requires an authenticated or signed access design, together with appropriate database and bucket policies. Do not make sensitive files public merely to satisfy the current calls.
- **Storage and metadata changes are separate operations.** A failed metadata insert can leave an uploaded object behind. Folder deletion depends on database cascade behavior; document deletion can report metadata success while Storage cleanup fails.
- **Files are buffered in memory.** Pickers read complete file bytes, including recursive directory selections. Large files or trees need memory, cancellation, progress, and failure-recovery work.
- **The Android update path is an experiment.** Its byte-write call is commented out in `AppUpdateViewModel.kt`, and the install permission is commented out in the manifest. It is not a working release/update mechanism.
- **iOS is partial.** Picker launchers are stubs and platform behavior needs further implementation and device verification.

No application build, device run, backend provisioning, policy validation, or end-to-end file operation was performed as part of the documentation review.

## License

[Apache License 2.0](LICENSE).
