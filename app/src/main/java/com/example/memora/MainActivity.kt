package com.example.memora
import com.example.memora.data.*
import com.example.memora.viewmodel.MemoraViewModel
import com.example.memora.ui.components.*
import com.example.memora.ui.screens.*
import com.example.memora.ui.theme.*
import com.example.memora.utils.*
import com.example.memora.worker.*

import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.WindowInsets
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.ui.res.colorResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import com.example.memora.data.ItemWithLocation
import com.example.memora.data.RoomEntity
import com.example.memora.data.StorageEntity
import com.example.memora.ui.theme.MemoraTheme
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.flowOf

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val purgeWorkRequest = PeriodicWorkRequestBuilder<ArchivePurgeWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "ArchivePurgeWork",
            ExistingPeriodicWorkPolicy.KEEP,
            purgeWorkRequest
        )

        setContent {
            // Photos fade in as they load instead of popping in while a screen is still
            // animating. Memory-cache hits are shown immediately, so revisited screens stay instant.
            setSingletonImageLoaderFactory { context ->
                ImageLoader.Builder(context).crossfade(true).build()
            }

            MemoraTheme {
                // Created here, at the activity, so every destination shares one instance. Calling
                // viewModel() inside a composable("...") block scopes it to that NavBackStackEntry
                // instead, which is what previously left the confirmation screen with no summary
                // to show and broke Add Room -> Add Storage chaining.
                val viewModel: MemoraViewModel = viewModel()
                MemoraNavHost(viewModel = viewModel)
            }
        }
    }
}

private object Routes {
    const val STARTING = "starting"

    const val HOME = "home"
    const val ALL_ITEMS = "all_items"
    const val HISTORY = "history"
    const val PROFILE = "profile"
    const val ARCHIVE = "archive"

    const val FAVORITES = "favorites"
    const val ROOMS = "rooms"
    const val STORAGES = "storages"

    const val SEARCH = "search"
    const val SEARCH_VOICE = "search_voice"
    const val SEARCH_RESULTS = "search_results"

    const val ADD = "add"
    const val ADD_ITEM_PHOTO = "add_item_photo"
    const val ADD_ITEM_DETAILS = "add_item_details"
    const val ADD_ITEM_LOCATION = "add_item_location"
    const val ADD_ROOM = "add_room"
    const val ADD_STORAGE = "add_storage"

    const val ITEM_DETAIL = "item_detail"
    const val EDIT_ITEM = "edit_item"
    const val FOUND_ITEM = "found_item"
    const val EDIT_ROOM_DETAILS = "edit_room_details"
    const val EDIT_STORAGE_DETAILS = "edit_storage_details"
    const val SAVED = "saved"

    fun itemDetail(itemId: Long) = "$ITEM_DETAIL/$itemId"
    fun editItem(itemId: Long) = "$EDIT_ITEM/$itemId"
    fun foundItem(itemId: Long) = "$FOUND_ITEM/$itemId"
    fun editRoomDetails(roomId: Long) = "$EDIT_ROOM_DETAILS/$roomId"
    fun editStorageDetails(storageId: Long) = "$EDIT_STORAGE_DETAILS/$storageId"

    /** Percent-encodes the query: a raw one containing a space or "/" produces a route that
     *  matches no destination, so searching for "house keys" used to go nowhere. */
    fun searchResults(query: String) = "$SEARCH_RESULTS/${Uri.encode(query)}"

    /** [returnToItem] marks the detour taken from the add-item flow's "+" buttons, so saving a
     *  room or storage returns to the half-finished item instead of ending the flow. */
    fun addRoom(returnToItem: Boolean = false) = "$ADD_ROOM?returnToItem=$returnToItem"
    fun addStorage(returnToItem: Boolean = false, roomId: Long? = null) =
        "$ADD_STORAGE?returnToItem=$returnToItem&roomId=${roomId ?: 0L}"
}

/**
 * Opens the add hub, remembering where the user was so the confirmation screen's DONE returns
 * there. Every save used to pop all the way to Home, which threw away the tab or the profile
 * section the flow started from.
 */
private fun NavHostController.startAddFlow(viewModel: MemoraViewModel, origin: String, route: String = Routes.ADD) {
    viewModel.setAddFlowOrigin(origin)
    navigate(route)
}

/**
 * Switches between the four bottom-nav tabs without growing the back stack.
 *
 * Tapping Home -> All Items -> History -> Profile -> All Items used to push five entries, so Back
 * walked the whole history instead of leaving the app from Home.
 */
private fun NavHostController.switchTab(route: String) {
    if (currentDestination?.route == route) return
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

// ---------------------------------------------------------------------------------- transitions

private const val FADE_MILLIS = 200
private const val DRILL_MILLIS = 260

private val TAB_ROUTES = setOf(Routes.HOME, Routes.ALL_ITEMS, Routes.HISTORY, Routes.PROFILE)

private fun NavBackStackEntry.isTab() = destination.route in TAB_ROUTES

/**
 * Between the bottom-nav tabs, and out of the splash, the new screen simply fades in. Going deeper
 * (a detail, an edit, the add flow...) also eases in from slightly to the right, which reads as
 * "forward" without the full-width slide that would drag every striped background across the
 * screen.
 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.memoraEnter(
    from: NavBackStackEntry,
    to: NavBackStackEntry
): EnterTransition {
    val flat = from.destination.route == Routes.STARTING || (from.isTab() && to.isTab())
    return if (flat) {
        fadeIn(animationSpec = tween(FADE_MILLIS))
    } else {
        fadeIn(animationSpec = tween(DRILL_MILLIS)) +
                slideInHorizontally(animationSpec = tween(DRILL_MILLIS)) { it / 12 }
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.memoraPopExit(
    from: NavBackStackEntry,
    to: NavBackStackEntry
): ExitTransition {
    val flat = from.isTab() && to.isTab()
    return if (flat) {
        fadeOut(animationSpec = tween(FADE_MILLIS))
    } else {
        fadeOut(animationSpec = tween(DRILL_MILLIS)) +
                slideOutHorizontally(animationSpec = tween(DRILL_MILLIS)) { it / 12 }
    }
}

@Composable
fun MemoraNavHost(viewModel: MemoraViewModel) {
    val navController = rememberNavController()

    // Solid backdrop matching the striped screen background. Without it the window behind the
    // NavHost (white) shows through whenever two screens are mid-fade, which was the white flash.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.memora_window_background))
    ) {
        NavHost(
            navController = navController,
            startDestination = Routes.STARTING,
            // FIX (landscape): keep content clear of the side navigation bar / camera cut-out.
            // The larger of the two side insets is used on BOTH sides so the screen (and the
            // bottom nav bar) stays symmetric instead of leaving a gap on one side only.
            modifier = if (isLandscape()) {
                val density = LocalDensity.current
                val layoutDirection = LocalLayoutDirection.current
                val insets = WindowInsets.safeDrawing
                val sideInset = with(density) {
                    maxOf(
                        insets.getLeft(density, layoutDirection),
                        insets.getRight(density, layoutDirection)
                    ).toDp()
                }
                Modifier.padding(horizontal = sideInset)
            } else {
                Modifier
            },
            // Only the incoming screen animates in. The outgoing screen stays fully opaque underneath
            // until it is covered, so nothing translucent ever lets the backdrop show through.
            // Going back mirrors this: the screen underneath is shown instantly and the one being
            // popped animates out on top of it.
            enterTransition = { memoraEnter(initialState, targetState) },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { memoraPopExit(initialState, targetState) }
        ) {

            // ------------------------------------------------------------------ starting screen

            composable(Routes.STARTING) {
                // popUpTo(inclusive) so Back from Home leaves the app rather than returning to a
                // splash the user has already seen.
                StartingScreen(
                    onFinished = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.STARTING) { inclusive = true }
                        }
                    }
                )
            }

            // ------------------------------------------------------------------ tabs

            composable(Routes.HOME) {
                val recent by viewModel.recentlyAdded.collectAsStateWithLifecycle()
                val favorites by viewModel.favorites.collectAsStateWithLifecycle()
                val rooms by viewModel.roomsWithCount.collectAsStateWithLifecycle()

                HomeScreen(
                    recentlyAdded = recent,
                    favorites = favorites,
                    rooms = rooms,
                    onItemClick = { itemId -> navController.navigate(Routes.itemDetail(itemId)) },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onAddClick = { navController.startAddFlow(viewModel, Routes.HOME) },
                    onRoomClick = { entry ->
                        viewModel.requestRoomFilter(entry.room.name)
                        navController.switchTab(Routes.ALL_ITEMS)
                    },
                    onAllItemsClick = { navController.switchTab(Routes.ALL_ITEMS) },
                    onHistoryClick = { navController.switchTab(Routes.HISTORY) },
                    onProfileClick = { navController.switchTab(Routes.PROFILE) },
                    onSearchClick = { navController.navigate(Routes.SEARCH) },
                    onVoiceSearchClick = { navController.navigate(Routes.SEARCH_VOICE) }
                )
            }

            composable(Routes.ALL_ITEMS) {
                val items by viewModel.items.collectAsStateWithLifecycle()
                val rooms by viewModel.rooms.collectAsStateWithLifecycle()
                val pendingRoomFilter by viewModel.pendingRoomFilter.collectAsStateWithLifecycle()

                // Consumed once, right after this arrival reads it as the seed below — so a room
                // tapped on Home applies here, but a later plain tab switch back to All Items
                // (bottom nav) starts from ALL again instead of reapplying a stale room.
                LaunchedEffect(Unit) {
                    if (pendingRoomFilter != null) viewModel.consumeRoomFilter()
                }

                AllItemsScreen(
                    items = items,
                    rooms = rooms,
                    initialRoomFilter = pendingRoomFilter,
                    onHomeClick = { navController.switchTab(Routes.HOME) },
                    onHistoryClick = { navController.switchTab(Routes.HISTORY) },
                    onProfileClick = { navController.switchTab(Routes.PROFILE) },
                    onAddClick = { navController.startAddFlow(viewModel, Routes.ALL_ITEMS) },
                    onItemClick = { itemId -> navController.navigate(Routes.itemDetail(itemId)) },
                    onSearchClick = { navController.navigate(Routes.SEARCH) },
                    onMicClick = { navController.navigate(Routes.SEARCH_VOICE) }
                )
            }

            composable(Routes.HISTORY) {
                val events by viewModel.events.collectAsStateWithLifecycle()

                HistoryScreen(
                    events = events,
                    onHomeClick = { navController.switchTab(Routes.HOME) },
                    onAllItemsClick = { navController.switchTab(Routes.ALL_ITEMS) },
                    onProfileClick = { navController.switchTab(Routes.PROFILE) },
                    onAddClick = { navController.startAddFlow(viewModel, Routes.HISTORY) }
                )
            }

            composable(Routes.PROFILE) {
                val profile by viewModel.profile.collectAsStateWithLifecycle()

                ProfileScreen(
                    username = profile?.username.orEmpty(),
                    photoPath = profile?.photoPath,
                    onUsernameChange = { viewModel.updateUsername(it) },
                    onPhotoChanged = { viewModel.updateProfilePhoto(it) },
                    onHomeClick = { navController.switchTab(Routes.HOME) },
                    onAllItemsClick = { navController.switchTab(Routes.ALL_ITEMS) },
                    onHistoryClick = { navController.switchTab(Routes.HISTORY) },
                    onAddClick = { navController.startAddFlow(viewModel, Routes.PROFILE) },
                    onMyRoomsClick = { navController.navigate(Routes.ROOMS) },
                    onMyStorageClick = { navController.navigate(Routes.STORAGES) },
                    onFavoritesClick = { navController.navigate(Routes.FAVORITES) },
                    onArchiveClick = { navController.navigate(Routes.ARCHIVE) }
                )
            }

            composable(Routes.ARCHIVE) {
                val archivedItems by viewModel.archivedItems.collectAsStateWithLifecycle()
                val archivedRooms by viewModel.archivedRoomsWithCount.collectAsStateWithLifecycle()
                val archivedStorages by viewModel.archivedStoragesWithRoom.collectAsStateWithLifecycle()

                ArchiveScreen(
                    archivedItems = archivedItems,
                    archivedRooms = archivedRooms,
                    archivedStorages = archivedStorages,
                    onBackClick = { navController.popBackStack() },
                    onRestoreItem = { itemId -> viewModel.restoreItem(itemId) },
                    onRestoreRoom = { roomId, restoreStorage, restoreItems ->
                        viewModel.restoreRoom(roomId, restoreStorage, restoreItems)
                    },
                    onRestoreStorage = { storageId, restoreItems ->
                        viewModel.restoreStorage(storageId, restoreItems)
                    },
                    onRestoreItemUnassigned = { itemId -> viewModel.restoreItemUnassigned(itemId) },
                    onEditItemToRestore = { itemId -> navController.navigate(Routes.editItem(itemId)) },
                    onCheckItemRestore = { itemId -> viewModel.checkItemRestoreStatus(itemId) },
                    onCheckStorageRestore = { storageId -> viewModel.checkStorageRestoreStatus(storageId) },
                    onPermanentlyDeleteItem = { itemId -> viewModel.permanentlyDeleteItem(itemId) },
                    onPermanentlyDeleteRoom = { room -> viewModel.permanentlyDeleteRoom(room) },
                    onPermanentlyDeleteStorage = { storage -> viewModel.permanentlyDeleteStorage(storage) }
                )
            }

            // ------------------------------------------------------------------ profile sections

            composable(Routes.FAVORITES) {
                val favorites by viewModel.favorites.collectAsStateWithLifecycle()
                FavoritesScreen(
                    favorites = favorites,
                    onBackClick = { navController.popBackStack() },
                    onItemClick = { itemId -> navController.navigate(Routes.itemDetail(itemId)) }
                )
            }

            composable(Routes.ROOMS) {
                val rooms by viewModel.roomsWithCount.collectAsStateWithLifecycle()
                RoomsScreen(
                    rooms = rooms,
                    onBackClick = { navController.popBackStack() },
                    onAddRoomClick = {
                        navController.startAddFlow(viewModel, Routes.ROOMS, Routes.addRoom())
                    },
                    onEditRoomClick = { room -> navController.navigate(Routes.editRoomDetails(room.id)) },
                    onDeleteRoom = { viewModel.deleteRoom(it) }
                )
            }

            composable(Routes.STORAGES) {
                val storages by viewModel.storagesWithRoom.collectAsStateWithLifecycle()
                StoragesScreen(
                    storages = storages,
                    onBackClick = { navController.popBackStack() },
                    onAddStorageClick = {
                        // A room draft abandoned earlier must not silently chain onto this storage.
                        viewModel.clearPendingRoom()
                        navController.startAddFlow(viewModel, Routes.STORAGES, Routes.addStorage())
                    },
                    onEditStorageClick = { storage ->
                        navController.navigate(Routes.editStorageDetails(storage.id))
                    },
                    onDeleteStorage = { storage, archiveItems -> viewModel.deleteStorage(storage, archiveItems) }
                )
            }

            // ------------------------------------------------------------------ search

            composable(Routes.SEARCH) {
                val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()

                // Owned here (not inside SearchScreen) so the same query drives both the field and
                // the live results query below, the same way Routes.SEARCH_RESULTS already keys its
                // own search off the route's query argument.
                var query by remember { mutableStateOf("") }
                val results by remember(query) {
                    if (query.isBlank()) flowOf(emptyList()) else viewModel.search(query)
                }.collectAsStateWithLifecycle(initialValue = emptyList())

                SearchScreen(
                    query = query,
                    onQueryChange = { query = it },
                    results = results,
                    recentSearches = recentSearches,
                    onBackClick = { navController.popBackStack() },
                    onResultClick = { itemId -> navController.navigate(Routes.itemDetail(itemId)) },
                    // Typed search no longer navigates to Routes.SEARCH_RESULTS — results already
                    // show in place — so this just keeps the term in Recent Searches.
                    onSearchSubmit = { viewModel.recordSearch(it) },
                    onMicClick = { navController.navigate(Routes.SEARCH_VOICE) }
                )
            }

            composable(Routes.SEARCH_VOICE) {
                SearchVoiceScreen(
                    onBackClick = { navController.popBackStack() },
                    onQueryCaptured = { query ->
                        viewModel.recordSearch(query)
                        navController.navigate(Routes.searchResults(query)) {
                            popUpTo(Routes.SEARCH_VOICE) { inclusive = true }
                        }
                    },
                    onTypeInsteadClick = {
                        navController.navigate(Routes.SEARCH) {
                            popUpTo(Routes.SEARCH_VOICE) { inclusive = true }
                            // Reached from Search's own mic, Search is already on the stack; without
                            // this a second copy is pushed and Back appears to do nothing.
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                route = "${Routes.SEARCH_RESULTS}/{query}",
                arguments = listOf(navArgument("query") { type = NavType.StringType })
            ) { backStackEntry ->
                val query = backStackEntry.arguments?.getString("query").orEmpty()
                val results by remember(query) { viewModel.search(query) }
                    .collectAsStateWithLifecycle(initialValue = emptyList())

                SearchResultsScreen(
                    query = query,
                    results = results,
                    onBackClick = { navController.popBackStack() },
                    onResultClick = { itemId -> navController.navigate(Routes.itemDetail(itemId)) }
                )
            }

            // ------------------------------------------------------------------ add flow

            composable(Routes.ADD) {
                AddChooserScreen(
                    onBackClick = { navController.popBackStack() },
                    onAddItemClick = {
                        viewModel.startItemDraft()
                        navController.navigate(Routes.ADD_ITEM_PHOTO)
                    },
                    onAddRoomClick = { navController.navigate(Routes.addRoom()) },
                    onAddStorageClick = {
                        // A room draft left behind by an abandoned Add Room must not chain on here.
                        viewModel.clearPendingRoom()
                        navController.navigate(Routes.addStorage())
                    }
                )
            }

            composable(Routes.ADD_ITEM_PHOTO) {
                val draft by viewModel.itemDraft.collectAsStateWithLifecycle()
                CameraCaptureScreen(
                    photoPath = draft.photoPath,
                    onBackClick = { navController.popBackStack() },
                    onPhotoCaptured = { viewModel.setDraftPhoto(it) },
                    onNextClick = { navController.navigate(Routes.ADD_ITEM_DETAILS) }
                )
            }

            composable(Routes.ADD_ITEM_DETAILS) {
                val draft by viewModel.itemDraft.collectAsStateWithLifecycle()
                AddItemDetailsScreen(
                    photoPath = draft.photoPath,
                    itemName = draft.name,
                    onNameChange = { viewModel.setDraftName(it) },
                    onBackClick = { navController.popBackStack() },
                    onNextClick = { navController.navigate(Routes.ADD_ITEM_LOCATION) }
                )
            }

            composable(Routes.ADD_ITEM_LOCATION) {
                val draft by viewModel.itemDraft.collectAsStateWithLifecycle()
                val rooms by viewModel.rooms.collectAsStateWithLifecycle()
                val allStorages by viewModel.storagesWithRoom.collectAsStateWithLifecycle()

                val selectedRoom = rooms.firstOrNull { it.id == draft.roomId }
                val storagesHere = allStorages
                    .filter { it.storage.roomId == draft.roomId }
                    .map { it.storage }
                val selectedStorage = storagesHere.firstOrNull { it.id == draft.storageId }

                RoomStorageSelectionScreen(
                    photoPath = draft.photoPath,
                    rooms = rooms,
                    storages = storagesHere,
                    selectedRoom = selectedRoom,
                    selectedStorage = selectedStorage,
                    note = draft.note,
                    onRoomSelected = { room ->
                        // Changing the room invalidates the storage, which belongs to the old one.
                        viewModel.setDraftLocation(room.id, null, draft.note)
                    },
                    onStorageSelected = { storage ->
                        viewModel.setDraftLocation(draft.roomId, storage.id, draft.note)
                    },
                    onNoteChange = { viewModel.setDraftLocation(draft.roomId, draft.storageId, it) },
                    onBackClick = { navController.popBackStack() },
                    onAddRoomClick = {
                        viewModel.clearPendingRoom()
                        navController.navigate(Routes.addRoom(returnToItem = true)) {
                            launchSingleTop = true
                        }
                    },
                    onAddStorageClick = {
                        viewModel.clearPendingRoom()
                        // Carry the room the item is already headed for, so the new storage is
                        // created somewhere the item can actually be filed.
                        navController.navigate(
                            Routes.addStorage(returnToItem = true, roomId = draft.roomId)
                        ) {
                            launchSingleTop = true
                        }
                    },
                    onSaveClick = {
                        viewModel.saveDraftItem {
                            navController.navigate(Routes.SAVED) {
                                popUpTo(viewModel.addFlowOrigin ?: Routes.HOME) { inclusive = false }
                            }
                        }
                    }
                )
            }

            composable(
                route = "${Routes.ADD_ROOM}?returnToItem={returnToItem}",
                arguments = listOf(
                    navArgument("returnToItem") { type = NavType.BoolType; defaultValue = false }
                )
            ) { backStackEntry ->
                val returnToItem = backStackEntry.arguments?.getBoolean("returnToItem") ?: false

                AddRoomScreen(
                    onBackClick = { navController.popBackStack() },
                    onSaved = { roomId, _ ->
                        if (returnToItem) {
                            // Select what was just created, so the user does not have to hunt for it
                            // in the dropdown they were sent away from.
                            val draft = viewModel.itemDraft.value
                            viewModel.setDraftLocation(roomId, null, draft.note)
                            viewModel.clearLastSaved()
                            navController.popBackStack(Routes.ADD_ITEM_LOCATION, inclusive = false)
                        } else {
                            navController.navigate(Routes.SAVED) {
                                popUpTo(viewModel.addFlowOrigin ?: Routes.HOME) { inclusive = false }
                            }
                        }
                    },
                    onAddStorage = { roomName, roomPhoto ->
                        viewModel.startStorageForNewRoom(roomName, roomPhoto)
                        navController.navigate(Routes.addStorage(returnToItem)) {
                            launchSingleTop = true
                        }
                    },
                    viewModel = viewModel
                )
            }

            composable(
                route = "${Routes.ADD_STORAGE}?returnToItem={returnToItem}&roomId={roomId}",
                arguments = listOf(
                    navArgument("returnToItem") { type = NavType.BoolType; defaultValue = false },
                    navArgument("roomId") { type = NavType.LongType; defaultValue = 0L }
                )
            ) { backStackEntry ->
                val returnToItem = backStackEntry.arguments?.getBoolean("returnToItem") ?: false
                val presetRoomId = (backStackEntry.arguments?.getLong("roomId") ?: 0L).takeIf { it > 0L }

                AddStorageScreen(
                    onBackClick = {
                        // Abandoning the screen must not leave a half-created room queued up for the
                        // next visit.
                        viewModel.clearPendingRoom()
                        navController.popBackStack()
                    },
                    onSaved = { roomId, storageId ->
                        if (returnToItem) {
                            val draft = viewModel.itemDraft.value
                            viewModel.setDraftLocation(roomId, storageId, draft.note)
                            viewModel.clearLastSaved()
                            navController.popBackStack(Routes.ADD_ITEM_LOCATION, inclusive = false)
                        } else {
                            navController.navigate(Routes.SAVED) {
                                popUpTo(viewModel.addFlowOrigin ?: Routes.HOME) { inclusive = false }
                            }
                        }
                    },
                    onAddRoom = {
                        navController.navigate(Routes.addRoom(returnToItem)) {
                            launchSingleTop = true
                        }
                    },
                    viewModel = viewModel,
                    presetRoomId = presetRoomId
                )
            }

            composable(Routes.SAVED) {
                val summary by viewModel.lastSaved.collectAsStateWithLifecycle()
                val current = summary

                if (current == null) {
                    // Belt and braces: without a summary there is nothing to confirm, and rendering
                    // nothing would strand the user on a blank screen — which is exactly what the old
                    // per-destination view model did on every single save.
                    LaunchedEffect(Unit) {
                        navController.popBackStack(
                            viewModel.addFlowOrigin ?: Routes.HOME,
                            inclusive = false
                        )
                    }
                } else {
                    SavedScreen(
                        summary = current,
                        onDoneClick = {
                            // Pop first: clearing the summary while this destination is still the
                            // current one recomposes it into the null branch, whose fallback pop
                            // would then strip the origin off the stack as well.
                            navController.popBackStack(
                                viewModel.addFlowOrigin ?: Routes.HOME,
                                inclusive = false
                            )
                            viewModel.clearLastSaved()
                        }
                    )
                }
            }

            // ------------------------------------------------------------------ item detail / edit

            composable(
                route = "${Routes.ITEM_DETAIL}/{itemId}",
                arguments = listOf(navArgument("itemId") { type = NavType.LongType })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getLong("itemId") ?: 0L
                val entry by remember(itemId) { viewModel.itemFlow(itemId) }
                    .collectAsStateWithLifecycle(initialValue = null)

                var current by remember(itemId) { mutableStateOf<ItemWithLocation?>(null) }
                LaunchedEffect(entry, itemId) {
                    if (entry != null) {
                        current = entry
                    } else if (current == null) {
                        current = viewModel.itemById(itemId)
                    }
                }

                val itemData = current ?: entry

                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.bg_stripes),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (itemData != null) {
                        ItemDetailScreen(
                            entry = itemData,
                            onBackClick = { navController.popBackStack() },
                            onEditClick = { navController.navigate(Routes.editItem(itemId)) },
                            onToggleFavorite = { viewModel.toggleFavorite(itemData) },
                            onPhotoUpdated = { newPath -> viewModel.updateItemPhoto(itemId, newPath) },
                            onFoundClick = {
                                navController.navigate(Routes.foundItem(itemId))
                            }
                        )
                    }
                }
            }

            composable(
                route = "${Routes.EDIT_ITEM}/{itemId}",
                arguments = listOf(navArgument("itemId") { type = NavType.LongType })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getLong("itemId") ?: 0L
                val entry by remember(itemId) { viewModel.itemFlow(itemId) }
                    .collectAsStateWithLifecycle(initialValue = null)
                val rooms by viewModel.rooms.collectAsStateWithLifecycle()
                val allStorages by viewModel.storagesWithRoom.collectAsStateWithLifecycle()

                var lastEntry by remember(itemId) { mutableStateOf<ItemWithLocation?>(null) }
                LaunchedEffect(entry, itemId) {
                    if (entry != null) {
                        lastEntry = entry
                    } else {
                        val fallback = viewModel.itemById(itemId)
                        if (fallback != null) lastEntry = fallback
                    }
                }

                lastEntry?.let { current ->
                    EditItemScreen(
                        entry = current,
                        rooms = rooms,
                        allStorages = allStorages,
                        onBackClick = { navController.popBackStack() },
                        onSaveConfirmed = { name, note, photoPath, roomId, storageId ->
                            viewModel.updateItem(itemId, name, note, photoPath, roomId, storageId)
                        },
                        onDeleteConfirmed = { viewModel.deleteItem(itemId) },
                        onDoneAfterSave = { navController.popBackStack() },
                        onDoneAfterDelete = {
                            // The detail screen behind this one is showing a row that no longer
                            // exists, so pop past it rather than back onto it.
                            navController.popBackStack(Routes.HOME, inclusive = false)
                        },
                        onAddRoomClick = {
                            // Returns to this Edit Item screen once the room is saved.
                            navController.startAddFlow(viewModel, "${Routes.EDIT_ITEM}/{itemId}", Routes.addRoom())
                        },
                        onAddStorageClick = { roomId ->
                            viewModel.clearPendingRoom()
                            navController.startAddFlow(
                                viewModel,
                                "${Routes.EDIT_ITEM}/{itemId}",
                                Routes.addStorage(roomId = roomId)
                            )
                        }
                    )
                }
            }

            composable(
                route = "${Routes.FOUND_ITEM}/{itemId}",
                arguments = listOf(navArgument("itemId") { type = NavType.LongType })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getLong("itemId") ?: 0L
                val entry by remember(itemId) { viewModel.itemFlow(itemId) }
                    .collectAsStateWithLifecycle(initialValue = null)

                DisposableEffect(itemId) {
                    onDispose {
                        viewModel.markFound(itemId)
                    }
                }

                FoundItemScreen(
                    itemName = entry?.item?.name.orEmpty(),
                    photoPath = entry?.item?.photoPath,
                    onSearchAnotherClick = {
                        viewModel.markFound(itemId)
                        // Clear back to Home first, then open Search fresh on top of it, so Back
                        // from Search lands on Home rather than the item that was just found.
                        navController.popBackStack(Routes.HOME, inclusive = false)
                        navController.navigate(Routes.SEARCH)
                    },
                    onBackToHomeClick = {
                        viewModel.markFound(itemId)
                        navController.popBackStack(Routes.HOME, inclusive = false)
                    }
                )
            }

            composable(
                route = "${Routes.EDIT_ROOM_DETAILS}/{roomId}",
                arguments = listOf(navArgument("roomId") { type = NavType.LongType })
            ) { backStackEntry ->
                val roomId = backStackEntry.arguments?.getLong("roomId") ?: 0L
                val rooms by viewModel.roomsWithCount.collectAsStateWithLifecycle()
                val storagesInRoom by remember(roomId) { viewModel.storagesInRoom(roomId) }
                    .collectAsStateWithLifecycle(initialValue = emptyList())

                // Keep the last row we saw: DELETE removes this room from `rooms`, and the screen
                // still needs to stay up long enough to show its "ROOM DELETED!" confirmation.
                var lastRoom by remember(roomId) { mutableStateOf<RoomEntity?>(null) }
                LaunchedEffect(rooms, roomId) {
                    rooms.firstOrNull { it.room.id == roomId }?.let { lastRoom = it.room }
                }

                lastRoom?.let { current ->
                    EditRoomDetailsScreen(
                        room = current,
                        storagesInRoom = storagesInRoom,
                        onBackClick = { navController.popBackStack() },
                        onSaveConfirmed = { name, photoPath ->
                            viewModel.updateRoom(roomId, name, photoPath)
                        },
                        onDeleteConfirmed = { viewModel.deleteRoom(current) },
                        onAddStorageClick = {
                            // Land on the Rooms list after saving the new storage, rather than back on
                            // this exact room, since addFlowOrigin only tracks plain, non-parameterized
                            // routes elsewhere in this file — tap the pencil again to keep editing.
                            navController.startAddFlow(viewModel, Routes.ROOMS, Routes.addStorage(roomId = roomId))
                        },
                        onDoneAfterSave = { navController.popBackStack() },
                        onDoneAfterDelete = {
                            navController.popBackStack(Routes.ROOMS, inclusive = false)
                        }
                    )
                }
            }

            composable(
                route = "${Routes.EDIT_STORAGE_DETAILS}/{storageId}",
                arguments = listOf(navArgument("storageId") { type = NavType.LongType })
            ) { backStackEntry ->
                val storageId = backStackEntry.arguments?.getLong("storageId") ?: 0L
                val storages by viewModel.storagesWithRoom.collectAsStateWithLifecycle()
                val rooms by viewModel.rooms.collectAsStateWithLifecycle()

                // Keep the last row we saw: DELETE removes this storage from `storages`, and the
                // screen still needs to stay up long enough to show its "STORAGE DELETED!" card.
                var lastStorage by remember(storageId) { mutableStateOf<StorageEntity?>(null) }
                LaunchedEffect(storages, storageId) {
                    storages.firstOrNull { it.storage.id == storageId }?.let { lastStorage = it.storage }
                }

                lastStorage?.let { current ->
                    EditStorageDetailsScreen(
                        storage = current,
                        rooms = rooms,
                        onBackClick = { navController.popBackStack() },
                        onSaveConfirmed = { name, photoPath, roomId ->
                            viewModel.updateStorage(storageId, name, photoPath, roomId)
                        },
                        onDeleteConfirmed = { archiveItems -> viewModel.deleteStorage(current, archiveItems) },
                        onAddRoomClick = {
                            // Land on the Storages list after saving the new room, rather than back on
                            // this exact storage, for the same route-matching reason noted on the Edit
                            // Room screen's own "+" button.
                            navController.startAddFlow(viewModel, Routes.STORAGES, Routes.addRoom())
                        },
                        onDoneAfterSave = { navController.popBackStack() },
                        onDoneAfterDelete = {
                            navController.popBackStack(Routes.STORAGES, inclusive = false)
                        }
                    )
                }
            }
        }
    }
}