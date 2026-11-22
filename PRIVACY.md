# Privacy Policy for Compressor

**Last updated:** November 22, 2026

**Compressor** ("the App") is designed with privacy as its core principle.

## 1. Data Collection
The App **does not** collect, store, transmit, or share any personal information, usage data, or metadata. 

## 2. Permissions
The App operates without invasive network or tracking permissions:
*   **No Internet Access:** The App does not request the `INTERNET` permission. It is technically impossible for the App to send data to a remote server.
*   **Compress tab (picker-only):** The Compress flow uses the Android System Photo Picker (or a gallery app you choose). It only has access to the specific video files you explicitly select for compression.
*   **Free space tab (opt-in scan):** The optional Free space feature can request media read access (`READ_MEDIA_VIDEO` / `READ_MEDIA_IMAGES`, or legacy storage read on older Android versions) so it can list large photos and videos on your device, estimate savings, and batch-compress items you select. This scan runs only after you open Free space and grant access. Processing stays on-device. Free space never silently replaces originals; deleting an original requires your confirmation.

## 3. Third-Party Services
The App contains **no** third-party tracking, analytics, or advertising SDKs (such as Google Analytics, Firebase, or AdMob).

## 4. Local Processing
All photo and video compression is performed locally on your device using Android media APIs and hardware acceleration where available. Your media never leaves your device.

## 5. Contact
If you have questions about this policy, please open an issue on the [GitHub Repository](https://github.com/JoshAtticus/Compressor) or send an email to compressor@joshattic.us.
