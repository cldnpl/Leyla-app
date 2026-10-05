package com.claudianapolitano.leyla.core

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The shared translation table is keyed by iOS's English strings, which spell
 * arguments `%@`. Android's `strings.xml` spells them `%1${'$'}s`. If this
 * mapping is wrong nothing crashes — the lookup just misses and every
 * formatted string silently stays English, which is exactly the kind of bug
 * that survives a manual pass.
 */
class PlaceholdersTest {

    @Test
    fun `a single argument becomes the bare iOS form`() {
        // iOS writes one-argument templates without an index.
        assertEquals("%@ km apart", Placeholders.toIos("%1\$s km apart"))
        assertEquals("%@ days together 💜", Placeholders.toIos("%1\$s days together 💜"))
    }

    @Test
    fun `an unindexed android argument also becomes the bare form`() {
        assertEquals("%@ questions", Placeholders.toIos("%s questions"))
    }

    @Test
    fun `several arguments keep their positions`() {
        assertEquals(
            "Day %1\$@ · next period %2\$@",
            Placeholders.toIos("Day %1\$d · next period %2\$s"),
        )
    }

    @Test
    fun `numeric and decimal arguments map like text ones`() {
        assertEquals("Week %@", Placeholders.toIos("Week %1\$d"))
        assertEquals("%1\$@ of %2\$@", Placeholders.toIos("%1\$f of %2\$d"))
    }

    @Test
    fun `a string with no arguments is untouched`() {
        assertEquals("Milestones", Placeholders.toIos("Milestones"))
        assertEquals("Milestones", Placeholders.toAndroid("Milestones"))
    }

    @Test
    fun `translations come back in a form String format understands`() {
        assertEquals("%1\$s giorni insieme 💜", Placeholders.toAndroid("%@ giorni insieme 💜"))
        assertEquals(
            "Giorno %1\$s · prossimo ciclo %2\$s",
            Placeholders.toAndroid("Giorno %1\$@ · prossimo ciclo %2\$@"),
        )
    }

    @Test
    fun `a translation that reorders its arguments keeps the mapping`() {
        // Word order differs between languages; the indices are what carry it.
        assertEquals("%2\$s in %1\$s", Placeholders.toAndroid("%2\$@ in %1\$@"))
    }

    /**
     * The round trip is what actually happens at runtime: an Android template
     * becomes a lookup key, and a translation comes back formattable.
     */
    @Test
    fun `an android template survives the round trip through the table`() {
        val android = "Day %1\$d · next period %2\$s"
        val key = Placeholders.toIos(android)
        // Stand in for the server answering in the same shape it was asked.
        val translated = Placeholders.toAndroid(key)
        assertEquals("Day %1\$s · next period %2\$s", translated)
        assertEquals("Day 14 · next period tomorrow", String.format(translated, 14, "tomorrow"))
    }

    @Test
    fun `a percent that is not an argument is left alone`() {
        assertEquals("100% yours", Placeholders.toIos("100% yours"))
        assertEquals("100% yours", Placeholders.toAndroid("100% yours"))
    }
}

/** The lookup itself: a missing key must read as English, never as blank. */
class TranslationLookupTest {

    @Test
    fun `an unknown key falls back to the english it was keyed on`() {
        assertEquals("Milestones", Translations.tr("Milestones"))
    }
}
