import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.example.beatpulse.ui.screens.DesktopAppScreen
import com.example.beatpulse.di.DummyAppPreferences
import com.example.beatpulse.di.DummyVisualizerManager
import com.example.beatpulse.di.DummyEqualizerManager
import com.example.beatpulse.player.AppPlayer
import com.example.beatpulse.data.DesktopLibraryPlatformHelper
import com.example.beatpulse.data.DesktopLibraryScanner
import com.example.beatpulse.data.MusicRepository
import com.example.beatpulse.data.getAppDatabase
import com.example.beatpulse.ui.screens.LibraryViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.example.beatpulse.player.DesktopPlayerAdapter
import com.example.beatpulse.ui.components.player.DesktopPlayerViewModel
import com.example.beatpulse.data.OnlineMusicRepository
import org.schabi.newpipe.extractor.NewPipe
import com.example.beatpulse.data.NewPipeDownloader
import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.localization.ContentCountry
fun main() = application {
    NewPipe.init(NewPipeDownloader.getInstance(OkHttpClient.Builder()), Localization.DEFAULT, ContentCountry.DEFAULT)
    Window(onCloseRequest = ::exitApplication, title = "DuWave") {
        val prefs = DummyAppPreferences()
        
        val db = getAppDatabase()
        val libraryScanner = DesktopLibraryScanner(db.trackDao())
        val musicRepository = MusicRepository(db, libraryScanner, prefs)
        val onlineRepository = OnlineMusicRepository()
        val platformHelper = DesktopLibraryPlatformHelper()
        val libraryViewModel = LibraryViewModel(platformHelper, musicRepository, onlineRepository, prefs)
        
        val appPlayer = DesktopPlayerAdapter()
        val playerViewModel = DesktopPlayerViewModel(appPlayer, musicRepository, prefs)
        
        val visualizerManager = DummyVisualizerManager()
        val equalizerManager = DummyEqualizerManager()
        
        DesktopAppScreen(
            prefs = prefs,
            libraryViewModel = libraryViewModel,
            playerViewModel = playerViewModel,
            visualizerManager = visualizerManager,
            equalizerManager = equalizerManager,
            appPlayer = appPlayer
        )
    }
}
