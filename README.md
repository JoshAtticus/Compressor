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

## Warning about malicious sites
It has come to my attention that malicious websites (specifically magiskmodule[.]gitlab[.]io and others) are writing fake articles about Compressor and linking to Ransomware/Malware instead of the real app.

Please ONLY download Compressor from:

- GitHub Releases (This repo)
- Google Play Store (Early Access)
- IzzyOnDroid
- APKMirror

If you see a "Secure Verify" or "Human Verification" page, CLOSE IT IMMEDIATELY. That is not me. Compressor is free and open source. I will never ask you to "Verify" to download.

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
How does Compressor run on different devices? All tests are completed with a 25 second, 200MB 4K video compressed using the Medium preset in Compressor.

| Device                      | Speed    |
|-----------------------------|----------|
| Google Pixel 8 Pro          | 11s 61ms |
| Samsung Galaxy S25          | 7s 99ms
| Samsung Galaxy S10 (Exynos) | 11s 27ms |
| Samsung Galaxy S8+ (Exynos) | 20s 79ms |
| Samsung Galaxy S7 (Exynos)  | 25s 35ms |

And what about Compressor vs Panda Video Compressor, a highly rated video compression app filled with ads with 10M+ downloads. These tests were done using each app on their respective medium presets.

| Device             | Compressor | Panda Video Compressor |
|--------------------|------------|------------------------|
| Google Pixel 8 Pro | 11s 61ms   |  21m 40s 49ms          |

I ran out of time waiting for my 21 minute video compression so I only ran it on my main phone, my Pixel 8 Pro. Hopefully this gives you an idea of how much faster Compressor is compared to an outdated ffmpeg wrapper using software encoding. To be precise, it's 117x faster.

## Credits
Compressor wouldn't be possible without these amazing people

[@rA9stuff](https://github.com/rA9stuff) - Inspiration to create Compressor & donated

[@tgranz](https://github.com/tgranz) - Provided funding to get Compressor on Google Play

[@sirtoaks](https://github.com/sirtoaks) - Provided funding to get Compressor on Google Play

I would like to acknowledge that Compressor has used AI language models to assist in translation. Should you find any issues in translation, please open a bug report or a pull request so they can be fixed.

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=JoshAtticus/Compressor&type=Date)](https://star-history.com/#JoshAtticus/Compressor&Date)
