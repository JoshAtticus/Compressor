# Contributing to Compressor
First off, thank you for considering contributing to Compressor! This project is built on the philosophy of doing exactly one thing perfectly: **Lightning-fast, native, offline video compression.**

To keep the codebase clean, features focused and app lightweight and private, there are very strict guidelines on what features will be accepted.

## What WILL NOT be accepted (Do not open PRs or Issues for these)
Before you write any code, please review [Pinned Issue #3](https://github.com/JoshAtticus/Compressor/issues/3). PRs containing the following will be immediately closed and labeled as `invalid`:
*   **Batch/Bulk Processing:** This requires complex UI and queue management. Out of scope.
*   **Custom File Pickers:** We strictly use the Android Photo Picker or the `Share` intent. **Never** request `READ_EXTERNAL_STORAGE` or `MANAGE_EXTERNAL_STORAGE`.
*   **Audio/Image Compression:** Compressor is a video tool. 
*   **Third-Party Libraries:** Especially FFmpeg wrappers. We use pure Media3.
*   **AI-Generated Slop:** If you used an autonomous agent (like Jules, Devin, etc.) to generate a massive architectural rewrite without understanding the code, it will be closed. We review code. We do not review hallucinations.

## What WILL be accepted
*   **Bug Fixes:** Especially edge-case crashes on specific OEM hardware (Samsung, Huawei, etc.).
*   **Translations:** Adding new languages to `strings.xml` is always welcome! (Please ensure you are a native speaker or have verified the context).
*   **UI/UX Polish:** Improvements to Jetpack Compose layouts, accessibility, or Material 3 dynamic theming.
*   **Media3 Optimizations:** Better handling of weird codecs, HDR tone-mapping fallbacks, or metadata (EXIF) preservation.

## How to submit a Pull Request
1.  **Open an Issue First:** Unless it's a minor typo or translation, please open an issue to discuss your planned changes before you spend hours coding. 
2.  **Test It:** Ensure your code compiles and runs. Test it on a physical device, not just an emulator (Emulators notoriously struggle with Media3 hardware encoders).
3.  **Keep it Clean:** Follow standard Kotlin styling. No placeholder code,
4.  **No New Permissions:** Check the `AndroidManifest.xml`. If your PR adds a permission (like `ACCESS_NETWORK_STATE`), it will be rejected.