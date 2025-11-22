# Design Document: Dynamic Language Switching

## Overview

This design implements instant language switching in the Paysage Android application without requiring a manual app restart. The solution leverages Android's Activity recreation mechanism combined with proper locale configuration management to apply language changes immediately when the user selects a new language in settings.

The current implementation already has the foundation in place with `LocaleHelper` and `attachBaseContext()` override in `MainActivity`, but the language change doesn't trigger Activity recreation, requiring users to manually restart the app.

## Architecture

### Current State Analysis

The application currently has:
- `LocaleHelper`: Utility class that handles locale configuration
- `MainActivity.attachBaseContext()`: Applies locale on Activity creation
- `SettingsRepository`: Persists language preference to DataStore
- `SettingsViewModel`: Manages language update logic

The missing piece is triggering Activity recreation when the language changes.

### Proposed Solution

The solution involves:
1. Detecting language changes in the ViewModel
2. Triggering Activity recreation via a callback mechanism
3. Ensuring the new locale is applied during Activity recreation
4. Preserving navigation state during recreation

## Components and Interfaces

### 1. SettingsViewModel Enhancement

**Purpose**: Detect language changes and notify the Activity to recreate itself

**Changes**:
- Add a `SharedFlow<Unit>` to emit recreation events
- Expose this flow as `recreateActivityEvent`
- Emit event after successful language update in `updateLanguage()`

**Interface**:
```kotlin
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    // Existing code...
    
    private val _recreateActivityEvent = MutableSharedFlow<Unit>(replay = 0)
    val recreateActivityEvent: SharedFlow<Unit> = _recreateActivityEvent.asSharedFlow()
    
    fun updateLanguage(language: Language) {
        viewModelScope.launch {
            try {
                settingsRepository.updateLanguage(language)
                _recreateActivityEvent.emit(Unit) // Trigger recreation
                Log.d(TAG, "Language updated successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update language", e)
            }
        }
    }
}
```

### 2. MainActivity Enhancement

**Purpose**: Listen for recreation events and recreate the Activity

**Changes**:
- Collect `recreateActivityEvent` flow in `onCreate()`
- Call `recreate()` when event is received
- The existing `attachBaseContext()` will apply the new locale automatically

**Implementation Pattern**:
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Collect recreation events
    lifecycleScope.launch {
        settingsViewModel.recreateActivityEvent.collect {
            recreate() // Recreate Activity to apply new locale
        }
    }
    
    // Existing setup code...
}
```

### 3. Settings Screen Enhancement

**Purpose**: Provide immediate visual feedback when language changes

**Changes**:
- No structural changes needed
- The existing language selection UI will work as-is
- Activity recreation will happen automatically after selection

### 4. LocaleHelper (No Changes Required)

**Current Functionality**:
- `setLocale()`: Creates a new Context with the specified locale
- `updateResources()`: Applies locale configuration
- `getCurrentLanguage()`: Retrieves current locale

**Why No Changes**: The existing implementation is sufficient. It's called during `attachBaseContext()` which runs before `onCreate()`, ensuring the new locale is applied before any UI is rendered.

## Data Models

### Language Enum (Existing)

```kotlin
enum class Language {
    SYSTEM,
    ENGLISH,
    CHINESE
}
```

No changes required to the data model.

## Error Handling

### Potential Issues and Solutions

1. **Activity Recreation Failure**
   - **Issue**: `recreate()` might fail in certain edge cases
   - **Solution**: Wrap in try-catch, log error, show user message if needed
   - **Fallback**: User can manually restart app

2. **DataStore Write Failure**
   - **Issue**: Language preference might not persist
   - **Solution**: Already handled in SettingsRepository with try-catch
   - **Impact**: Language would revert on next app start

3. **Race Condition**
   - **Issue**: Multiple rapid language changes
   - **Solution**: Use `SharedFlow` with `replay = 0` to avoid queuing multiple recreations
   - **Behavior**: Only the latest language change takes effect

4. **State Loss During Recreation**
   - **Issue**: Navigation state might be lost
   - **Solution**: Android's default state restoration handles this
   - **Note**: Current screen and basic state are preserved automatically

## Testing Strategy

### Unit Tests

1. **SettingsViewModel Tests**
   - Test that `recreateActivityEvent` emits when `updateLanguage()` is called
   - Test that language is persisted to repository before event emission
   - Test error handling when repository update fails

2. **LocaleHelper Tests**
   - Test locale configuration for each Language enum value
   - Test that correct Locale is returned for each language
   - Test system locale fallback

### Integration Tests

1. **Language Change Flow**
   - Test that changing language in settings triggers Activity recreation
   - Test that new language is applied after recreation
   - Test that navigation state is preserved

2. **Persistence Tests**
   - Test that language preference persists across app restarts
   - Test that saved language is applied on app launch

### Manual Testing Checklist

1. Change language from English to Chinese - verify immediate update
2. Change language from Chinese to English - verify immediate update
3. Change language to System - verify system language is applied
4. Navigate to different screens, change language - verify state preservation
5. Change language multiple times rapidly - verify no crashes
6. Restart app - verify language preference is maintained
7. Test on different Android versions (API 21+)

## Implementation Notes

### Why Activity Recreation?

Activity recreation is the Android-recommended approach for applying configuration changes like locale. It ensures:
- All string resources are reloaded
- All UI components reflect the new language
- Compose recomposition happens automatically
- System UI (like dialogs) uses the correct language

### Performance Considerations

- Activity recreation is fast (typically < 500ms)
- DataStore write is asynchronous and non-blocking
- SharedFlow has minimal memory overhead
- No additional background processes required

### Compatibility

- Solution works on Android API 21+ (current minSdk)
- Uses standard Android APIs (no deprecated methods)
- Compatible with Jetpack Compose lifecycle
- No third-party dependencies required

## Alternative Approaches Considered

### 1. Manual Recomposition
**Approach**: Trigger Compose recomposition without Activity recreation
**Rejected Because**: 
- Doesn't update system UI elements
- Requires manual resource reloading
- More complex implementation
- Doesn't follow Android best practices

### 2. Application-Level Locale
**Approach**: Set locale at Application level only
**Rejected Because**:
- Still requires Activity recreation to take effect
- Doesn't solve the core problem
- More invasive changes required

### 3. Per-Screen Locale Updates
**Approach**: Update locale for each screen individually
**Rejected Because**:
- Fragile and error-prone
- Doesn't handle system dialogs
- High maintenance burden
- Inconsistent behavior

## Migration Path

Since this is an enhancement to existing functionality:
1. No database migrations required
2. No breaking changes to existing APIs
3. Existing language preferences remain valid
4. Users will immediately benefit from the improvement

## Future Enhancements

1. **Smooth Transition Animation**: Add custom transition animation during recreation
2. **Language Preview**: Show preview of UI in selected language before applying
3. **Per-Feature Language**: Allow different languages for different app sections
4. **RTL Support**: Add right-to-left language support (Arabic, Hebrew)
