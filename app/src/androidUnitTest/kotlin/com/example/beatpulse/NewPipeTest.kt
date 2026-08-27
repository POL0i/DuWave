package com.example.beatpulse

import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo

fun main() {
    NewPipe.init(org.schabi.newpipe.extractor.Downloader { request ->
        // This is a dummy test script, we just need to see if we can call it.
        // Actually it's easier to run an instrumentation test or just add a log.
        null
    })
}
