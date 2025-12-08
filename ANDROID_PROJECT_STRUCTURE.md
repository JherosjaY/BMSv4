# Android Project Structure - Blotter Management System v3

## Organized Directory Structure

```
app/src/main/java/com/example/blottermanagementsystem/
│
├── config/                          ✅ NEW - Configuration files
│   ├── ApiConfig.java              # API endpoints & base URL
│   ├── AppConfig.java              # App-wide settings & feature flags
│   └── Constants.java              # App constants (request codes, intent extras, etc)
│
├── data/                           # Data layer (Repository pattern)
│   ├── api/                        # API clients
│   │   ├── ApiClient.java          # Retrofit client setup
│   │   └── ApiService.java         # API interface definitions
│   │
│   ├── interceptors/               ✅ NEW - HTTP interceptors
│   │   ├── AuthInterceptor.java    # Adds auth token to requests
│   │   └── LoggingInterceptor.java # Logs API requests/responses
│   │
│   ├── database/                   # Local database (Room)
│   │   └── AppDatabase.java        # Database instance
│   │
│   ├── dao/                        # Data Access Objects
│   │   ├── ReportDao.java
│   │   ├── UserDao.java
│   │   └── ...
│   │
│   ├── entity/                     # Database entities
│   │   ├── ReportEntity.java
│   │   ├── UserEntity.java
│   │   └── ...
│   │
│   ├── model/                      # API response models
│   │   ├── ReportModel.java
│   │   ├── UserModel.java
│   │   ├── ApiResponse.java
│   │   └── ...
│   │
│   └── repository/                 # Repository classes
│       ├── ReportRepository.java
│       ├── UserRepository.java
│       ├── AuthRepository.java
│       └── ...
│
├── ui/                             # User Interface layer
│   ├── activities/                 # Activities
│   │   ├── MainActivity.java
│   │   ├── LoginActivity.java
│   │   ├── EditReportActivity.java
│   │   ├── ReportDetailActivity.java
│   │   ├── UserDashboardActivity.java
│   │   └── ...
│   │
│   ├── fragments/                  # Fragments
│   │   ├── ReportsFragment.java
│   │   ├── DashboardFragment.java
│   │   ├── ProfileFragment.java
│   │   └── ...
│   │
│   ├── adapters/                   # RecyclerView adapters
│   │   ├── ReportsAdapter.java
│   │   ├── IncidentTypesAdapter.java
│   │   ├── SuspectsAdapter.java
│   │   └── ...
│   │
│   ├── dialogs/                    # Custom dialogs
│   │   ├── ConfirmDialog.java
│   │   ├── DatePickerDialog.java
│   │   └── ...
│   │
│   └── components/                 ✅ NEW - Reusable UI components
│       ├── LoadingDialog.java      # Loading dialog
│       ├── CustomEditText.java     # Custom EditText
│       ├── CustomButton.java       # Custom Button
│       └── ...
│
├── viewmodel/                      # ViewModel layer (MVVM)
│   ├── ReportViewModel.java
│   ├── UserViewModel.java
│   ├── AuthViewModel.java
│   └── ...
│
├── services/                       # Background services
│   ├── SyncService.java            # Data sync service
│   ├── NotificationService.java    # Push notifications
│   └── ...
│
├── workers/                        # WorkManager tasks
│   ├── SyncWorker.java
│   └── ...
│
├── utils/                          # Utility classes
│   ├── DateUtils.java              # Date formatting
│   ├── ValidationUtils.java        # Input validation
│   ├── FileUtils.java              # File operations
│   ├── SharedPreferencesManager.java
│   ├── NetworkUtils.java           # Network checks
│   ├── AudioRecorder.java          # Audio recording
│   ├── CloudinaryUploader.java     # Image upload
│   ├── BiometricHelper.java        # Biometric auth
│   ├── FCMHelper.java              # Firebase Cloud Messaging
│   └── ... (23 utility classes)
│
├── BlotterApplication.java         # Application class
└── MainActivity.java               # Main entry point
```

## File Organization Guidelines

### 1. Config Folder
- **Purpose**: Centralized configuration management
- **Files**:
  - `ApiConfig.java` - API endpoints, base URLs, timeouts
  - `AppConfig.java` - Feature flags, database settings, preferences
  - `Constants.java` - Request codes, intent extras, status values

### 2. Data Layer
- **api/** - Retrofit client and API interface
- **interceptors/** - HTTP interceptors (auth, logging)
- **database/** - Room database setup
- **dao/** - Data access objects
- **entity/** - Database entity classes
- **model/** - API response models
- **repository/** - Repository pattern implementation

### 3. UI Layer
- **activities/** - Screen activities
- **fragments/** - Fragment screens
- **adapters/** - RecyclerView adapters
- **dialogs/** - Custom dialog classes
- **components/** - Reusable UI components

### 4. ViewModel Layer
- MVVM pattern implementation
- Handles UI logic and data binding

### 5. Services & Workers
- **services/** - Background services
- **workers/** - WorkManager tasks for periodic work

### 6. Utils
- Helper classes for common operations
- Keep organized by functionality

## Benefits of This Structure

✅ **Scalability** - Easy to add new features
✅ **Maintainability** - Clear separation of concerns
✅ **Testability** - Easy to unit test components
✅ **Reusability** - Shared components and utilities
✅ **Consistency** - Follows Android best practices
✅ **Navigation** - Easy to find files

## Migration Checklist

- [x] Create `config/` folder with ApiConfig, AppConfig, Constants
- [x] Create `data/interceptors/` folder with AuthInterceptor, LoggingInterceptor
- [x] Create `ui/components/` folder with LoadingDialog
- [ ] Move API-related files from utils to data/api/
- [ ] Move interceptors from utils to data/interceptors/
- [ ] Update all imports in existing files
- [ ] Test all functionality after migration

## Next Steps

1. **Update ApiClient.java** to use new config classes
2. **Update ApiService.java** to use new endpoint constants
3. **Migrate utility files** to appropriate folders
4. **Update all imports** across the project
5. **Test thoroughly** before committing

## Notes

- All new files should follow this structure
- Use meaningful package names
- Keep related files together
- Document complex logic
- Follow Java naming conventions
