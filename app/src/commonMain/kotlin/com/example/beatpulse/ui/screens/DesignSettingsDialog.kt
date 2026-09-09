package com.example.beatpulse.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.input.key.*
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.runtime.remember
import androidx.compose.foundation.focusable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Surface
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.example.beatpulse.utils.getLocalizedString
import androidx.compose.ui.unit.dp
import com.example.beatpulse.data.AppPreferences
import com.example.beatpulse.theme.PaletteColors
import com.example.beatpulse.ui.components.backgrounds.*
import com.example.beatpulse.ui.components.player.IAudioVisualizerManager
import com.example.beatpulse.utils.getLocalizedString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.shape.GenericShape

private val DummyVisualizerManager = object : IAudioVisualizerManager {
    override val bassAmplitudes: StateFlow<FloatArray> = MutableStateFlow(FloatArray(0))
    override val midAmplitudes: StateFlow<FloatArray> = MutableStateFlow(FloatArray(0))
    override val highAmplitudes: StateFlow<FloatArray> = MutableStateFlow(FloatArray(0))
    override val combinedAmplitudes: StateFlow<FloatArray> = MutableStateFlow(FloatArray(0))
    override val isAdvancedMode: MutableStateFlow<Boolean> = MutableStateFlow(false)
    override val filterMode: MutableStateFlow<Any> = MutableStateFlow(Any())
    override val sensitivity: MutableStateFlow<Float> = MutableStateFlow(1f)
    override val reactivity: MutableStateFlow<Float> = MutableStateFlow(1f)
    override val damping: MutableStateFlow<Float> = MutableStateFlow(0.8f)
    override val bassMultiplier: MutableStateFlow<Float> = MutableStateFlow(1f)
    override val midMultiplier: MutableStateFlow<Float> = MutableStateFlow(1f)
    override val trebleMultiplier: MutableStateFlow<Float> = MutableStateFlow(1f)
    override val visualizerArchetype: MutableStateFlow<Int> = MutableStateFlow(0)
    override val fftMode: MutableStateFlow<String> = MutableStateFlow("")
    override val elementSize: MutableStateFlow<Float> = MutableStateFlow(1f)
}

// Shapes are now centralized in com.example.beatpulse.ui.utils.Shapes.kt

@Composable
fun DesignSettingsDialog(
    prefs: AppPreferences,
    paletteColors: PaletteColors,
    dynamicTextColor: Color,
    currentShapeIdx: Int,
    currentBgStyle: Int,
    onDismiss: () -> Unit
) {
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    
    androidx.compose.runtime.LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = paletteColors.dominant,
            modifier = Modifier
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.isAltPressed) {
                        false
                    } else false
                }
                .fillMaxWidth(0.95f)
                .widthIn(max = 560.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    getLocalizedString("design_settings").takeIf { it.isNotBlank() && it != "design_settings" } ?: "Ajustes de Diseño",
                    color = dynamicTextColor,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                val styles = listOf(
                    0 to (getLocalizedString("style_classic").takeIf { it != "style_classic" } ?: "Clásico"),
                    1 to (getLocalizedString("style_cyberpunk").takeIf { it != "style_cyberpunk" } ?: "Cyberpunk"),
                    2 to (getLocalizedString("style_anime").takeIf { it != "style_anime" } ?: "Anime"),
                    3 to (getLocalizedString("style_luminous").takeIf { it != "style_luminous" } ?: "Luminoso"),
                    4 to (getLocalizedString("style_kawaii").takeIf { it != "style_kawaii" } ?: "Kawaii"),
                    5 to (getLocalizedString("style_black_metal").takeIf { it != "style_black_metal" } ?: "Black Metal"),
                    6 to (getLocalizedString("style_dark_fantasy").takeIf { it != "style_dark_fantasy" } ?: "Fantasía Oscura"),
                    7 to (getLocalizedString("style_cathedral").takeIf { it != "style_cathedral" } ?: "Catedral"),
                    8 to (getLocalizedString("style_tale_legend").takeIf { it != "style_tale_legend" } ?: "Leyenda"),
                    14 to "Ojos Lullaby"
                ).map { (id, name) -> id to name.removePrefix("Estilo: ").trim() }.toMutableList()
                
                val isPatreonUnlocked by prefs.isPatreonUnlockedFlow.collectAsState()
                if (isPatreonUnlocked) {
                    styles.add(9 to getLocalizedString("patreon_wall"))
                    styles.add(10 to "Fuente Oscura")
                    styles.add(11 to "Nubes")
                    styles.add(12 to "Zen Clear")
                    styles.add(13 to "Mareas de Arena")
                    styles.add(15 to ((getLocalizedString("style_retro_crt").takeIf { it != "style_retro_crt" } ?: "Retro CRT") + "\nby Kabuto"))
                    styles.add(16 to "Procedural CRT")
                }
                
                val favoriteStyles by prefs.favoriteBackgroundStylesFlow.collectAsState()
                val sortedStyles = styles.sortedByDescending { it.first in favoriteStyles }
                val chunkedStyles = sortedStyles.chunked(4)
                val pagerState = rememberPagerState(pageCount = { chunkedStyles.size })
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(480.dp) // Más largo, ensures all shapes and styles fit well
                        .onKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown && event.isAltPressed) {
                                if (event.key == Key.DirectionRight) {
                                    val next = (pagerState.currentPage + 1) % pagerState.pageCount
                                    coroutineScope.launch { pagerState.animateScrollToPage(next) }
                                    return@onKeyEvent true
                                } else if (event.key == Key.DirectionLeft) {
                                    val prev = if (pagerState.currentPage - 1 < 0) pagerState.pageCount - 1 else pagerState.currentPage - 1
                                    coroutineScope.launch { pagerState.animateScrollToPage(prev) }
                                    return@onKeyEvent true
                                }
                            }
                            false
                        },
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left Column: Thumbnail Shapes
                    Column(
                        modifier = Modifier
                            .width(48.dp)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val shapeIndices = listOf(0, 1, 2, 3, 4, 5, 6)
                        
                        shapeIndices.forEach { idx ->
                            val isSelected = currentShapeIdx == idx
                            val shapeForThumb = com.example.beatpulse.ui.utils.getShapeForIndex(idx)
                            
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(shapeForThumb)
                                    .background(if (isSelected) paletteColors.vibrant.copy(alpha = 0.5f) else Color.Transparent)
                                    .clickable { prefs.thumbnailShape = idx }
                                    .border(
                                        width = 2.dp,
                                        color = if (isSelected) paletteColors.vibrant else dynamicTextColor.copy(alpha = 0.3f),
                                        shape = shapeForThumb
                                    )
                            )
                        }
                    }
                    
                    // Right Column: Visual Styles
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Text(
                            getLocalizedString("visual_style").takeIf { it.isNotBlank() && it != "visual_style" } ?: getLocalizedString("visual_style"),
                            color = dynamicTextColor,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        ) { page ->
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 180.dp),
                                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(chunkedStyles[page], key = { it.first }) { item ->
                                    val idx = item.first
                                    val name = item.second
                                    val isSelected = currentBgStyle == idx
                                    val isFavorite = idx in favoriteStyles
                                    
                                    StyleGridItem(
                                        idx = idx,
                                        name = name,
                                        isSelected = isSelected,
                                        isFavorite = isFavorite,
                                        onToggleFavorite = {
                                            val newFavorites = favoriteStyles.toMutableSet()
                                            if (isFavorite) newFavorites.remove(idx) else newFavorites.add(idx)
                                            prefs.favoriteBackgroundStyles = newFavorites
                                        },
                                        paletteColors = paletteColors,
                                        dynamicTextColor = dynamicTextColor,
                                        prefs = prefs,
                                        modifier = Modifier.animateItem()
                                    )
                                }
                            }
                        }
                        
                        // Dots and Close button aligned at the bottom
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Empty box for left spacing to match the close button width
                            Box(modifier = Modifier.weight(1f))
                            
                            // Dots
                            Row(
                                modifier = Modifier.weight(2f),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                repeat(chunkedStyles.size) { iteration ->
                                    val color = if (pagerState.currentPage == iteration) paletteColors.vibrant else dynamicTextColor.copy(alpha = 0.3f)
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 6.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .size(10.dp)
                                            .clickable {
                                                coroutineScope.launch { pagerState.animateScrollToPage(iteration) }
                                            }
                                    )
                                }
                            }
                            
                            // Close Button
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                                Text(
                                    text = getLocalizedString("close").takeIf { it != "close" } ?: getLocalizedString("close"),
                                    color = dynamicTextColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(dynamicTextColor.copy(alpha = 0.15f))
                                        .clickable { onDismiss() }
                                        .padding(horizontal = 24.dp, vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StyleGridItem(
    idx: Int,
    name: String,
    isSelected: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    paletteColors: PaletteColors,
    dynamicTextColor: Color,
    prefs: AppPreferences,
    modifier: Modifier = Modifier
) {
    val starScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isFavorite) 1.2f else 1.0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "starScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { prefs.backgroundStyle = idx }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(85.dp)
        ) {
            // The clipped container for the preview
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 6.dp, top = 6.dp) // Leave room for the star to bleed out
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) paletteColors.vibrant else dynamicTextColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                // Real preview using the component
                Box(modifier = Modifier.fillMaxSize()) {
                    when (idx) {
                        1 -> CyberpunkBackground(paletteColors = paletteColors, visualizerManager = DummyVisualizerManager, isPlayerScreen = false) {}
                        2 -> AnimeBackground(paletteColors = paletteColors, visualizerManager = DummyVisualizerManager, isPlayerScreen = false) {}
                        3 -> LuminousBackground(paletteColors = paletteColors, visualizerManager = DummyVisualizerManager, isPlayerScreen = false) {}
                        4 -> Y2KBackground(paletteColors = paletteColors, visualizerManager = DummyVisualizerManager, isPlayerScreen = false) {}
                        5 -> DarkAmbientBackground(paletteColors = paletteColors, visualizerManager = DummyVisualizerManager, isPlayerScreen = false) {}
                        6 -> GothicFantasyBackground(paletteColors = paletteColors, visualizerManager = DummyVisualizerManager, isPlayerScreen = false) {}
                        7 -> CathedralFantasyBackground(paletteColors = paletteColors, visualizerManager = DummyVisualizerManager, isPlayerScreen = false) {}
                        8 -> TaleLegendBackground(paletteColors = paletteColors, visualizerManager = DummyVisualizerManager, isPlayerScreen = false) {}
                        9 -> RetroWallBackground(paletteColors = paletteColors, visualizerManager = DummyVisualizerManager, isPlayerScreen = false) {}
                        10 -> FountainBackground(paletteColors = paletteColors, visualizerManager = DummyVisualizerManager, isPlayerScreen = false) {}
                        11 -> TerrariaWaterBackground(paletteColors = paletteColors, visualizerManager = DummyVisualizerManager, isPlayerScreen = false) {}
                        12 -> ZenClearBackground(paletteColors = paletteColors, visualizerManager = DummyVisualizerManager, isPlayerScreen = false) {}
                        13 -> SandsFlowBackground(paletteColors = paletteColors, visualizerManager = DummyVisualizerManager, isPlayerScreen = false) {}
                        14 -> com.example.beatpulse.ui.components.backgrounds.LullabyEyesBackground(paletteColors = paletteColors, visualizerManager = DummyVisualizerManager, isPlayerScreen = false) {}
                        15 -> com.example.beatpulse.ui.components.backgrounds.RetroCRTBackground(dominantColor = paletteColors.dominant, vibrantColor = paletteColors.vibrant, mutedColor = paletteColors.muted, dynamicEnergy = 0.5f, dynamicOffsetY = 0f, dynamicOffsetX = 0f, isPlayerScreen = false) {}
                        16 -> com.example.beatpulse.ui.components.backgrounds.ProceduralCRTCdc3rxBackground(dominantColor = paletteColors.dominant, vibrantColor = paletteColors.vibrant, mutedColor = paletteColors.muted, dynamicEnergy = 0.5f, dynamicOffsetY = 0f, dynamicOffsetX = 0f, isPlayerScreen = false) {}
                        else -> Box(modifier = Modifier.fillMaxSize().background(paletteColors.dominant))
                    }
                }
            }
            
            // The animated star placed outside the clip bounds
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "Favorite",
                tint = if (isFavorite) paletteColors.vibrant else dynamicTextColor.copy(alpha = 0.3f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-6).dp)
                    .graphicsLayer {
                        scaleX = starScale
                        scaleY = starScale
                    }
                    .size(24.dp)
                    .clickable { onToggleFavorite() }
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
            color = if (isSelected) paletteColors.vibrant else dynamicTextColor,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

