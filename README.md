# Mycelium AI

Android-приложение на основе WebView + Overlay сервис для взаимодействия с Claude AI (Anthropic).

## Возможности

- 🤖 Чат с Claude AI (через официальный API)
- 🎙 Голосовой ввод
- 🪟 Overlay-режим (всплывающее окно поверх других приложений)
- 🌐 WebView интерфейс с поддержкой JavaScript

## Сборка APK

### Требования
- JDK 17+
- Android SDK (compileSdk 34)

### Локальная сборка

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (unsigned)
./gradlew assembleRelease
```

APK появится в:
- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release-unsigned.apk`

## Скачать готовый APK

Перейдите в раздел **Actions** репозитория → выберите последний успешный запуск **Build APK** → скачайте артефакт `mycelium-ai-debug` или `mycelium-ai-release-unsigned`.

## Первый запуск

1. Установите APK на Android-устройство (minSdk 24 / Android 7.0+)
2. Откройте настройки ⚙ и введите Anthropic API Key (`sk-ant-...`)
3. Начните общение с ассистентом

## Структура проекта

```
app/src/main/
├── java/com/mycelium/ai/
│   ├── MainActivity.kt      # WebView активити
│   ├── OverlayService.kt    # Foreground сервис для overlay
│   └── AndroidBridge.kt     # JavaScript ↔ Android мост
├── assets/index.html        # UI приложения
└── res/                     # Ресурсы (иконки, темы, лейауты)
```