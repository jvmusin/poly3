package util

/** Group of [items] with the same [key], generally produced by [sequentiallyGroupedBy]. */
data class Group<TKey, TItem>(val key: TKey, val items: List<TItem>) {

    /** Number of elements in this group. */
    val size get() = items.size
}

/**
 * Splits items of `this` iterable onto groups with a key taken from [getKey].
 *
 * Takes items from the beginning while [getKey] returns the same value
 * and puts those items in a [Group].
 *
 * Then repeats the same operation to all other items in `this` iterable
 * until there are any items left in `this` iterable and finally
 * returns a list of built [Group]s.
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
