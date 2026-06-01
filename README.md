# Couple Canvas

Couple Canvas is an Android app for couples to share live drawings, notes, memories, date plans, quizzes, Daily Spark answers, and widgets through Firebase.

The repository does not include private Firebase project files. You create your own Firebase project, add the local config files, and run the app.

## What Is Included

- Kotlin + Jetpack Compose Android app
- Firebase Authentication with Google sign-in
- Firebase Realtime Database room sync and drawing sync
- Firebase Storage for selected images and snapshots
- Screen overlay drawing mode
- Jetpack Glance widgets
- Firebase Database and Storage rules
- Unit and instrumentation test sources

## What Is Ignored

These files stay local and must not be committed:

- `.env`, `.env.*`
- `.firebaserc`
- `local.properties`
- `app/google-services.json`
- `app/google-services.json.*`
- `GoogleService-Info.plist`
- Firebase service account JSON files
- Firebase logs, emulator exports, Gradle build outputs, and local agent state

Examples are committed instead:

- `.firebaserc.example`
- `.env.example`
- `app/google-services.example.json`

## Requirements

- Android Studio
- JDK 17
- Firebase CLI
- A Firebase project with:
  - Authentication
  - Google sign-in provider
  - Realtime Database
  - Storage

## Setup

1. Clone the repo.

   ```bash
   git clone https://github.com/heodongun/lovedraw.git
   cd lovedraw
   ```

2. Create a Firebase project in the Firebase Console.

3. Add an Android app in Firebase.

   Use this package name:

   ```text
   com.example.couplecanvas
   ```

4. Download `google-services.json` from Firebase Console.

   Put it here:

   ```text
   app/google-services.json
   ```

5. Create `.firebaserc` from the example.

   ```bash
   cp .firebaserc.example .firebaserc
   ```

   Replace `your-firebase-project-id` with your Firebase project ID.

6. Add the database URL to `local.properties`.

   Keep the existing Android SDK line, then add:

   ```properties
   COUPLE_CANVAS_DATABASE_URL=https://your-project-id-default-rtdb.firebaseio.com
   ```

   If your Realtime Database uses a regional URL, use that exact URL from Firebase Console, for example:

   ```properties
   COUPLE_CANVAS_DATABASE_URL=https://your-project-id-default-rtdb.asia-southeast1.firebasedatabase.app
   ```

   Optional: if you want the debug app to use local Firebase emulators, add:

   ```properties
   COUPLE_CANVAS_USE_FIREBASE_EMULATORS=true
   ```

7. Enable Google sign-in.

   In Firebase Console:

   - Open Authentication
   - Open Sign-in method
   - Enable Google
   - Add the SHA-1 debug fingerprint if Google sign-in does not open correctly

   Get the debug SHA-1:

   ```bash
   ./gradlew signingReport
   ```

8. Deploy Firebase rules.

   ```bash
   firebase login
   firebase use your-firebase-project-id
   firebase deploy --only database,storage
   ```

9. Build the app.

   ```bash
   ./gradlew :app:assembleDebug
   ```

10. Run tests.

    ```bash
    ./gradlew :app:testDebugUnitTest
    ```

## Firebase Emulator

For local rule testing, first enable emulator mode in `local.properties`:

```properties
COUPLE_CANVAS_USE_FIREBASE_EMULATORS=true
```

Then run:

```bash
firebase emulators:start --config firebase.debug.json --only auth,database,storage
```

In another terminal:

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Optional shell variables for `scripts/verify_firebase_rules.sh` are documented in `.env.example`.

## Notes For Development

- Real Firebase credentials are intentionally ignored.
- Do not commit `app/google-services.json`.
- Do not commit `.firebaserc` if it points to a real project.
- Use `app/google-services.example.json` only as a shape reference.
- The debug build uses your real Firebase project by default.
- Set `COUPLE_CANVAS_USE_FIREBASE_EMULATORS=true` only when running local emulators.
