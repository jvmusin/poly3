package util

/**
 * Group of items with the same [key] produced by [sequentiallyGroupedBy].
 *
 * @param TKey type of a key.
 * @param TItem type of items.
 * @property key Key associated with items in this group.
 * @property items Items with the same [key].
 */
data class Group<TKey, TItem>(val key: TKey, val items: List<TItem>) {
    /**
     * Number of elements in this group.
     */
    val size get() = items.size
}

/**
 * Splits items onto groups with a key taken from [getKey].
 *
 * Takes items from the beginning while [getKey] returns the same value
 * and puts those items in a [Group].
 *
 * Then repeats the same operation to all other items in *this* iterable
 * until there are any items left in *this* iterable.
 *
 * @param TKey type of a key used to split items by groups.
 * @param TItem type of items in a sequence.
 * @param getKey function used to get a key of an item.
 * @return List of [Group] elements where it's items are
 *         in the same order as they were in a source iterable.
 */
inline fun <TKey, TItem> Iterable<TItem>.sequentiallyGroupedBy(getKey: (TItem) -> TKey): List<Group<TKey, TItem>> {
    val result = mutableListOf<Group<TKey, TItem>>()
    for (item in this) {
        val key = getKey(item)
        if (result.isEmpty() || result.last().key != key) result += Group(key, mutableListOf())
        (result.last().items as MutableList<TItem>) += item
    }
    return result
}
