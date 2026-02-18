# RootShare Mobile
 
 Created by

>Nitzan Avargil nitzanpro883@gmail.com (322888769).

> Nir Shitrit nirvsgov@gmail.com (313526642).

Android mobile client for **RootShare** - a plant-sharing social network where users can share their plants, swap cuttings, and connect with fellow plant enthusiasts.

Built with Jetpack Compose, Kotlin, and MVVM architecture.

> College of management project - Android Development course, by Tal Zion.

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Architecture:** MVVM (ViewModel + LiveData)
- **Networking:** Retrofit + OkHttp
- **Auth:** Google Sign-In (Credential Manager) + local email/password
- **Image Loading:** Coil
- **Navigation:** Navigation Compose
- **Storage:** DataStore Preferences

## Quick Setup from Scratch

### Prerequisites

- [Android Studio](https://developer.android.com/studio) (Ladybug or newer)
- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- [Node.js](https://nodejs.org/) (v18+)
- Git

### 1. Clone the Repos

```bash
git clone <rootshare-mobile-repo-url>
git clone <rootshare-webapp-repo-url>
```

### 2. Start the Database (Docker)

From the **RootShareWebApp** directory, spin up MongoDB:

```bash
cd RootShareWebApp
docker compose up -d
```

This starts a MongoDB 7.0 container on port `27017`.

### 3. Start the Backend Server

Still in **RootShareWebApp**, install dependencies and start the server:

```bash
npm install
npm run dev
```

The API server should now be running on `http://localhost:3000`.

> For full web app setup details (environment variables, Google OAuth config, etc.), refer to the **RootShareWebApp** README.

### 4. Configure `local.properties`

In the **RootShareMobile** project root, open (or create) `local.properties` and add:

```properties
GOOGLE_WEB_CLIENT_ID=your-google-web-client-id-here
API_BASE_URL=http://10.0.2.2:3000/api/
```
* Note that 10.0.2.2 is the localhost for Android Studio, and 3000 is the default API port.

| Property | Description |
|---|---|
| `GOOGLE_WEB_CLIENT_ID` | Your Google OAuth Web Client ID from the Google Cloud Console. Required for Google Sign-In. |
| `API_BASE_URL` | The backend API URL. `10.0.2.2` is the Android emulator alias for `localhost`. |

> `local.properties` is git-ignored.

### 5. Build & Run

1. Open the project in Android Studio
2. Sync Gradle
3. Run on an emulator or physical device (min SDK 24)

## Quick Setup with Seed Data

To populate the app with sample users, plants, and posts so you have content to see right away:

### From the **RootShareWebApp** directory:

**macOS / Linux:**
```bash
npm run seed
```

**Windows:**
```bash
npm run seed:win
```

This creates:
- **7 test users** with profile pictures
- **14 plants** (2 per user) with images
- **14 posts** (updates, swaps, giveaways) with likes
- Ready-to-use feed content

### Test Login

After seeding, you can log in with:

```
Email:    emma.green@example.com
Password: Password123!
```

## Project Structure

```
app/src/main/java/com/example/rootsharemobile/
├── data/           # API services, models, repositories
├── ui/             # Compose screens & components
├── viewmodel/      # ViewModels (MVVM)
└── utils/          # Helpers & utilities
```
