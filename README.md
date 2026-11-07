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

## 📥 Download

<div align="center">

[![Download APK](https://img.shields.io/badge/Download-Memora%20APK-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/itskaiworks/Memora/releases/download/v1.0.0/app-debug.apk)

</div>

**To install on your Android phone:**

1. Tap the button above (or [click here](https://github.com/itskaiworks/Memora/releases/download/v1.0.0/app-debug.apk)) to download `app-debug.apk`
2. Open the downloaded file
3. If prompted, allow **Install unknown apps** for your browser or file manager
4. Tap **Install**, then open **Memora**

> ⚠️ This is a debug build for testing, so Android may show a security warning during install.

You can also find all versions on the [Releases page](https://github.com/itskaiworks/Memora/releases).

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

| Search | Search Results | 
|:---:|:---:|
| <img src="docs/screenshots/search.jpg" width="220" /> | <img src="docs/screenshots/search-result.jpg" width="220" /> | 
|:---:|:---:|
| <img src="docs/screenshots/search-dark.jpg" width="220" /> | <img src="docs/screenshots/search-result-dark.jpg" width="220" /> | 

### Items

| Item Detail | Edit Item | Found Item |
|:---:|:---:|:---:|
| <img src="docs/screenshots/item-detail.jpg" width="220" /> | <img src="docs/screenshots/edit-item.jpg" width="220" /> | <img src="docs/screenshots/found-item.jpg" width="220" /> |
|:---:|:---:|:---:|
| <img src="docs/screenshots/item-detail-dark.jpg" width="220" /> | <img src="docs/screenshots/edit-item-dark.jpg" width="220" /> | <img src="docs/screenshots/found-item-dark.jpg" width="220" /> |

### Adding Through Camera & Archive

| Add Option | Camera | Archive |
|:---:|:---:|:---:|
| <img src="docs/screenshots/add-option.jpg" width="220" /> | <img src="docs/screenshots/camera.jpg" width="220" /> | <img src="docs/screenshots/archive.jpg" width="220" /> |
|:---:|:---:|:---:|
| <img src="docs/screenshots/add-option-dark.jpg" width="220" /> | <img src="docs/screenshots/camera-dark.jpg" width="220" /> | <img src="docs/screenshots/archive-dark.jpg" width="220" /> |

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

## 🧭 How It Works

1. **Add a room** (e.g., Kitchen, Bedroom)
2. **Add storage areas** inside that room (e.g., Top Drawer, Closet Shelf)
3. **Add items** and assign them to a room and storage location
4. **Find them later** with search, favorites, or by browsing rooms
5. **Deleted something by mistake?** Open the **Archive** to restore it
   
---

## 📄 License

This project was created for academic purposes. ## 📄 License. This project is licensed under the [MIT License](LICENSE).
