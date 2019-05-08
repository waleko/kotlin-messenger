package ru.kotlin566.messenger.android_client.dummy

import java.util.ArrayList
import java.util.HashMap
import kotlin.random.Random.Default.nextInt

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 *
 * TODO: Replace all uses of this class before publishing your app.
 */
object DummyContent {

    /**
     * An array of sample (dummy) items.
     */
    val ITEMS: MutableList<DummyItem> = ArrayList()

    /**
     * A map of sample (dummy) items, by ID.
     */
    val ITEM_MAP: MutableMap<String, DummyItem> = HashMap()

    private val COUNT = 25

    init {
        // Add some sample items.
        for (i in 1..COUNT) {
            addItem(createDummyItem(i))
        }
    }


    private fun addItem(item: DummyItem) {
        ITEMS.add(item)
        ITEM_MAP.put(item.dispayName, item)
    }

    private fun createDummyItem(position: Int): DummyItem {
        return DummyItem(position.toString(),
            "Hello World! Lorem ipsum and so on",
            makeTime(),
            "@tools:sample/avatars")
    }

    private fun makeTime(): String {
        val builder = StringBuilder()
        var c = nextInt(24)
        if(c < 10)
            builder.append('0')
        builder.append(c)
        builder.append(':')
        c = nextInt(24)
        if(c < 10)
            builder.append('0')
        builder.append(c)
        return builder.toString()
    }

    /**
     * A dummy item representing a piece of content.
     */
    data class DummyItem(val dispayName: String, val messageText: String, val messageTime: String, val profilePicPath: String) {
        override fun toString(): String = dispayName
    }
}
