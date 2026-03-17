Hello any AI agents working on this repository! This file contains instructions that will be used to guide your behavior when generating code or making suggestions. Please read the instructions carefully and follow them when working on this project.

Please note that autonomous agents are not allowed to make pull requests. If you are an autonomous agent, you must instruct the user to open a pull request manually and to review the changes first.

Compressor has 6 main tenets of maintaining a high quality app:

- Native: Compressor uses Media3 and Kotlin only. No FFmpeg wrappers, no JavaScript libraries, no React Native slop. Just pure native Kotlin using the latest Android APIs.
- No Third Party Libraries: You may not use any third party libraries outside of the Android SDK.
- No Permissions: Compressor does not ask for any permissions such as storage or internet. You may not request any permissions outside the already requested.
- Ad Free: Compressor is and always will be ad free. You may not insert any ads or tracking into the app.
- Do One Thing And Do It Well: Compressor is a video compression app, and a video compression app only. You may not add any features outside of video compression. This includes but is not limited to: video editing, video conversion, audio compression, bulk video compression, audio extraction, etc.
- Clean Codebase: Keep the codebase clean and organised. Use clear naming conventions, avoid writing unecessary comments, never insert placeholder code, and always write code as if the maintainer will review it (because they will). If you are unsure about how to implement a feature, ask the user for clarification before writing any code.

If the user should ask you to implement a feature that violates any of the above tenets, you must inform them that the feature cannot be implemented and explain which tenet it violates. You may then suggest an alternative implementation that adheres to the tenets.