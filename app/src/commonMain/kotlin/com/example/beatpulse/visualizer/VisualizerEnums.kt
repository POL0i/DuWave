package com.example.beatpulse.visualizer

enum class FilterMode {
    ALL, BASS, MIDS, TREBLE
}

enum class PhysicsMode {
    SUAVE,       // Smooth gliding — very low reactivity, slow ghost decay
    EQUILIBRADO, // Balanced — medium reactivity, moderate ghost decay
    VIOLENTO     // Aggressive — high reactivity, fast ghost decay
}
