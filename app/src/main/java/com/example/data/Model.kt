package com.example.data

data class Anime(
    val id: String,
    val title: String,
    val description: String,
    val rating: Double,
    val coverUrl: String,
    val bannerUrl: String,
    val genres: List<String>,
    val episodes: List<Episode>,
    val releaseYear: Int,
    val isTrending: Boolean,
    val isFeatured: Boolean
)

data class Episode(
    val id: String,
    val title: String,
    val duration: String,
    val thumbnailUrl: String,
    val videoUrl: String,
    val episodeNumber: Int,
    val synopsis: String
)

data class WatchHistory(
    val animeId: String,
    val animeTitle: String,
    val animeCoverUrl: String,
    val episodeId: String,
    val episodeNumber: Int,
    val progressSeconds: Long,
    val totalSeconds: Long,
    val timestamp: Long
)

object MockData {
    val genres = listOf("All", "Action", "Adventure", "Fantasy", "Sci-Fi", "Drama", "Shonen")

    val mockEpisodes = listOf(
        Episode(
            id = "ep1",
            title = "The Journey Begins",
            duration = "10:11",
            thumbnailUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=500&auto=format&fit=crop",
            videoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            episodeNumber = 1,
            synopsis = "The mysterious journey starts as the young protagonist discovers an ancient scroll of endless powers."
        ),
        Episode(
            id = "ep2",
            title = "Awakening of Fire",
            duration = "09:53",
            thumbnailUrl = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=500&auto=format&fit=crop",
            videoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            episodeNumber = 2,
            synopsis = "While under attack by the shadow minions, our hero unleashes a dormant flame dragon from within."
        ),
        Episode(
            id = "ep3",
            title = "Clash of Sovereigns",
            duration = "14:20",
            thumbnailUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=500&auto=format&fit=crop",
            videoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            episodeNumber = 3,
            synopsis = "An encounter on the ancient celestial bridge forces a confrontation between the two major factions."
        ),
        Episode(
            id = "ep4",
            title = "The Forbidden Gate",
            duration = "15:02",
            thumbnailUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=500&auto=format&fit=crop",
            videoUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            episodeNumber = 4,
            synopsis = "Ignoring warnings, the team breaches the dimensional portal to retrieve the fallen star shard."
        )
    )

    val animeList = listOf(
        Anime(
            id = "a1",
            title = "Demon Slayer: Hinokami",
            description = "A young man embarks on a quest to cure his sister and avenge his family, training hard to join the legendary organization of demon hunters.",
            rating = 9.4,
            coverUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=500&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1626544827763-d516dce335e2?w=1000&auto=format&fit=crop",
            genres = listOf("Action", "Fantasy", "Shonen"),
            episodes = mockEpisodes,
            releaseYear = 2024,
            isTrending = true,
            isFeatured = true
        ),
        Anime(
            id = "a2",
            title = "Jujutsu Kaisen: Cursed Realm",
            description = "In a world of curses, high school boy Yuji Itadori swallows a legendary cursed finger to save his classmates, becoming bound to the ancient Curse King.",
            rating = 9.2,
            coverUrl = "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=500&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1000&auto=format&fit=crop",
            genres = listOf("Action", "Sci-Fi", "Shonen"),
            episodes = mockEpisodes.map { it.copy(id = it.id + "_a2", episodeNumber = it.episodeNumber) },
            releaseYear = 2024,
            isTrending = true,
            isFeatured = false
        ),
        Anime(
            id = "a3",
            title = "Chronicles of Frieren",
            description = "The beautiful and melancholic journey of an immortal elven mage who reflects on the brevity of human life after defeating the Demon King with her guild.",
            rating = 9.6,
            coverUrl = "https://images.unsplash.com/photo-1541701494587-cb58502866ab?w=500&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=1000&auto=format&fit=crop",
            genres = listOf("Adventure", "Fantasy", "Drama"),
            episodes = mockEpisodes.map { it.copy(id = it.id + "_a3", episodeNumber = it.episodeNumber) },
            releaseYear = 2023,
            isTrending = true,
            isFeatured = false
        ),
        Anime(
            id = "a4",
            title = "Cyberpunk: Neon Runners",
            description = "A street kid trying to survive in a technology and body modification-obsessed city of the future. Having everything to lose, he chooses to stay alive by becoming an edgerunner.",
            rating = 8.9,
            coverUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=500&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1511512578047-dfb367046420?w=1000&auto=format&fit=crop",
            genres = listOf("Sci-Fi", "Action", "Drama"),
            episodes = mockEpisodes.map { it.copy(id = it.id + "_a4", episodeNumber = it.episodeNumber) },
            releaseYear = 2024,
            isTrending = false,
            isFeatured = false
        ),
        Anime(
            id = "a5",
            title = "Attack on Titan: The Last Stand",
            description = "In a city surrounded by high walls, Eren Jaeger vows to wipe out every single colossal Titan after witnessing the tragic loss of his mother.",
            rating = 9.5,
            coverUrl = "https://images.unsplash.com/photo-1533134242443-d4fd215305ad?w=500&auto=format&fit=crop",
            bannerUrl = "https://images.unsplash.com/photo-1579783902614-a3fb3927b6a5?w=1000&auto=format&fit=crop",
            genres = listOf("Action", "Drama", "Shonen"),
            episodes = mockEpisodes.map { it.copy(id = it.id + "_a5", episodeNumber = it.episodeNumber) },
            releaseYear = 2023,
            isTrending = false,
            isFeatured = true
        )
    )
}
