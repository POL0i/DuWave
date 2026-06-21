package com.example.beatpulse.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val dataPath: String,
    val folderPath: String,
    val isFavorite: Boolean = false,
    val lastPlayedTime: Long = 0,
    val playCount: Int = 0,
    val dateAdded: Long = 0,
    val customTitle: String? = null,
    val customArtist: String? = null,
    val customAlbum: String? = null,
    val customCoverPath: String? = null
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val playlistId: Long = 0,
    val name: String
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"],
    foreignKeys = [
        androidx.room.ForeignKey(entity = PlaylistEntity::class, parentColumns = ["playlistId"], childColumns = ["playlistId"], onDelete = androidx.room.ForeignKey.CASCADE),
        androidx.room.ForeignKey(entity = TrackEntity::class, parentColumns = ["id"], childColumns = ["trackId"], onDelete = androidx.room.ForeignKey.CASCADE)
    ],
    indices = [androidx.room.Index("trackId")]
)
data class PlaylistTrackCrossRef(
    val playlistId: Long,
    val trackId: Long,
    val orderIndex: Int = 0
)

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks")
    fun getAllTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE isFavorite = 1")
    fun getFavorites(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE lastPlayedTime > 0 ORDER BY lastPlayedTime DESC LIMIT 50")
    fun getRecentTracks(): Flow<List<TrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTrack(track: TrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTracks(tracks: List<TrackEntity>)

    @Query("SELECT * FROM tracks WHERE playCount > 0 ORDER BY playCount DESC LIMIT 50")
    fun getTopPlayedTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY dateAdded DESC LIMIT 10")
    fun getRecentlyAddedTracks(): Flow<List<TrackEntity>>

    @Query("UPDATE tracks SET isFavorite = :isFav WHERE id = :trackId")
    fun updateFavorite(trackId: Long, isFav: Boolean)

    @Query("UPDATE tracks SET lastPlayedTime = :time, playCount = playCount + 1 WHERE id = :trackId")
    fun updateLastPlayed(trackId: Long, time: Long)

    @Query("SELECT * FROM playlists")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPlaylist(playlist: PlaylistEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertPlaylistTrack(crossRef: PlaylistTrackCrossRef)

    @Query("SELECT t.* FROM tracks t INNER JOIN playlist_tracks pt ON t.id = pt.trackId WHERE pt.playlistId = :playlistId ORDER BY pt.orderIndex ASC")
    fun getTracksForPlaylist(playlistId: Long): Flow<List<TrackEntity>>

    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = :playlistId")
    fun getPlaylistTrackCountFlow(playlistId: Long): Flow<Int>

    @Query("SELECT MAX(orderIndex) FROM playlist_tracks WHERE playlistId = :playlistId")
    fun getMaxOrderIndex(playlistId: Long): Int?

    @Query("UPDATE playlist_tracks SET orderIndex = :newOrder WHERE playlistId = :playlistId AND trackId = :trackId")
    fun updatePlaylistTrackOrder(playlistId: Long, trackId: Long, newOrder: Int)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    fun removeTrackFromPlaylist(playlistId: Long, trackId: Long)

    @Query("DELETE FROM tracks WHERE id IN (:ids)")
    fun deleteTracksById(ids: List<Long>)

    @Query("UPDATE tracks SET customTitle = :title, customArtist = :artist, customAlbum = :album, customCoverPath = :coverPath WHERE id = :id")
    fun updateTrackMetadata(id: Long, title: String?, artist: String?, album: String?, coverPath: String?)

    @Query("SELECT * FROM tracks WHERE id = :id")
    fun getTrackById(id: Long): TrackEntity?
}
