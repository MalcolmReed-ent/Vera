# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Initial project setup
- Basic EPUB parsing functionality
- Library management system
- Reading interface with customizable settings
- A sense of humor to the documentation

## [1.1.0] - 2024-03-20

### Added
- Persistent font preferences using SharedPreferences
- Extended font family support with 15 different fonts
- Enhanced theme customization (Light/Dark/Sepia)
- Font picker dialog with preview
- Theme preview in settings

### Changed
- Improved reader settings UI
- Updated theme implementation for better consistency
- Made the app slightly less likely to crash (no promises though)

## [1.0.0] - 2024-12-15

### Added
- Initial release
- Core Features:
  - EPUB file parsing and rendering
  - Chapter navigation
  - Progress tracking and position saving
  - Image support with proper scaling
  - Reading progress persistence
- Library Management:
  - Book import and organization
  - Cover image extraction and display
  - Book metadata handling
  - File management system
- Reading Interface:
  - Continuous scrolling reader
  - Chapter-based navigation
  - Progress indicators
  - Last read position restoration
- Customization:
  - Font size adjustment
  - Font family selection
  - Theme customization (Light/Dark/Sepia)
  - Reading progress tracking
- User Experience:
  - Smooth scrolling
  - Responsive layout
  - Progress saving
  - Error handling
- Several bugs that we'll pretend were features

### Technical Features
- Modern Android architecture components
- Kotlin Coroutines for asynchronous operations
- Room database for data persistence
- Jetpack Compose UI
- MVVM architecture
- Dependency injection with Hilt
- Coil for image loading
