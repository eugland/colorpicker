import com.primortex.color.service.ColorNameIndex
import org.junit.Test

class ColorSearchIndexTest {

    @Test
    fun search_partial_prefix_and_substring() {
        val queries = listOf(
            "blu",     // prefix + substring
            "blue",    // exact / prefix
            "dark",    // substring
            "nav",     // prefix
            "#ff",     // should likely return empty (name-based index)
            "  BlU  "  // normalization test
        )

        for (q in queries) {
            val results = ColorNameIndex.search(q, limit = 10)

            println("Query: \"$q\"  →  ${results.size} result(s)")
            for ((idx, r) in results.withIndex()) {
                val hex = "#" + (r.argb and 0x00FFFFFF)
                    .toString(16)
                    .uppercase()
                    .padStart(6, '0')

                println("  [$idx] ${r.name}  $hex")
            }
            println("--------------------------------------------------")
        }
    }
}
