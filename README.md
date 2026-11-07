<div align="center">

<!-- Put your logo at docs/screenshots/logo.png -->
<img src="docs/screenshots/logo.png" alt="Memora logo" width="120" />

# Memora

**Never lose track of your things at home again.**

An Android app for cataloging household items across rooms and storage locations, built with Kotlin and Jetpack Compose.

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Android Studio](https://img.shields.io/badge/Android%20Studio-3DDC84?style=for-the-badge&logo=androidstudio&logoColor=white)

</div>

---

## 📖 About

Memora helps you remember where everything in your home is. Add your items, organize them by room and storage area, mark your favorites, search quickly, and review each item's history. Deleted entries are kept in an Archive so you can restore them if you change your mind.

This project was built for **ITEW 3**.

---

## ✨ Features

- 🏠 **Rooms & Storage** – organize items by room and by storage location inside each room
- 📦 **Item Management** – add, view, edit, and delete items with photos and details
- ⭐ **Favorites** – keep your most-used items one tap away
- 🔍 **Search** – find any item with search and a dedicated results screen
- 🕘 **Item History** – see what happened to each item over time
- 🗃️ **Archive** – deleted items, rooms, and storage are soft-deleted and can be restored, or permanently deleted from the Archive
- 🎨 **Smooth Navigation** – animated transitions between screens

---

## 📸 Screenshots

> Replace each image path below with your own screenshot. Keep the files inside `docs/screenshots/`.

### Home & Browsing

| Home | All Items | History | Profile |
|:---:|:---:|:---:|:---:|
| <img src="docs/screenshots/home.jpg" width="220" /> | <img src="docs/screenshots/all-items.jpg" width="220" /> | <img src="docs/screenshots/history.jpg" width="220" /> | <img src="docs/screenshots/profile.jpg" width="220" /> |
|:---:|:---:|:---:|:---:|
| <img src="docs/screenshots/home-dark.jpg" width="220" /> | <img src="docs/screenshots/all-items-dark.jpg" width="220" /> | <img src="docs/screenshots/history-dark.jpg" width="220" /> | <img src="docs/screenshots/profile-dark.jpg" width="220" /> |

### Search

| Search | Search Results | Saved |
|:---:|:---:|:---:|
| <img src="docs/screenshots/search.png" width="220" /> | <img src="docs/screenshots/search-results.png" width="220" /> | <img src="docs/screenshots/saved.png" width="220" /> |

### Items

| Item Detail | Edit Item | Add Item |
|:---:|:---:|:---:|
| <img src="docs/screenshots/item-detail.png" width="220" /> | <img src="docs/screenshots/edit-item.png" width="220" /> | <img src="docs/screenshots/add-item.png" width="220" /> |

### Rooms & Storage

| Add Room | Add Storage | History |
|:---:|:---:|:---:|
| <img src="docs/screenshots/add-room.png" width="220" /> | <img src="docs/screenshots/add-storage.png" width="220" /> | <img src="docs/screenshots/history.png" width="220" /> |

### Profile & Archive

| Profile | Archive | |
|:---:|:---:|:---:|
| <img src="docs/screenshots/profile.png" width="220" /> | <img src="docs/screenshots/archive.png" width="220" /> | |

---

## 🛠️ Tech Stack

| Category | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Navigation | Navigation Compose (`NavHost`) |
| State | `MemoraViewModel` |
| IDE | Android Studio |

---

## 🗂️ Project Structure

```
app/src/main/java/.../memora/
├── MainActivity.kt          # NavHost and app entry point
├── MemoraViewModel.kt       # App state and logic
├── Components.kt            # Reusable UI (MemoraTextField, SocialIconCircle, ...)
├── Colors.kt                # Color palette
├── ui/theme/Theme.kt        # App theme
└── screens/                 # Login, Home, Item Detail, Archive, ...
```

> Adjust the folder names to match your actual project layout.

---

## 🚀 Getting Started

### Prerequisites

- [Android Studio](https://developer.android.com/studio) (latest stable)
- Android SDK installed through Android Studio
- An emulator or a physical Android device

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/<your-username>/<your-repo>.git
   ```
2. **Open the project** in Android Studio (`File > Open`)
3. **Sync Gradle** and wait for dependencies to download
4. **Run the app** on an emulator or connected device (`Shift + F10`)

---

## 🧭 How It Works

1. **Add a room** (e.g., Kitchen, Bedroom)
2. **Add storage areas** inside that room (e.g., Top Drawer, Closet Shelf)
3. **Add items** and assign them to a room and storage location
4. **Find them later** with search, favorites, or by browsing rooms
5. **Deleted something by mistake?** Open the **Archive** to restore it

---

## 🗺️ Roadmap

- [ ] Splash screen on launch
- [ ] Live search as you type
- [ ] Clickable rooms showing their items and storage
- [ ] Editable rooms and storage areas
- [ ] Dark mode
- [ ] First-time user guide

---

## 👤 Author

**Kai**
GitHub: [@your-username](https://github.com/your-username)

---

## 📄 License

This project was created for academic purposes. Add a license here if you plan to share it publicly (e.g., [MIT](https://choosealicense.com/licenses/mit/)).
