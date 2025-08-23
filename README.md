# SwiftConvert (Android)

**On-device file converter** (Kotlin + Jetpack Compose) with **FFmpegKit** and Android-native APIs.  
Convert **Audio**, **Video**, **Images**, and **Documents** completely offline via the Storage Access Framework (no storage permission).

## Features

- **Audio** ⟷ mp3, m4a, **m4b (audiobook)**, aac (adts), opus/webm, ogg/vorbis, flac, wav  
- **Video** ⟷ mp4 (H.264/H.265), webm (VP9/Opus), mkv, mov, **gif (animated)**
- **Images** ⟷ jpg, png, webp, bmp, tiff, heic* ; **Image → PDF**
- **Documents** ⟷ PDF → images.zip (one PNG per page), **.txt/.html/.md → PDF** (simple offline rendering)

\* HEIC encoding depends on your FFmpegKit build including `libheif`. Decoding is usually fine. If encoding fails, convert **from** HEIC to other formats.

---

## Input → Output Matrix (high-level)

| Input       | Output (examples)                                                                                          |
|-------------|-------------------------------------------------------------------------------------------------------------|
| **Audio**   | mp3, m4a, **m4b**, aac, ogg (vorbis), opus (webm), flac, wav                                               |
| **Video**   | mp4 (H.264/H.265), webm (VP9/Opus), mkv (H.264/AAC), mov (H.264/AAC), **gif**                              |
| **Image**   | jpg, png, webp, bmp, tiff, heic*  ·  **→ PDF**                                                             |
| **PDF**     | **→ images.zip** (page_1.png, page_2.png, …)                                                                |
| **Text/HTML/MD** | **→ PDF** (plain text rendering; no external libs)                                                     |

> Chapters for **.m4b** are not authored by default; we can add metadata/chapters later via `-map_metadata` from a cue/JSON when needed.

---

## Requirements

- Android Studio **Jellyfish** or newer  
- **minSdk 24**, **targetSdk 34**  
- App depends on **FFmpegKit (min-gpl)** and **Media3** (kept for optional fallback)

---

## Build & Run

1. **Clone** the repo.
2. Open in **Android Studio**. Let Gradle sync.
3. Run on a device/emulator (Android 7.0+).

> If using GitHub Actions, ensure you commit the **Gradle wrapper** (`gradlew`, `gradlew.bat`, and `gradle/wrapper/**`).  
> From a terminal you can create/update it with: `./gradlew wrapper --gradle-version 8.7`

---

## How it Works

- **FFmpegTranscoder** handles broad **audio/video/image** conversions (FFmpegKit).
- **PdfConverter** does **image → PDF** and **PDF → images.zip**
