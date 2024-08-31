fun main() {
	class Node(val name: String) {
		var parent: Node? = null
		val children = mutableListOf<Node>()
		var distance = -1
		fun neighbors() = if (parent == null) children else children + parent!!
		override fun toString() = name
	}

	fun parse(input: List<String>): Map<String, Node> {
		val nodes = mutableMapOf<String, Node>()
		for (line in input) {
			val (parent, child) = line.split(')').map { nodes.getOrPut(it) { Node(it) } }
			parent.children.add(child)
			child.parent = parent
		}
		return nodes
	}

	fun countOrbitsRecurs(node: Node, level: Int): Int {
		return level + node.children.sumOf { countOrbitsRecurs(it, level + 1) }
	}

	fun bfs(start: Node, stop: Node): Int {
		val queue = mutableListOf<Node>()
		start.distance = 0
		queue.add(start)
		while (queue.isNotEmpty()) {
			val curr = queue.removeFirst()
			if (curr === stop) return stop.distance
			for (next in curr.neighbors()) {
				if (next.distance == -1) {
					next.distance = curr.distance + 1
					queue.add(next)
				}
			}
		}
		return -1
	}

	fun part1(input: List<String>): Int {
		val nodes = parse(input)
		val root = nodes["COM"]!!
		return countOrbitsRecurs(root, 0)
	}

	fun part2(input: List<String>): Int {
		val nodes = parse(input)
		val youParent = nodes["YOU"]!!.parent!!
		val sanParent = nodes["SAN"]!!.parent!!
		return bfs(youParent, sanParent)
	}

	val testInput = readInput("Day06_test")
	val testInput2 = readInput("Day06_test2")
	check(part1(testInput) == 42)
	check(part2(testInput2) == 4)

	val input = readInput("Day06")
	part1(input).println()
	part2(input).println()
}
