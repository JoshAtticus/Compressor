<div align="center">
  <img width="96" alt="ic_launcher-playstore" src="assets/logo.png?v=2" />

# Compressor

Lightning fast, ad free, super lightweight native video compressor for Android (inspired by the AMAZING Kompresso app for iOS).

<a href="https://apt.izzysoft.de/packages/compress.joshattic.us"><img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroidButtonGreyBorder_nofont.png" height="40" align="middle" alt="Get it at IzzyOnDroid"></a>
<a href="https://play.google.com/store/apps/details?id=compress.joshattic.us"><img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" height="60" align="middle" alt="Get it on Google Play"></a>

<br>

![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white) 
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white) 
![License](https://img.shields.io/github/license/JoshAtticus/Compressor?style=for-the-badge)

<br>

<img src="assets/select.jpg" alt="Screenshot 3" width="24%"/> <img src="assets/settings.jpg" alt="Screenshot 1" width="24%"/> <img src="assets/compressing.jpg" alt="Screenshot 2" width="24%"> <img src="assets/done.jpg" alt="Screenshot 4" width="24%"/>
</div>

> [!TIP]
> Looking for something more advanced? Try [Compressor Edge](https://github.com/JoshAtticus/CompressorEdge)! Compressor Edge receives extra features and fixes not found in Compressor.

---

## Stats & Downloads

[![RB Status](https://shields.rbtlog.dev/simple/compress.joshattic.us?style=for-the-badge)](https://shields.rbtlog.dev/compress.joshattic.us) ![IzzyOnDroid Version](https://img.shields.io/endpoint?url=https://apt.izzysoft.de/fdroid/api/v1/shield/compress.joshattic.us&label=IzzyOnDroid%20Version&style=for-the-badge) ![Stars](https://img.shields.io/github/stars/JoshAtticus/Compressor?style=for-the-badge) ![Forks](https://img.shields.io/github/forks/JoshAtticus/Compressor?style=for-the-badge)

[![GitHub Downloads (all assets, all releases)](https://img.shields.io/endpoint?style=for-the-badge&url=https%3A%2F%2Fcobalt-merry-fresh.joshatticus.workers.dev?v=2)](https://github.com/JoshAtticus/Compressor/releases) [![IzzyOnDroid Downloads (This year)](https://img.shields.io/badge/dynamic/json?url=https://dlstats.izzyondroid.org/iod-stats-collector/stats/basic/yearly/rolling.json&query=$.['compress.joshattic.us']&label=IzzyOnDroid%20yearly%20downloads&style=for-the-badge)](https://apt.izzysoft.de/packages/compress.joshattic.us) [![Google Play Downloads](https://img.shields.io/endpoint?color=green&style=for-the-badge&url=https%3A%2F%2Fplay.cuzi.workers.dev%2Fplay%3Fi%3Dcompress.joshattic.us%26gl%3DUS%26hl%3Den%26l%3DGoogle%2520Play%2520Store%2520Downloads%26m%3D%24totalinstalls)](https://play.google.com/store/apps/details?id=compress.joshattic.us)

---

Do you like Compressor? Consider supporting development by [buying me a coffee](https://www.buymeacoffee.com/joshatticus) ☕️

You can also donate with crypto:
- **Bitcoin:** `bc1q8hkcv5xejcg4n4vf5839pqytp87v92rtgyyccr`
- **Ethereum:** `0xC5Ae73a73F83CF48ed1Cb832ccb9Ca5ff1776EC9`
- **Litecoin:** `ltc1qmf9s65cwk65rlepjme4auqhw7t2wz98f00n3t4`
- **Solana Mainnet:** `HSkCeCd8BzabeVJTzrqcFYvsRmSGLrrDdtZ61oYBgNoD`

---

## Features
- Faster than every single compression app on the Play Store. Period.
- Uses native Media3 library, not another slow, bulky FFMpeg wrapper
- H.265 and AV1 support for compatible devices
- Share Sheet Support
- No third party libraries
- No invasive permissions (no storage, no internet etc)
- Ad free
- Completely native Kotlin (no React Native slop here)
- Simple, clean UI
- Works on Android 7.0 and up
- Reproducible Builds

---

## Performance
Below are four benchmarks of Compressor running on various devices. The baseline videos are available underneath all the benchmarks. All devices are running Compressor **1.6.3**.

> [!NOTE]
> **Testing Context & Methodology (v1.6.3 vs v1.5.2):**
> - **Thermal Conditions:** In the 1.5.2 benchmarks, devices were kept chilled in a cooler between runs. For 1.6.3, all tests were run at realistic **ambient room temperature (~21°C)** without active cooling to better reflect real-world use.
> - **Timing:** Timing was captured manually via stopwatch, so a small margin of error is expected.
> - **Comparison:** Comparisons against previous 1.5.2 results are included where the same device was re-tested.

<details>
<summary><b>Testing Devices</b></summary>
<br>
The following devices are used for testing where possible:

| Device | SoC | RAM |
|---|---|---|
| **Google Pixel 8 Pro** | Tensor G3 | 12GB |
| **Google Pixel (1st Generation)** | Snapdragon 821 | 4GB |
| **Samsung Galaxy Z Flip6** | Snapdragon 8 Gen 3 | 12GB |
| **Samsung Galaxy S21+** | Exynos 2100 | 8GB |
| **Samsung Galaxy S10** | Exynos 9820 | 8GB |
| **Samsung Galaxy A05s** | Snapdragon 680 | 4GB |
| **ZTE Blade A73 5G** | Unisoc T760 | 4GB |
| **Oppo A5 2020** | Snapdragon 665 | 4GB |
| **Nokia 7 plus** | Snapdragon 660 | 4GB |
</details>

<details>
<summary><b>4K60 HEVC SDR - Walk in the Park</b></summary>
<br>
<b>197.3MB 4K 60fps HEVC SDR video compressed using the Medium preset in Compressor.</b>

| Device | Speed (v1.6.3) | Speed (v1.5.2) | Difference |
|---|---|---|---|
| **Google Pixel 8 Pro** | 10s 55ms | 13s 42ms | -2s 87ms (~21% faster) |
| **Google Pixel (1st Generation)** | 24s 49ms | — | New |
| **Samsung Galaxy Z Flip6** | 9s 89ms | — | New |
| **Samsung Galaxy S21+ (Exynos)** | 13s 80ms | 16s 50ms | -2s 70ms (~16% faster) |
| **Samsung Galaxy S10 (Exynos)** | 14s 19ms | 21s 24ms | -7s 05ms (~33% faster) |

The following testing devices were ineligible for this benchmark:
| Device | Reason |
|---|---|
| **Samsung Galaxy A05s** | Hardware cannot handle this video |
| **ZTE Blade A73 5G** | Hardware cannot handle this video |
| **Oppo A5 2020** | Hardware cannot handle this video |
| **Nokia 7 plus** | Hardware cannot handle this video |
</details>

<details>
<summary><b>4K30 HEVC HDR10+ - Challenging Lighting</b></summary>
<br>
<b>136.8MB 4K 30fps HEVC HDR10+ video compressed using the Medium preset in Compressor.</b>

| Device | Speed (v1.6.3) | Speed (v1.5.2) | Difference |
|---|---|---|---|
| **Google Pixel 8 Pro** | 6s 38ms | 7s 23ms | -0s 85ms (~12% faster) |
| **Samsung Galaxy Z Flip6** | 5s 79ms | — | New |
| **Samsung Galaxy S21+ (Exynos)** | 10s 47ms | 9s 27ms | +1s 20ms |
| **Samsung Galaxy S10 (Exynos)** | 11s 25ms | 12s 03ms | -0s 78ms (~6% faster) |
| **ZTE Blade A73 5G** | 24s 40ms | — | New |
| **Oppo A5 2020** | 27s 85ms | — | New |
| **Nokia 7 plus** | 34s 15ms | — | New |

The following testing devices were ineligible for this benchmark:
| Device | Reason |
|---|---|
| **Google Pixel (1st Generation)** | Codec exception (related to handling of HDR) |
| **Samsung Galaxy A05s** | Hardware cannot handle this video |
</details>

<details>
<summary><b>8K24 HEVC SDR - Ultra High Resolution</b></summary>
<br>
<b>266.4MB 8K 24fps HEVC SDR video compressed using the Medium preset in Compressor.</b>

| Device | Speed (v1.6.3) | Speed (v1.5.2) | Difference |
|---|---|---|---|
| **Google Pixel 8 Pro** | 15s 51ms | 16s 21ms | -0s 70ms (~4% faster) |
| **Samsung Galaxy Z Flip6** | 12s 37ms | — | New |
| **Samsung Galaxy S21+ (Exynos)** | 41s 19ms* | 68s 27ms* | -27s 08ms (~40% faster) |
| **Samsung Galaxy S10 (Exynos)** | 21s 61ms | 38s 07ms | -16s 46ms (~43% faster) |

*The Exynos 2100 on the S21+ experiences decoding lag on 8K video playback and compression compared to older and newer SoCs, though performance has notably improved over 1.5.2.

The following testing devices were ineligible for this benchmark:
| Device | Reason |
|---|---|
| **Google Pixel (1st Generation)** | Hardware cannot handle this video |
| **Samsung Galaxy A05s** | Hardware cannot handle this video |
| **ZTE Blade A73 5G** | Hardware cannot handle this video |
| **Oppo A5 2020** | Hardware cannot handle this video |
| **Nokia 7 plus** | Hardware cannot handle this video |
</details>

<details>
<summary><b>1080p60 HEVC SDR - Consistent Subject</b></summary>
<br>
<b>34.5MB 1080p 60fps HEVC SDR video compressed using the Medium preset in Compressor.</b>

| Device | Speed (v1.6.3) | Speed (v1.5.2) | Difference |
|---|---|---|---|
| **Google Pixel 8 Pro** | 4s 45ms | 3s 45ms | +1s 00ms |
| **Google Pixel (1st Generation)** | 7s 45ms | — | New |
| **Samsung Galaxy Z Flip6** | 4s 65ms | — | New |
| **Samsung Galaxy S21+ (Exynos)** | 9s 19ms | 4s 73ms | +4s 46ms |
| **Samsung Galaxy S10 (Exynos)** | 9s 26ms | 5s 38ms | +3s 88ms |
| **Samsung Galaxy A05s** | 19s 09ms | 22s 27ms | -3s 18ms (~14% faster) |
| **ZTE Blade A73 5G** | 7s 25ms | — | New |
| **Oppo A5 2020** | 10s 64ms | — | New |
| **Nokia 7 plus** | 14s 94ms | — | New |
</details>

<details>
<summary><b>Old Benchmarks (v1.5.2)</b></summary>
<br>

| Device | Walk in the Park (4K60) | Challenging Lighting (4K30 HDR) | Ultra High Res (8K24) | Consistent Subject (1080p60) |
|---|---|---|---|---|
| **Google Pixel 8 Pro** | 13s 42ms | 7s 23ms | 16s 21ms | 3s 45ms |
| **Samsung Galaxy S21+ (Exynos)** | 16s 50ms | 9s 27ms | 68s 27ms | 4s 73ms |
| **Samsung Galaxy S10 (Exynos)** | 21s 24ms | 12s 03ms | 38s 07ms | 5s 38ms |
| **Samsung Galaxy S9 (Exynos)** | 35s 77ms | 16s 59ms | Unsupported | 9s 05ms |
| **Samsung Galaxy S7 (Exynos)** | 43s 48ms | Unsupported | Unsupported | 17s 02ms |
| **Samsung Galaxy A71 4G** | 64s 19ms | 22s 84ms | Unsupported | 9s 92ms |
| **Samsung Galaxy A32 4G** | Unsupported | Unsupported | Unsupported | 11s 41ms |
| **Samsung Galaxy A05s** | Unsupported | Unsupported | Unsupported | 22s 27ms |
</details>

<details>
<summary><b>Download Baseline Videos</b></summary>
<br>
If you would like to test Compressor on your own device, you can download the baseline videos used in the benchmarks below. The baseline videos are not for commercial use. Compressor Baseline Videos © 2026 by JoshAtticus is licensed under CC BY-NC-ND 4.0.

You can download them [here](https://l.joshattic.us/mDAc6J)
</details>

<br>

*Why are the benchmarks different between versions?*
Between v1.5.2 and v1.6.3, the testing methodology shifted from keeping phones in a cooler between rounds to realistic ambient room temperature testing (~21°C). Additionally, timing was recorded manually via stopwatch so minor variations are expected. Despite ambient thermal loads, optimizations across newer versions show substantial improvements on heavy workloads (such as 8K & 4K decoding on Exynos chips and budget devices like the A05s).

*Why have some devices changed?*
New devices (Galaxy Z Flip6, ZTE Blade A73 5G, Oppo A5 2020, Nokia 7 plus, and the Google Pixel 1st Gen) were introduced to provide a broader view spanning modern flagships, budget 5G/4G SoCs, and older legacy hardware.

---

## Credits
Compressor wouldn't be possible without these amazing people

[@rA9stuff](https://github.com/rA9stuff) - Inspiration to create Compressor & donated

[@tgranz](https://github.com/tgranz) - Provided funding to get Compressor on Google Play

[@sirtoaks](https://github.com/sirtoaks) - Provided funding to get Compressor on Google Play

[@3r1s-s](https://github.com/3r1s-s) - Created Compressor's new logo

[@AhmedRX20](https://github.com/AhmedRX20) - Translated into Arabic

I would like to acknowledge that Compressor has used AI language models to assist in translation. Should you find any issues in translation, please open a bug report or a pull request so they can be fixed.

---

## Star History

[![Star History Chart](https://api.star-history.com/chart?repos=JoshAtticus/Compressor&type=date&legend=top-left&sealed_token=sNTeAN-m_zFqDfeGZd5lJAfsf4y3KjmvJR7uXX32il2bO6AFPFeqr7UiNveSw0YOrLwlol6zew0GOlZa21PNAzzf3XQLOhQCyoxJ3prS4EAmbhp3MOq_yg)](https://www.star-history.com/?type=date&repos=JoshAtticus%2FCompressor)
