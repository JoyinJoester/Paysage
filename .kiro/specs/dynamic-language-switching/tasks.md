# Implementation Plan

- [x] 1. Add Activity recreation event mechanism to SettingsViewModel


  - Add `MutableSharedFlow<Unit>` for recreation events
  - Expose as public `SharedFlow<Unit>` named `recreateActivityEvent`
  - Emit event in `updateLanguage()` after successful repository update
  - _Requirements: 1.1, 1.2_



- [ ] 2. Implement Activity recreation listener in MainActivity
  - Collect `recreateActivityEvent` flow in `onCreate()` using `lifecycleScope`
  - Call `recreate()` when event is received



  - Add try-catch error handling around `recreate()` call
  - _Requirements: 1.1, 1.3, 1.4_

- [ ] 3. Add unit tests for language switching mechanism
  - Write test for `recreateActivityEvent` emission in SettingsViewModel
  - Write test for language persistence before event emission
  - Write test for error handling when repository update fails
  - _Requirements: 4.1, 4.2, 4.3, 4.4_
