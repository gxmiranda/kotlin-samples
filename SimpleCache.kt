import java.util.concurrent.ConcurrentHashMap

//TODO: There is no garbage collection implemented and no size bound, so the cache will grow forever keeping expired and obsolete values indefinitely, which eventually will cause a memory leak
class SimpleCache<K, V> {
    private val cache = ConcurrentHashMap<K, CacheEntry<V>>()
    ///TODO: Hardcoded TTL could be replaced with configurable value for more flexibility
    private val ttlMs = 60000 // 1 minute
    
    data class CacheEntry<V>(val value: V, val timestamp: Long)
    
    fun put(key: K, value: V) {
        ///TODO: Use nanoTime instead of currentTimeMillis to avoid breaking the expiration logic in case the clock changes for whatever reason
        cache[key] = CacheEntry(value, System.currentTimeMillis())
    }
    
    fun get(key: K): V? {
        val entry = cache[key]
        if (entry != null) {
            ///TODO: Use nanoTime instead of currentTimeMillis to avoid breaking the expiration logic in case the clock changes for whatever reason
            if (System.currentTimeMillis() - entry.timestamp < ttlMs) {
                return entry.value
            }
        }
        return null
    }
    
    ///TODO: Even though this function will work, the result is misleading as it will also include expired entries. A more accurate size will be returned if a garbage collector is implemented to remove expired entries
    fun size(): Int {
        return cache.size
    }
}
