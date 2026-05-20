package com.example.model

object TagExtractor {
    fun extractTags(title: String, artist: String): List<String> {
        val tags = mutableSetOf<String>()
        val text = "$title $artist".lowercase()
        
        if (text.contains("lofi") || text.contains("lo-fi") || text.contains("chill") || text.contains("relax") || text.contains("study") || text.contains("jazzhop")) {
            tags.add("Lo-Fi")
        }
        if (text.contains("j-pop") || text.contains("jpop") || text.contains("japanese") || text.contains("anime") || text.contains("vocaloid") || text.contains("j-rock") || text.contains("radwimps") || text.contains("yoasobi")) {
            tags.add("J-Pop")
        }
        if (text.contains("k-pop") || text.contains("kpop") || text.contains("korean") || text.contains("bts") || text.contains("blackpink")) {
            tags.add("K-Pop")
        }
        if (text.contains("metal") || text.contains("deathmetal") || text.contains("thrash") || text.contains("slipknot") || text.contains("metallica") || text.contains("eclipse") || text.contains("iron")) {
            tags.add("Metal")
        }
        if (text.contains("rock") || text.contains("alternative") || text.contains("punk") || text.contains("grunge")) {
            tags.add("Rock")
        }
        if (text.contains("pop") || text.contains("top hits") || text.contains("radio") || text.contains("billboard") || text.contains("star")) {
            tags.add("Pop")
        }
        if (text.contains("rap") || text.contains("hiphop") || text.contains("hip-hop") || text.contains("trap") || text.contains("beat")) {
            tags.add("Hip Hop")
        }
        if (text.contains("synthwave") || text.contains("vaporwave") || text.contains("retro") || text.contains("electronic") || text.contains("edm") || text.contains("techno") || text.contains("house") || text.contains("cyber") || text.contains("cruise")) {
            tags.add("Electronic")
        }
        if (text.contains("jazz") || text.contains("blues") || text.contains("sax") || text.contains("blue note")) {
            tags.add("Jazz")
        }
        if (text.contains("classical") || text.contains("piano") || text.contains("violin") || text.contains("orchestra") || text.contains("sonata") || text.contains("amadeus")) {
            tags.add("Classical")
        }
        if (text.contains("gaming") || text.contains("cyberpunk") || text.contains("synth") || text.contains("glitch")) {
            tags.add("Synth")
        }

        // Assign deterministic tags based on hash to ensure every track always has tags
        if (tags.isEmpty()) {
            val hash = kotlin.math.abs((title + artist).hashCode())
            val defaultTags = listOf("Lo-Fi", "Pop", "Electronic", "Rock", "J-Pop", "Hip Hop", "Jazz", "Classical")
            tags.add(defaultTags[hash % defaultTags.size])
            tags.add(defaultTags[(hash + 3) % defaultTags.size])
        }

        return tags.toList()
    }
}
