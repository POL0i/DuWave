package com.example.beatpulse.core.focus

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class FocusSection {
    // Player sections
    PLAYER_SONGLIST,
    PLAYER_CONTROLS,
    PLAYER_OPTIONS,
    // Library sections
    LIBRARY_TABS,
    LIBRARY_CONTENT,
    // Global lists sections
    GLOBAL_SEARCH,
    GLOBAL_LIST
}

object AppFocusManager {
    var isTabNavigationActive by mutableStateOf(false)
    var currentFocusedSection by mutableStateOf(FocusSection.PLAYER_CONTROLS)

    private val _focusActions = MutableSharedFlow<FocusAction>(
        extraBufferCapacity = 64,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val focusActions = _focusActions.asSharedFlow()

    fun cycleTabNavigation(currentPage: Int, forward: Boolean = true) {
        val validSections = when (currentPage) {
            2 -> listOf(FocusSection.PLAYER_SONGLIST, FocusSection.PLAYER_CONTROLS, FocusSection.PLAYER_OPTIONS)
            0 -> listOf(FocusSection.LIBRARY_TABS, FocusSection.LIBRARY_CONTENT)
            1 -> listOf(FocusSection.GLOBAL_SEARCH, FocusSection.GLOBAL_LIST)
            else -> emptyList()
        }
        println("DEBUG ACTION: cycleTabNavigation. currentPage=$currentPage, forward=$forward, validSections=$validSections")

        if (!isTabNavigationActive || currentFocusedSection !in validSections) {
            isTabNavigationActive = true
            currentFocusedSection = if (forward) validSections.first() else validSections.last()
            println("DEBUG ACTION: Activated tab navigation. focusedSection=$currentFocusedSection")
        } else {
            val currentIndex = validSections.indexOf(currentFocusedSection)
            val nextIndex = if (forward) {
                (currentIndex + 1) % validSections.size
            } else {
                if (currentIndex - 1 < 0) validSections.size - 1 else currentIndex - 1
            }
            currentFocusedSection = validSections[nextIndex]
            println("DEBUG ACTION: Cycled tab navigation. focusedSection=$currentFocusedSection")
        }
    }

    fun dispatchAction(action: FocusAction) {
        println("DEBUG ACTION: dispatchAction called with action: $action")
        val result = _focusActions.tryEmit(action)
        println("DEBUG ACTION: tryEmit result: $result")
    }
}

enum class FocusAction {
    NAVIGATE_UP,
    NAVIGATE_DOWN,
    NAVIGATE_LEFT,
    NAVIGATE_RIGHT,
    ACTION_ENTER
}
