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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloat
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
    val isDesktop = !com.example.beatpulse.utils.SystemUtils.isMobilePlatform
    
    val isPatreonUnlocked by prefs.isPatreonUnlockedFlow.collectAsState()
    val favoriteStyles by prefs.favoriteBackgroundStylesFlow.collectAsState()
    
    val list = mutableListOf(
        0 to (getLocalizedString("style_classic").takeIf { it != "style_classic" } ?: "Clásico"),
        1 to (getLocalizedString("style_cyberpunk").takeIf { it != "style_cyberpunk" } ?: "Cyberpunk"),
        2 to (getLocalizedString("style_anime").takeIf { it != "style_anime" } ?: "Anime"),
        3 to (getLocalizedString("style_luminous").takeIf { it != "style_luminous" } ?: "Luminoso"),
        4 to (getLocalizedString("style_kawaii").takeIf { it != "style_kawaii" } ?: "Kawaii"),
        5 to (getLocalizedString("style_black_metal").takeIf { it != "style_black_metal" } ?: "Black Metal"),
        6 to (getLocalizedString("style_dark_fantasy").takeIf { it != "style_dark_fantasy" } ?: "Fantasía Oscura"),
        7 to (getLocalizedString("style_cathedral").takeIf { it != "style_cathedral" } ?: "Catedral"),
        8 to (getLocalizedString("style_hearts").takeIf { it != "style_hearts" } ?: "Corazones"),
        14 to "Ojos Lullaby",
        9 to getLocalizedString("patreon_wall"),
        10 to "Fuente Oscura",
        11 to (getLocalizedString("clouds_filter").takeIf { it.isNotBlank() } ?: "Nubes"),
        12 to "Zen Clear",
        13 to "Mareas de Arena",
        15 to (getLocalizedString("style_retro_crt").takeIf { it != "style_retro_crt" } ?: "Retro CRT"),
        16 to "Procedural CRT",
        17 to "Synthwave",
        18 to "Polygon Unfold",
        19 to "Wind Waker Ocean"
    ).map { (id, name) -> id to name.removePrefix("Estilo: ").trim() }.toMutableList()
    
    val styles = list
    
    val sortedStyles = remember(favoriteStyles, styles) {
        styles.sortedByDescending { it.first in favoriteStyles }
    }
    
    val pagerState = androidx.compose.foundation.pager.rememberPagerState { (sortedStyles.size + 2) / 3 }
    
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
                    if (isDesktop && event.type == KeyEventType.KeyDown) {
                        if (event.key == Key.DirectionRight) {
                            if (pagerState.currentPage < pagerState.pageCount - 1) {
                                coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                return@onKeyEvent true
                            }
                        } else if (event.key == Key.DirectionLeft) {
                            if (pagerState.currentPage > 0) {
                                coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                return@onKeyEvent true
                            }
                        }
                    }
                    false
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
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Top Row: Thumbnail Shapes
                    Text(
                        getLocalizedString("visualizer_shape"),
                        color = dynamicTextColor,
                        style = MaterialTheme.typography.titleMedium
                    )
                    val shapeIndices = listOf(0, 1, 2, 3, 4, 5, 6)
                    val shapeKeys = listOf("thumb_shape_circle", "thumb_shape_square", "thumb_shape_rounded", "thumb_shape_squircle", "thumb_shape_cathedral", "thumb_shape_rhombus", "thumb_shape_hexagon")
                    val shapeDefaults = listOf("Círculo", "Cuadrado", "Esquinas", "Suave", "Catedral", "Rombo", "Hexágono")
                    
                    var showPatreonUnlockDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                    
                    if (showPatreonUnlockDialog) {
                        AlertDialog(
                            onDismissRequest = { showPatreonUnlockDialog = false },
                            title = { Text(getLocalizedString("patreon_exclusive_style").takeIf { it != "patreon_exclusive_style" } ?: "Estilo único para Patreons", color = paletteColors.vibrant) },
                            text = { Text(getLocalizedString("support_patreon_desc").takeIf { it != "support_patreon_desc" } ?: "Apóyanos en Patreon para desbloquear.", color = dynamicTextColor) },
                            confirmButton = {
                                TextButton(onClick = {
                                    showPatreonUnlockDialog = false
                                    onDismiss()
                                    com.example.beatpulse.core.focus.AppFocusManager.dispatchShortcut(com.example.beatpulse.core.focus.AppShortcut.OPEN_PATREON)
                                }) {
                                    Text(getLocalizedString("unlock_patreon").takeIf { it != "unlock_patreon" } ?: "Desbloquear", color = paletteColors.vibrant)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showPatreonUnlockDialog = false }) {
                                    Text(getLocalizedString("cancel").takeIf { it != "cancel" } ?: "Cancelar", color = dynamicTextColor.copy(alpha=0.6f))
                                }
                            },
                            containerColor = paletteColors.dominant
                        )
                    }

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        items(shapeIndices.size) { index ->
                            val idx = shapeIndices[index]
                            val isSelected = currentShapeIdx == idx
                            val shapeForThumb = com.example.beatpulse.ui.utils.getShapeForIndex(idx)
                            val isLocked = (idx == 5 || idx == 6) && !isPatreonUnlocked
                            
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(shapeForThumb)
                                        .background(if (isSelected) paletteColors.vibrant.copy(alpha = 0.5f) else Color.Transparent)
                                        .clickable { 
                                            if (isLocked) {
                                                showPatreonUnlockDialog = true
                                            } else {
                                                prefs.thumbnailShape = idx 
                                            }
                                        }
                                        .border(
                                            width = 2.dp,
                                            color = if (isSelected) paletteColors.vibrant else dynamicTextColor,
                                            shape = shapeForThumb
                                        )
                                ) {
                                    if (isLocked) {
                                        Box(
                                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Lock,
                                                contentDescription = "Patreon Locked",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = getLocalizedString(shapeKeys[index]).takeIf { it != shapeKeys[index] } ?: shapeDefaults[index],
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) paletteColors.vibrant else dynamicTextColor.copy(alpha=0.8f),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    
                    // Bottom Row: Visual Styles
                    Text(
                        getLocalizedString("visual_style").takeIf { it.isNotBlank() && it != "visual_style" } ?: "Estilo Visual",
                        color = dynamicTextColor,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Column(modifier = Modifier.fillMaxWidth()) {
                        androidx.compose.foundation.pager.HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxWidth(),
                        ) { page ->
                            val startIdx = page * 3
                            val endIdx = kotlin.math.min(startIdx + 3, sortedStyles.size)
                            val pageItems = sortedStyles.subList(startIdx, endIdx)
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                for (i in 0..2) {
                                    if (i < pageItems.size) {
                                        val item = pageItems[i]
                                        val idx = item.first
                                        val name = item.second
                                        val isSelected = currentBgStyle == idx
                                        val isFavorite = idx in favoriteStyles
                                        
                                        StyleGridItem(
                                            idx = idx,
                                            name = name,
                                            isSelected = isSelected,
                                            isFavorite = isFavorite,
                                            isLocked = (idx in 9..16 && idx != 14) && !isPatreonUnlocked,
                                            onToggleFavorite = {
                                                val newFavorites = favoriteStyles.toMutableSet()
                                                if (isFavorite) newFavorites.remove(idx) else newFavorites.add(idx)
                                                prefs.favoriteBackgroundStyles = newFavorites
                                            },
                                            onLockedClick = { showPatreonUnlockDialog = true },
                                            paletteColors = paletteColors,
                                            dynamicTextColor = dynamicTextColor,
                                            prefs = prefs,
                                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f).padding(horizontal = 4.dp))
                                    }
                                }
                            }
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
                            val dotSize = if (isDesktop) 10.dp else 5.dp
                            val dotPadding = if (isDesktop) 6.dp else 2.dp
                            repeat(pagerState.pageCount) { iteration ->
                                val indicatorColor = if (pagerState.currentPage == iteration) paletteColors.vibrant else dynamicTextColor.copy(alpha = 0.2f)
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = dotPadding)
                                        .size(dotSize)
                                        .clip(CircleShape)
                                        .background(indicatorColor)
                                        .then(if (isDesktop) Modifier.clickable { coroutineScope.launch { pagerState.animateScrollToPage(iteration) } } else Modifier)
                                )
                            }
                        }
                    }
                    
                    // Close Button
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.CenterEnd) {
                        Text(
                            text = getLocalizedString("close").takeIf { it != "close" } ?: "Cerrar",
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

@Composable
fun StyleGridItem(
    idx: Int,
    name: String,
    isSelected: Boolean,
    isFavorite: Boolean,
    isLocked: Boolean = false,
    onToggleFavorite: () -> Unit,
    onLockedClick: () -> Unit = {},
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
    
    val animatedGradientBrush = androidx.compose.ui.graphics.Brush.sweepGradient(
        colors = listOf(paletteColors.dominant, paletteColors.vibrant, paletteColors.muted, paletteColors.dominant),
        center = androidx.compose.ui.geometry.Offset(12f, 12f)
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { 
                if (isLocked) onLockedClick() else prefs.backgroundStyle = idx 
            }
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
                        17 -> SynthwaveBackground(paletteColors = paletteColors, visualizerManager = DummyVisualizerManager, isPlayerScreen = false) {}
                        18 -> PolygonUnfoldBackground(paletteColors = paletteColors, visualizerManager = DummyVisualizerManager, isPlayerScreen = false) {}
                        19 -> WindWakerOceanBackground(paletteColors = paletteColors, visualizerManager = DummyVisualizerManager, isPlayerScreen = false) {}
                        else -> Box(modifier = Modifier.fillMaxSize().background(paletteColors.dominant))
                    }
                }
                
                if (isLocked) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Patreon Locked",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            // The animated star placed outside the clip bounds
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-6).dp)
                    .graphicsLayer {
                        scaleX = starScale
                        scaleY = starScale
                    }
                    .size(24.dp)
                    .clickable { onToggleFavorite() }
            ) {
                drawContext.canvas.save()
                val pxCenterX = center.x
                val pxCenterY = center.y
                
                val path = androidx.compose.ui.graphics.vector.PathParser().parsePathString("M12,17.27L18.18,21L16.54,13.97L22,9.24L14.81,8.62L12,2L9.19,8.62L2,9.24L7.45,13.97L5.82,21L12,17.27Z").toPath()
                if (isFavorite) {
                    drawPath(
                        path = path,
                        brush = animatedGradientBrush
                    )
                } else {
                    drawPath(
                        path = path,
                        color = dynamicTextColor.copy(alpha = 0.3f)
                    )
                }
                
                drawContext.canvas.restore()
            }
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

