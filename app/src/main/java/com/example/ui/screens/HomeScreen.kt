package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Anime
import com.example.data.Episode
import com.example.data.MockData
import com.example.data.WatchHistory
import com.example.service.FirebaseServiceHelper
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

// Custom Shimmer Modifier
fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Restart
        ), label = "shimmer_anim"
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF201A2E),
                Color(0xFF382B4E),
                Color(0xFF201A2E)
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    ).onGloballyPositioned {
        size = it.size
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreenMain(
    onNavigateToWatch: (String, String) -> Unit, // animeId, episodeId
    onLogoutNavigation: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val context = LocalContext.current

    // Offline state check
    var isOnline by remember { mutableStateOf(true) }
    LaunchedEffect(key1 = true) {
        while (true) {
            isOnline = FirebaseServiceHelper.isOnline(context)
            delay(4000) // Poll net capabilities every 4 sec
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = AnimeCardBg,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                listOf(
                    Pair("Home", Icons.Default.Home),
                    Pair("Search", Icons.Default.Search),
                    Pair("Favorites", Icons.Default.Favorite),
                    Pair("Profile", Icons.Default.Person)
                ).forEachIndexed { index, pair ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(imageVector = pair.second, contentDescription = pair.first) },
                        label = { Text(text = pair.first, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            unselectedIconColor = Color.White.copy(alpha = 0.5f),
                            selectedTextColor = AnimeAccent,
                            unselectedTextColor = Color.White.copy(alpha = 0.5f),
                            indicatorColor = AnimePrimary
                        )
                    )
                }
            }
        },
        containerColor = AnimeDarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Offline Detection Banner
            AnimatedVisibility(
                visible = !isOnline,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AnimeSecondary)
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "Offline", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Offline Mode - Showing Cached Streams",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    0 -> HomeTabContent(onNavigateToWatch)
                    1 -> SearchTabContent(onNavigateToWatch)
                    2 -> FavoritesTabContent(onNavigateToWatch)
                    3 -> ProfileTabContent(onLogoutNavigation)
                }
            }
        }
    }
}

// ==========================================
// 1. HOME TAB CONTENT
// ==========================================
@Composable
fun HomeTabContent(
    onNavigateToWatch: (String, String) -> Unit
) {
    var selectedGenre by remember { mutableStateOf("All") }
    var listLoading by remember { mutableStateOf(true) }
    val watchHistory = FirebaseServiceHelper.watchHistory.collectAsState()

    LaunchedEffect(selectedGenre) {
        listLoading = true
        delay(1000) // Aesthetic shimmer loading simulation
        listLoading = false
    }

    val filteredAnime = remember(selectedGenre) {
        if (selectedGenre == "All") {
            MockData.animeList
        } else {
            MockData.animeList.filter { it.genres.contains(selectedGenre) }
        }
    }

    val featuredAnime = remember { MockData.animeList.filter { it.isFeatured } }
    val trendingAnime = remember { MockData.animeList.filter { it.isTrending } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // App header bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // AX Premium Logo Box
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(AnimePrimary, AnimeSecondary)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "AX",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            style = androidx.compose.ui.text.TextStyle(
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        )
                    }
                    // Responsive text title
                    Row {
                        Text(
                            text = "AnimEx",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = " Pro",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = AnimeSecondary
                        )
                    }
                }
                // Immersive Glass Circle Icon Badge
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Alerts",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Featured ViewPager Banner
        item {
            FeaturedBannerSection(featuredAnime) { anime ->
                val firstEpId = anime.episodes.firstOrNull()?.id ?: "ep1"
                onNavigateToWatch(anime.id, firstEpId)
            }
        }

        // Continue Watching (Firestore backed)
        if (watchHistory.value.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(16.dp)
                                    .background(AnimeSecondary, RoundedCornerShape(100.dp))
                            )
                            Text(
                                text = "Continue Watching",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "VIEW ALL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.4f),
                            letterSpacing = 1.sp
                        )
                    }
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(watchHistory.value) { item ->
                            ContinueWatchingCard(item) {
                                onNavigateToWatch(item.animeId, item.episodeId)
                            }
                        }
                    }
                }
            }
        }

        // Genre Filter Chips
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(MockData.genres) { genre ->
                    FilterChip(
                        selected = selectedGenre == genre,
                        onClick = { selectedGenre = genre },
                        label = { Text(genre, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AnimeSecondary,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White.copy(alpha = 0.05f),
                            labelColor = Color.White.copy(alpha = 0.6f)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedGenre == genre,
                            borderColor = if (selectedGenre == genre) Color.Transparent else Color.White.copy(alpha = 0.1f),
                            selectedBorderColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(100.dp)
                    )
                }
            }
        }

        // Staggered results loading list / cards
        if (listLoading) {
            item {
                ShimmerRowPlaceholder()
            }
        } else {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(16.dp)
                                .background(AnimePrimary, RoundedCornerShape(100.dp))
                        )
                        Text(
                            text = if (selectedGenre == "All") "Trending Now" else "$selectedGenre Collection",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredAnime) { anime ->
                            AnimePosterCard(anime) {
                                val firstEpId = anime.episodes.firstOrNull()?.id ?: "ep1"
                                onNavigateToWatch(anime.id, firstEpId)
                            }
                        }
                    }
                }
            }
        }

        // Latest Releases Row
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(16.dp)
                            .background(AnimePrimary, RoundedCornerShape(100.dp))
                    )
                    Text(
                        text = "Weekly Top Hits",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(trendingAnime) { anime ->
                        AnimePosterCard(anime) {
                            val firstEpId = anime.episodes.firstOrNull()?.id ?: "ep1"
                            onNavigateToWatch(anime.id, firstEpId)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FeaturedBannerSection(
    featuredList: List<Anime>,
    onSelect: (Anime) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { featuredList.size })
    val coroutineScope = rememberCoroutineScope()

    // Auto scrolling viewpager2 equivalent loop
    LaunchedEffect(key1 = pagerState) {
        while (true) {
            delay(5000)
            val nextPage = (pagerState.currentPage + 1) % featuredList.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
        ) { page ->
            val anime = featuredList[page]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { onSelect(anime) }
            ) {
                AsyncImage(
                    model = anime.bannerUrl,
                    contentDescription = anime.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Dark Gradient Shadow Card Overlay matching theme
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.2f),
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )
                
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "NEW ARRIVAL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AnimeSecondary,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = anime.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = anime.description,
                        fontSize = 12.sp,
                        maxLines = 1,
                        color = Color.White.copy(alpha = 0.7f),
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { onSelect(anime) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(100.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Watch Now", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Rating",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = anime.rating.toString(),
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
        
        // Pager indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            featuredList.forEachIndexed { i, _ ->
                Box(
                    modifier = Modifier
                        .size(if (pagerState.currentPage == i) 16.dp else 6.dp, 6.dp)
                        .padding(horizontal = 1.dp)
                        .background(
                            color = if (pagerState.currentPage == i) AnimePrimary else Color.White.copy(alpha = 0.3f),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

@Composable
fun ContinueWatchingCard(
    history: WatchHistory,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = AnimeCardBg),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(220.dp)
            .height(115.dp)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = history.animeCoverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )
            
            // Central glassmorphic Play Button overlay
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color.White.copy(alpha = 0.25f), CircleShape)
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Save state progress overlay bar
            val progressPercent = if (history.totalSeconds > 0) {
                history.progressSeconds.toFloat() / history.totalSeconds.toFloat()
            } else 0f

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = history.animeTitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Ep ${history.episodeNumber} • ${history.progressSeconds / 60}m Left",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Progress Bar in Accent Pink
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(1.5.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressPercent.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(AnimeSecondary, RoundedCornerShape(1.5.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun AnimePosterCard(
    anime: Anime,
    onSelect: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .clickable { onSelect() }
    ) {
        Box(
            modifier = Modifier
                .height(180.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
        ) {
            AsyncImage(
                model = anime.coverUrl,
                contentDescription = anime.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Premium glassmorphic style rating tag
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = anime.rating.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = anime.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = anime.genres.firstOrNull() ?: "",
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.4f),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ShimmerRowPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(150.dp)
                .height(20.dp)
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(3) {
                Column(modifier = Modifier.width(120.dp)) {
                    Box(
                        modifier = Modifier
                            .height(160.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .shimmerEffect()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(12.dp)
                            .shimmerEffect()
                    )
                }
            }
        }
    }
}


// ==========================================
// 2. SEARCH TAB CONTENT
// ==========================================
@Composable
fun SearchTabContent(
    onNavigateToWatch: (String, String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedGenreChip by remember { mutableStateOf("All") }
    var searchResults by remember { mutableStateOf<List<Anime>>(MockData.animeList) }
    var isSearching by remember { mutableStateOf(false) }

    // SharedPreferences recent search persistence
    val context = LocalContext.current
    val recentPrefs = remember { context.getSharedPreferences("animex_recent_searches", Context.MODE_PRIVATE) }
    var recentList by remember {
        mutableStateOf(
            recentPrefs.getStringSet("searches", emptySet())?.toList() ?: emptyList()
        )
    }

    // Debounced Search Mechanism using Flow
    val searchFlow = remember { MutableStateFlow("") }
    LaunchedEffect(searchFlow) {
        searchFlow
            .debounce(500)
            .distinctUntilChanged()
            .collect { query ->
                if (query.isNotBlank()) {
                    isSearching = true
                    delay(600) // Simulated processing
                    isSearching = false
                    searchResults = MockData.animeList.filter {
                        it.title.contains(query, ignoreCase = true) ||
                        it.description.contains(query, ignoreCase = true)
                    }
                    
                    // Add to recent search persistence
                    val updatedSet = recentList.toMutableSet()
                    updatedSet.add(query)
                    recentPrefs.edit().putStringSet("searches", updatedSet).apply()
                    recentList = updatedSet.toList()
                } else {
                    searchResults = MockData.animeList
                }
            }
    }

    // Genre filter toggle updates searched list
    val finalResults = remember(searchResults, selectedGenreChip) {
        if (selectedGenreChip == "All") {
            searchResults
        } else {
            searchResults.filter { it.genres.contains(selectedGenreChip) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Debounced Search Bar Textfield
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                searchFlow.value = it
            },
            placeholder = { Text("Search Anime, Movies, Genres...", color = Color.White.copy(alpha = 0.4f)) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = AnimeSecondary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        searchQuery = ""
                        searchFlow.value = ""
                    }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                focusedBorderColor = AnimeSecondary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                cursorColor = AnimeAccent
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Genre filter categories chip row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(MockData.genres) { genre ->
                FilterChip(
                    selected = selectedGenreChip == genre,
                    onClick = { selectedGenreChip = genre },
                    label = { Text(genre, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AnimeSecondary,
                        selectedLabelColor = Color.White,
                        containerColor = Color.White.copy(alpha = 0.05f),
                        labelColor = Color.White.copy(alpha = 0.6f)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedGenreChip == genre,
                        borderColor = if (selectedGenreChip == genre) Color.Transparent else Color.White.copy(alpha = 0.1f),
                        selectedBorderColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(100.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recent Searches Block
        if (searchQuery.isEmpty() && recentList.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Searches", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    text = "Clear All", 
                    color = AnimeAccent, 
                    fontSize = 12.sp, 
                    modifier = Modifier.clickable {
                        recentPrefs.edit().remove("searches").apply()
                        recentList = emptyList()
                    }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recentList.take(5).forEach { searchString ->
                     Box(
                         modifier = Modifier
                             .background(AnimePurpleGrey, RoundedCornerShape(16.dp))
                             .clickable {
                                 searchQuery = searchString
                                 searchFlow.value = searchString
                             }
                             .padding(horizontal = 12.dp, vertical = 6.dp)
                     ) {
                         Text(searchString, color = Color.White, fontSize = 12.sp)
                     }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Search Results list or Loading/Empty States
        if (isSearching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AnimePrimary)
            }
        } else if (finalResults.isEmpty()) {
            // Elegant Native canvas-based empty search state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = AnimePrimary.copy(alpha = 0.6f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Anime Matches Here",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Try adjusting spelling or picking alternative genres.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )
                }
            }
        } else {
            // 3-Column Grid Results View utilizing LazyVerticalGrid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(finalResults) { anime ->
                    AnimePosterCard(anime) {
                        val firstEpId = anime.episodes.firstOrNull()?.id ?: "ep1"
                        onNavigateToWatch(anime.id, firstEpId)
                    }
                }
            }
        }
    }
}


// ==========================================
// 3. FAVORITES TAB CONTENT
// ==========================================
@Composable
fun FavoritesTabContent(
    onNavigateToWatch: (String, String) -> Unit
) {
    val favoritesState = FirebaseServiceHelper.favorites.collectAsState()
    val favoritedAnime = remember(favoritesState.value) {
        MockData.animeList.filter { favoritesState.value.contains(it.id) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(28.dp)
                    .background(AnimeSecondary, RoundedCornerShape(100.dp))
            )
            Column {
                Text(
                    text = "My Watchlist",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "${favoritedAnime.size} titles ready to stream",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (favoritedAnime.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        tint = AnimeAccent.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Your Watchlist is Empty", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        text = "Tap heart symbols inside any anime screen to pin items here.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(favoritedAnime) { anime ->
                    AnimePosterCard(anime) {
                        val firstEpId = anime.episodes.firstOrNull()?.id ?: "ep1"
                        onNavigateToWatch(anime.id, firstEpId)
                    }
                }
            }
        }
    }
}


// ==========================================
// 4. PROFILE TAB CONTENT
// ==========================================
@Composable
fun ProfileTabContent(
    onLogoutClick: () -> Unit
) {
    val profile = FirebaseServiceHelper.currentUser.collectAsState()
    val favorites = FirebaseServiceHelper.favorites.collectAsState()
    val watchHistory = FirebaseServiceHelper.watchHistory.collectAsState()

    val context = LocalContext.current
    var uploadLoading by remember { mutableStateOf(false) }

    // Theme values state
    val darkThemeState = ThemePreferences.isDarkMode.collectAsState()

    // Sign out confirmation dialog state
    var showLogoutModal by remember { mutableStateOf(false) }

    // Image Picker for Avatar editing
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            uploadLoading = true
            FirebaseServiceHelper.updateProfileAvatar(uri, null) { success, _, err ->
                uploadLoading = false
                if (success) {
                    Toast.makeText(context, "Profile Image Synced!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Upload Exception: $err", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    if (showLogoutModal) {
        AlertDialog(
            onDismissRequest = { showLogoutModal = false },
            title = { Text("Power Down AnimEx?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Are you certain you wish to sign out of your premium streaming terminal?", color = Color.White.copy(alpha = 0.7f)) },
            containerColor = AnimeCardBg,
            dismissButton = {
                TextButton(onClick = { showLogoutModal = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.5f))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutModal = false
                        FirebaseServiceHelper.logout()
                        onLogoutClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AnimeSecondary)
                ) {
                    Text("Sign Out", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(bottom = 36.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            // Editable Circular Profile Avatar
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AnimePurpleGrey, CircleShape)
                        .border(
                            width = 3.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(AnimePrimary, AnimeSecondary)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (uploadLoading) {
                        CircularProgressIndicator(color = AnimeAccent, modifier = Modifier.size(36.dp))
                    } else if (profile.value?.avatarUrl?.isNotEmpty() == true) {
                        AsyncImage(
                            model = profile.value?.avatarUrl,
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Text(
                            text = (profile.value?.fullName?.take(1) ?: "X").uppercase(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
                // Edit Small Icon
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(AnimeSecondary, CircleShape)
                        .border(2.dp, AnimeDarkBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Update Avatar",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = profile.value?.fullName ?: "Guest Streamer",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = profile.value?.email ?: "guest.fan@animexpro.com",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Statistics Row from local/Firestore sync states
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AnimeCardBg, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ProfileStatColumn(title = "Watched", value = watchHistory.value.size.toString())
                ProfileStatColumn(title = "Watchlist", value = favorites.value.size.toString())
                ProfileStatColumn(title = "Unlocked", value = "High-Res")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Menu Items Grid/List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AnimeCardBg, RoundedCornerShape(16.dp))
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                ProfileMenuItem(
                    icon = Icons.Default.ArrowDownward, 
                    title = "Downloads File Cache",
                    actions = { Text("0 MB", color = Color.White.copy(alpha = 0.4f), fontSize = 12.sp) }
                )
                ProfileMenuItem(
                    icon = Icons.Default.Notifications, 
                    title = "Push Feed Preferences",
                    actions = { Text("ON", color = AnimeAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                ProfileMenuItem(
                    icon = Icons.Default.Settings, 
                    title = "Midnight Anime Mode",
                    actions = {
                        Switch(
                            checked = darkThemeState.value,
                            onCheckedChange = { ThemePreferences.setDarkMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AnimeAccent,
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = AnimePurpleGrey
                            )
                        )
                    }
                )
                ProfileMenuItem(
                    icon = Icons.Default.Settings, 
                    title = "System Configuration Details",
                    onClick = { Toast.makeText(context, "AnimEx Config Terminal v1.0", Toast.LENGTH_SHORT).show() }
                )
                ProfileMenuItem(
                    icon = Icons.Default.ExitToApp, 
                    title = "De-authorize Terminal Session",
                    textColor = AnimeSecondary,
                    onClick = { showLogoutModal = true }
                )
            }
        }
    }
}

@Composable
fun ProfileStatColumn(
    title: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.5f),
            fontWeight = FontWeight.Light
        )
    }
}

@Composable
fun ProfileMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    textColor: Color = Color.White,
    actions: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = if (textColor == Color.White) AnimePrimary else textColor)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        if (actions != null) {
            actions()
        } else if (onClick != null) {
            Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.White.copy(alpha = 0.3f))
        }
    }
}
