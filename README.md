# Compressor
Lightning fast, ad free, super lightweight native video compressor for Android (inspired by the AMAZING Kompresso app for iOS).

<p align="left">
  <a href="https://apt.izzysoft.de/packages/compress.joshattic.us"><img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroidButtonGreyBorder_nofont.png" height="40" align="middle" alt="Get it at IzzyOnDroid"></a><a href="https://play.google.com/store/apps/details?id=compress.joshattic.us"><img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" height="60" align="middle" alt="Get it on Google Play"></a>
</p>

<img src="assets/select.png" alt="Screenshot 3" width="24%"/><img src="assets/settings.png" alt="Screenshot 1" width="24%"/><img src="assets/compressing.png" alt="Screenshot 2" width="24%"><img src="assets/done.png" alt="Screenshot 4" width="24%"/>

### See the quality for yourself on YouTube

<a href="https://www.youtube.com/watch?v=KieyI-8Tttk"><img width="50%" src="https://github.com/user-attachments/assets/759c18dc-afeb-4023-ac04-6f331058d6bb"></a>

![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white) ![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white) ![License](https://img.shields.io/github/license/JoshAtticus/Compressor?style=for-the-badge)

**Stats**

[![RB Status](https://shields.rbtlog.dev/simple/compress.joshattic.us?style=for-the-badge)](https://shields.rbtlog.dev/compress.joshattic.us) ![IzzyOnDroid Version](https://img.shields.io/endpoint?url=https://apt.izzysoft.de/fdroid/api/v1/shield/compress.joshattic.us&label=IzzyOnDroid%20Version&style=for-the-badge) ![Stars](https://img.shields.io/github/stars/JoshAtticus/Compressor?style=for-the-badge) ![Forks](https://img.shields.io/github/forks/JoshAtticus/Compressor?style=for-the-badge)

**Downloads**

[![GitHub Downloads (all assets, all releases)](https://img.shields.io/github/downloads/JoshAtticus/Compressor/total?style=for-the-badge&label=GitHub%20Downloads&v=2)](https://github.com/JoshAtticus/Compressor/releases) [![IzzyOnDroid Downloads (This year)](https://img.shields.io/badge/dynamic/json?url=https://dlstats.izzyondroid.org/iod-stats-collector/stats/basic/yearly/rolling.json&query=$.['compress.joshattic.us']&label=IzzyOnDroid%20yearly%20downloads&style=for-the-badge)](https://apt.izzysoft.de/packages/compress.joshattic.us) [![Google Play Downloads](https://img.shields.io/endpoint?color=green&style=for-the-badge&url=https%3A%2F%2Fplay.cuzi.workers.dev%2Fplay%3Fi%3Dcompress.joshattic.us%26gl%3DUS%26hl%3Den%26l%3DGoogle%2520Play%2520Store%2520Downloads%26m%3D%24totalinstalls)](https://play.google.com/store/apps/details?id=compress.joshattic.us)

Do you like Compressor? Consider supporting development by [buying me a coffee](https://www.buymeacoffee.com/joshatticus) ☕️

You can also donate with crypto
- Bitcoin: bc1q8hkcv5xejcg4n4vf5839pqytp87v92rtgyyccr
- Ethereum: 0xC5Ae73a73F83CF48ed1Cb832ccb9Ca5ff1776EC9
- Litecoin: ltc1qmf9s65cwk65rlepjme4auqhw7t2wz98f00n3t4
- Solana Mainnet: HSkCeCd8BzabeVJTzrqcFYvsRmSGLrrDdtZ61oYBgNoD

## Features
- Faster than every single compression app on the Play Store. Period.
- Uses native Media3 library, not another slow, bulky FFMpeg wrapper
- H.265 and AV1 support for compatible devices
- Share Sheet Support
- No third party libraries
- No invasive permissions (no storage, no internet etc)
- Ad free
- Super lightweight (< 10MB)
- Completely native Kotlin (no React Native slop here)
- Simple, clean UI
- Works on Android 7.0 and up
- Reproducible Builds

## Performance
Below are four benchmarks of Compressor running on various devices. The baseline videos are available underneath all the benchmarks. All devices are running the latest version of Compressor from the Google Play Store at the time (1.5.2).

<details>
<summary>Testing Devices</summary>
The following devices are used for testing where possible

| Device                      | SoC                     | RAM  |
|-----------------------------|-------------------------|------|
| Google Pixel 8 Pro          | Tensor G3               | 12GB |
| Samsung Galaxy S21+         | Exynos 2100             | 8GB  |
| Samsung Galaxy S10          | Exynos 9820             | 8GB  |
| Samsung Galaxy S9           | Exynos 9810             | 4GB  |
| Samsung Galaxy S7           | Exynos 8890             | 4GB  |
| Samsung Galaxy A71 4G       | Snapdragon 730          | 6GB  |
| Samsung Galaxy A32 4G       | Helio G80               | 6GB  |
| Samsung Galaxy A05s         | Snapdragon 680          | 4GB  |

*The Samsung Galaxy J5 (2017) is actually a Galaxy J5 Pro, but only 2GB of RAM is enabled instead of 3GB, making it essentially a Galaxy J5 (2017).
</details>

<details>
<summary>4K60 HEVC SDR - Walk in the Park</summary>
 
**197.3MB 4K 60fps HEVC SDR video compressed using the Medium preset in Compressor.**

| Device                      | Speed    | Final Size      |
|-----------------------------|----------|-----------------|
| Google Pixel 8 Pro          | 13s 42ms | 70.1MB (-64%)   |
| Samsung Galaxy S21+ (Exynos)| 16s 50ms | 76.8MB (-61%)   |
| Samsung Galaxy S10 (Exynos) | 21s 24 ms| 78.0MB (-60%)   |
| Samsung Galaxy S9 (Exynos)  | 35s 77ms | 77.7MB (-60%)   | 
| Samsung Galaxy S7 (Exynos)  | 43s 48ms | 77.6MB (-60%)   |
| Samsung Galaxy A71 4G       | 64s 19ms | 77.4MB (-60%)   |

The following testing devices were ineligible for this benchmark
| Device                      | Reason                                    |
|-----------------------------|-------------------------------------------|
| Samsung Galaxy A32 4G       | Hardware cannot handle this video         |
| Samsung Galaxy A05s         | Hardware cannot handle this video         |
</details>

<details>
<summary>4K30 HEVC HDR10+ - Challenging Lighting</summary>

**136.8MB 4K 30fps HEVC HDR10+ video compressed using the Medium preset in Compressor.**

| Device                      | Speed    | Final Size      |
|-----------------------------|----------|-----------------|
| Google Pixel 8 Pro          | 7s 23ms  | 51.5MB (-60%)   |
| Samsung Galaxy S21+ (Exynos)| 9s 27ms  | 50.6MB (-61%)   |
| Samsung Galaxy S10 (Exynos) | 12s 03 ms| 51.2MB (-60%)   |
| Samsung Galaxy S9 (Exynos)  | 16s 59ms | 51.7MB (-60%)   | 
| Samsung Galaxy A71 4G       | 22s 84ms | 50.7MB (-61%)   |

The following testing devices were ineligible for this benchmark
| Device                      | Reason                                    |
|-----------------------------|-------------------------------------------|
| Samsung Galaxy S7 (Exynos)  | Hardware cannot handle HDR10+             |
| Samsung Galaxy A32 4G       | Hardware cannot handle this video         |
| Samsung Galaxy A05s         | Hardware cannot handle this video         |

</details>

<details>
<summary>8K24 HEVC SDR - Ultra High Resolution</summary>

**266.4MB 8K 24fps HEVC SDR video compressed using the Medium preset in Compressor.**

| Device                      | Speed    | Final Size      |
|-----------------------------|----------|-----------------|
| Google Pixel 8 Pro          | 16s 21ms | 99.5MB (-60%)   |
| Samsung Galaxy S21+ (Exynos)| 68s 27ms*| 98.9MB (-61%)   |
| Samsung Galaxy S10 (Exynos) | 38s 07ms | 100.3MB (-60%)  |

*I'm unsure what happened here, but the S21+ lags significantly when even just playing the original 8K video in the Gallery app and is only able to do 8K playback at 15fps. Perhaps Samsung's Exynos 2100 SoC has a regression in 8K decoding performance? This is an extremely strange result, but it happened across three retries and even when an active cooling source was applied.

The following testing devices were ineligible for this benchmark
| Device                      | Reason                                    |
|-----------------------------|-------------------------------------------|
| Samsung Galaxy S9 (Exynos)  | Hardware cannot handle this video         |
| Samsung Galaxy S7 (Exynos)  | Hardware cannot handle this video         |
| Samsung Galaxy A71 4G       | Hardware cannot handle this video         |
| Samsung Galaxy A32 4G       | Hardware cannot handle this video         |
| Samsung Galaxy A05s         | Hardware cannot handle this video         |

</details>

<details>
<summary>1080p60 HEVC SDR - Consistent Subject</summary>

**34.5MB 1080p 60fps HEVC SDR video compressed using the Medium preset in Compressor.**

| Device                      | Speed    | Final Size      |
|-----------------------------|----------|-----------------|
| Google Pixel 8 Pro          | 3s 45ms  | 12.7MB (-61%)   |
| Samsung Galaxy S21+ (Exynos)| 4s 73ms  | 98.9MB (-61%)   |
| Samsung Galaxy S10 (Exynos) | 5s 38ms  | 12.9MB (-60%)   |
| Samsung Galaxy S9 (Exynos)  | 9s 05ms  | 13.0MB (-60%)   |
| Samsung Galaxy S7 (Exynos)  | 17s 02ms | 12.8MB (-61%)   |
| Samsung Galaxy A71 4G       | 9s 92ms  | 12.8MB (-61%)   |
| Samsung Galaxy A32 4G       | 11s 41ms | 13.1MB (-60%)   |
| Samsung Galaxy A05s         | 22s 27ms | 12.8MB (-61%)   |

</details>

<details>
<summary>Old Benchmarks</summary>
How does Compressor run on different devices? All tests are completed with a 25 second, 200MB 4K video compressed using the Medium preset in Compressor.

| Device                      | Speed    |
|-----------------------------|----------|
| Google Pixel 8 Pro          | 11s 61ms |
| Samsung Galaxy S25          | 7s 99ms  |
| Samsung Galaxy S10 (Exynos) | 11s 27ms |
| Samsung Galaxy S8+ (Exynos) | 20s 79ms |
| Samsung Galaxy S7 (Exynos)  | 25s 35ms |

And what about Compressor vs Panda Video Compressor, a highly rated video compression app filled with ads with 10M+ downloads. These tests were done using each app on their respective medium presets.

| Device             | Compressor | Panda Video Compressor |
|--------------------|------------|------------------------|
| Google Pixel 8 Pro | 11s 61ms   |  21m 40s 49ms          |

I ran out of time waiting for my 21 minute video compression so I only ran it on my main phone, my Pixel 8 Pro. Hopefully this gives you an idea of how much faster Compressor is compared to an outdated ffmpeg wrapper using software encoding. To be precise, it's 117x faster.

</details>
<details>
<summary>Download Baseline Videos</summary>
If you would like to test Compressor on your own device, you can download the baseline videos used in the benchmarks below. The baseline videos are not for commercial use. Compressor Baseline Videos © 2026 by JoshAtticus is licensed under CC BY-NC-ND 4.0.

You can download them [here](https://l.joshattic.us/mDAc6J)
</details>
<br>

*Why are the new benchmarks worse than the old ones?* Glad you asked! The old "benchmark" video was already quite low bitrate. It was simply a video of me violently shaking my phone around for 25 seconds. The new baseline videos are much more complex and contain more detail, creating a much more accurate benchmark for real world use.

*Why have some devices been removed?* I removed the Galaxy S8+ because it was running a custom ROM which may have unfairly affected its score. The Galaxy S25 was removed as I do not have access to it anymore (I got a friend to do the testing for me, and I don't want to bother them to retest).

*Why are there new devices added?* I added the Galaxy S9 and S21+ to get a more complete picture of how Compressor performs on older flagships. The Galaxy A71, A32 and A05s were added to see how Compressor performs on older midrange devices and newer budget devices. All three of these categories make up a significant amount of my Play Store users, so I wanted to make sure Compressor performs well on them.

## Credits
Compressor wouldn't be possible without these amazing people

[@rA9stuff](https://github.com/rA9stuff) - Inspiration to create Compressor & donated

[@tgranz](https://github.com/tgranz) - Provided funding to get Compressor on Google Play

[@sirtoaks](https://github.com/sirtoaks) - Provided funding to get Compressor on Google Play

I would like to acknowledge that Compressor has used AI language models to assist in translation. Should you find any issues in translation, please open a bug report or a pull request so they can be fixed.

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=JoshAtticus/Compressor&type=Date)](https://star-history.com/#JoshAtticus/Compressor&Date)
