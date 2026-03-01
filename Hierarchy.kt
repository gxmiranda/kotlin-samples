import kotlin.test.*

/**
 * A `Hierarchy` stores an arbitrary _forest_ (an ordered collection of ordered trees)
 * as an array of node IDs in the order of DFS traversal, combined with a parallel array of node depths.
 *
 * Parent-child relationships are identified by the position in the array and the associated depth.
 * Each tree root has depth 0, its children have depth 1 and follow it in the array, their children have depth 2 and follow them, etc.
 *
 * Example:
 * ```
 * nodeIds: 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11
 * depths:  0, 1, 2, 3, 1, 0, 1, 0, 1, 1, 2
 * ```
 *
 * the forest can be visualized as follows:
 * ```
 * 1
 * - 2
 * - - 3
 * - - - 4
 * - 5
 * 6
 * - 7
 * 8
 * - 9
 * - 10
 * - - 11
 *```
 * 1 is a parent of 2 and 5, 2 is a parent of 3, etc. Note that depth is equal to the number of hyphens for each node.
 *
 * Invariants on the depths array:
 *  * Depth of the first element is 0.
 *  * If the depth of a node is `D`, the depth of the next node in the array can be:
 *      * `D + 1` if the next node is a child of this node;
 *      * `D` if the next node is a sibling of this node;
 *      * `d < D` - in this case the next node is not related to this node.
 */
interface Hierarchy {
  /** The number of nodes in the hierarchy. */
  val size: Int

  /**
   * Returns the unique ID of the node identified by the hierarchy index. The depth for this node will be `depth(index)`.
   * @param index must be non-negative and less than [size]
   * */
  fun nodeId(index: Int): Int

  /**
   * Returns the depth of the node identified by the hierarchy index. The unique ID for this node will be `nodeId(index)`.
   * @param index must be non-negative and less than [size]
   * */
  fun depth(index: Int): Int

  fun formatString(): String {
    return (0 until size).joinToString(
      separator = ", ",
      prefix = "[",
      postfix = "]"
    ) { i -> "${nodeId(i)}:${depth(i)}" }
  }
}

/**
 * A node is present in the filtered hierarchy if its node ID passes the predicate and all of its ancestors pass it as well.
 */
fun Hierarchy.filter(nodeIdPredicate: (Int) -> Boolean): Hierarchy {
  val resultNodeIds = mutableListOf<Int>()
  val resultDepths = mutableListOf<Int>()
  val validUpTo = BooleanArray(size)

  for (i in 0 until size) {
    val d = depth(i)
    val id = nodeId(i)
    val nodeValid = (d == 0 || validUpTo[d - 1]) && nodeIdPredicate(id)
    validUpTo[d] = nodeValid
    if (nodeValid) {
      resultNodeIds.add(id)
      resultDepths.add(d)
    }
  }

  return ArrayBasedHierarchy(resultNodeIds.toIntArray(), resultDepths.toIntArray())
}

class ArrayBasedHierarchy(
  private val myNodeIds: IntArray,
  private val myDepths: IntArray,
) : Hierarchy {
  override val size: Int = myDepths.size

  override fun nodeId(index: Int): Int = myNodeIds[index]

  override fun depth(index: Int): Int = myDepths[index]
}

class FilterTest {
  @Test
  fun testFilter() {
    val unfiltered: Hierarchy = ArrayBasedHierarchy(
      intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11),
      intArrayOf(0, 1, 2, 3, 1, 0, 1, 0, 1, 1, 2))
    val filteredActual: Hierarchy = unfiltered.filter { nodeId -> nodeId % 3 != 0 }
    val filteredExpected: Hierarchy = ArrayBasedHierarchy(
      intArrayOf(1, 2, 5, 8, 10, 11),
      intArrayOf(0, 1, 1, 0, 1, 2))
    assertEquals(filteredExpected.formatString(), filteredActual.formatString())
  }

  @Test
  fun testFilterEmpty() {
    // Filtering an empty hierarchy returns empty
    val empty: Hierarchy = ArrayBasedHierarchy(IntArray(0), IntArray(0))
    assertEquals("[]", empty.filter { true }.formatString())
  }

  @Test
  fun testFilterAllPass() {
    // When all nodes pass the predicate the result equals the input
    val h: Hierarchy = ArrayBasedHierarchy(
      intArrayOf(1, 2, 3, 4, 5),
      intArrayOf(0, 1, 2, 1, 2))
    assertEquals(h.formatString(), h.filter { true }.formatString())
  }

  @Test
  fun testFilterNonePass() {
    // When no node passes the predicate the result is empty
    val h: Hierarchy = ArrayBasedHierarchy(
      intArrayOf(1, 2, 3),
      intArrayOf(0, 1, 2))
    assertEquals("[]", h.filter { false }.formatString())
  }

  @Test
  fun testFilterRootExcludedPrunesSubtree() {
    // Excluding a root removes all its descendants
    val h: Hierarchy = ArrayBasedHierarchy(
      intArrayOf(1, 2, 3, 4, 5),
      intArrayOf(0, 1, 2, 0, 1))
    val expected: Hierarchy = ArrayBasedHierarchy(
      intArrayOf(4, 5),
      intArrayOf(0, 1))
    assertEquals(expected.formatString(), h.filter { it != 1 }.formatString())
  }

  @Test
  fun testFilterIntermediateNodeExcludedPrunesDescendants() {
    // Excluding a mid-level node prunes its subtree but not its siblings
    val h: Hierarchy = ArrayBasedHierarchy(
      intArrayOf(1, 2, 3, 4, 5),
      intArrayOf(0, 1, 2, 1, 2))
    val expected: Hierarchy = ArrayBasedHierarchy(
      intArrayOf(1, 4, 5),
      intArrayOf(0, 1, 2))
    assertEquals(expected.formatString(), h.filter { it != 2 }.formatString())
  }

  @Test
  fun testFilterSingleNodePasses() {
    //Single-node pass case
    val h: Hierarchy = ArrayBasedHierarchy(intArrayOf(42), intArrayOf(0))
    assertEquals("[42:0]", h.filter { true }.formatString())
  }

  @Test
  fun testFilterSingleNodeFails() {
    //Single-node fail case
    val h: Hierarchy = ArrayBasedHierarchy(intArrayOf(42), intArrayOf(0))
    assertEquals("[]", h.filter { false }.formatString())
  }

  @Test
  fun testFilterOnlyLeavesPassButAncestorsFail() {
    // Leaves pass but their ancestors don't pass
    val h: Hierarchy = ArrayBasedHierarchy(
      intArrayOf(1, 2, 3),
      intArrayOf(0, 1, 2))
    assertEquals("[]", h.filter { it == 3 }.formatString())
  }

  @Test
  fun testFilterSiblingAfterExcludedBranch() {
    // Sibling of an excluded node is evaluated independently (guards against stale validUpTo leaking across siblings)
    val h: Hierarchy = ArrayBasedHierarchy(
      intArrayOf(1, 2, 3, 4, 5),
      intArrayOf(0, 1, 2, 1, 2))
    val expected: Hierarchy = ArrayBasedHierarchy(
      intArrayOf(1, 4, 5),
      intArrayOf(0, 1, 2))
    assertEquals(expected.formatString(), h.filter { it != 2 }.formatString())
  }
}