# RootShare Mobile

 Created by

>Nitzan Avargil nitzanpro883@gmail.com (322888769).

> Nir Shitrit nirvsgov@gmail.com (313526642).

Android mobile client for **RootShare** - a plant-sharing social network where users can share their plants, swap cuttings, and connect with fellow plant enthusiasts.

> College of management project - Android Development course, by Tal Zion.

## Tech Stack

- **Language:** Kotlin
- **UI:** XML Layouts + ViewBinding
- **Architecture:** MVVM (ViewModel + LiveData + Repository)
- **Local Database:** Room 2.6.1 (KSP) — Single Source of Truth (offline-first)
- **Networking:** Retrofit + OkHttp + Gson
- **Auth:** JWT (DataStore) + Google Sign-In (Credential Manager)
- **Image Loading:** Glide
- **Navigation:** Navigation Component + SafeArgs
- **Build:** AGP 8.13.2, Kotlin 2.0.21, KSP 2.0.21-1.0.28

## Architecture

The app follows **MVVM + Offline-First** architecture. Room is the single source of truth — data is fetched from the API, stored locally in Room, and UI observes Room LiveData for updates. This guarantees consistent state and enables offline access to previously loaded data.

**Data flow:**
1. Fragment calls a ViewModel method
2. ViewModel delegates to a Repository
3. Repository fetches from API → saves to Room
4. Room LiveData emits → ViewModel LiveData updates → Fragment re-renders

## Project Structure

```
app/src/main/java/com/example/rootsharemobile/
├── data/
│   ├── auth/              # Google Sign-In helper (Credential Manager)
│   ├── local/
│   │   ├── TokenManager   # JWT token + user cache (DataStore)
│   │   └── db/
│   │       ├── AppDatabase    # Room database singleton
│   │       ├── dao/           # UserDao, PlantDao, PostDao
│   │       └── entity/        # UserEntity, PlantEntity, PostEntity
│   ├── model/             # API response data classes (Plant, Post, User, Auth)
│   ├── remote/            # ApiService, RetrofitClient, ApiConfig, TokenRefreshAuthenticator
│   └── repository/        # AuthRepository, PlantRepository, PostRepository
├── ui/
│   ├── adapter/           # RecyclerView adapters (ListAdapter + DiffUtil)
│   ├── fragment/
│   │   ├── auth/          # Login & Register screens
│   │   ├── home/          # Home feed screen
│   │   ├── profile/       # User profile & post management
│   │   ├── garden/        # Plant details & CRUD
│   │   └── shared/        # My Garden, Community (shared tabs)
│   └── viewmodel/         # AuthViewModel, HomeViewModel, MyGardenViewModel, MyPostsViewModel
└── MainActivity.kt        # Single-Activity host with NavHostFragment
```

## Fragments

| Fragment | Location | Description |
|---|---|---|
| **LoginFragment** | `fragment/auth/` | Login screen with email/password and Google Sign-In. Observes auth state and auto-navigates to Home if already logged in. Passes username to Home via SafeArgs. |
| **RegisterFragment** | `fragment/auth/` | Registration screen with username, email, password fields and Google Sign-Up. Redirects to Login on success. |
| **HomeFragment** | `fragment/home/` | Main feed screen showing a welcome banner, a horizontal list of featured plants, and a vertical community feed. Supports swipe-to-refresh. |
| **ProfileFragment** | `fragment/profile/` | User profile with avatar (camera/gallery upload), editable username, stats (plants, posts, member since), a grid of the user's posts with edit/delete actions, and a FAB to create new posts. |
| **MyGardenFragment** | `fragment/shared/` | Displays the user's plant collection in a 2-column grid. Supports add (via bottom sheet), swipe-to-refresh, and navigates to PlantDetailsFragment on tap. |
| **PlantDetailsFragment** | `fragment/garden/` | Full detail view for a single plant showing image, name, species, health status badge, post count, and added date. Supports edit (bottom sheet) and delete with confirmation. |
| **CommunityFragment** | `fragment/shared/` | Placeholder for the full community feed (future iteration). |

## Adapters

| Adapter | Description |
|---|---|
| **FeaturedPlantsAdapter** | Horizontal `ListAdapter` for the Home screen's featured plants carousel. Displays plant image, name, category, and a badge. Uses Glide for image loading. |
| **FeedPostAdapter** | Vertical `ListAdapter` for community feed posts on the Home screen. Shows post type badge, content, optional plant name, like/comment counts, timestamp, and post image. |
| **GardenPlantAdapter** | Grid `ListAdapter` for the My Garden screen. Each card shows plant image, name, species, colour-coded health status badge, and associated post count. |
| **GridCardAdapter** | Generic reusable grid `ListAdapter` used by both My Garden and Profile posts grid. Renders a card with image, title, subtitle, badge, and optional counter. |

All adapters use `DiffUtil` for efficient RecyclerView updates when Room LiveData emits new lists.

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
