fun main() {
	data class Point3D(val x: Int, val y: Int, val level: Int)

	data class Point(val x: Int, val y: Int) {
		fun to3D(level: Int) = Point3D(x, y, level)
	}

	class Teleport(val name: String, val enter: Point, val exit: Point, val inner: Boolean)

	fun enlargeMap(input: List<String>): List<String> {
		val width = input.maxOf { it.length }
		val map = input.map { StringBuilder(it) }.toMutableList()
		for (row in map) {
			row.insert(0, ' ')
			while (row.length < width + 2) row.append(' ')
		}
		map.add(0, StringBuilder(" ".repeat(width + 2)))
		map.add(StringBuilder(" ".repeat(width + 2)))
		return map.map { it.toString() }
	}

	fun findTeleports(map: List<String>): Map<String, MutableList<Teleport>> {
		val teleports = mutableMapOf<String, MutableList<Teleport>>()
		val visited = mutableSetOf<Point>()
		for ((y, row) in map.withIndex()) {
			for ((x, c) in row.withIndex()) {
				if (!c.isUpperCase()) continue
				if (Point(x, y) in visited) continue
				val cl = map[y][x - 1]
				val cr = map[y][x + 1]
				val cu = map[y - 1][x]
				val cd = map[y + 1][x]
				val name: String
				val enter: Point
				val exit: Point
				val inner: Boolean
				if (cl.isUpperCase()) {
					name = "$cl$c"
					visited.add(Point(x - 1, y))
					if (map[y][x - 2] == '.') {
						exit = Point(x - 2, y)
						enter = Point(x - 1, y)
						inner = (x + 1 != row.length - 1)
					} else if (map[y][x + 1] == '.') {
						exit = Point(x + 1, y)
						enter = Point(x, y)
						inner = (x - 2 != 0)
					} else error("Wrong teleport: $name ($x, $y)")
				} else if (cr.isUpperCase()) {
					name = "$c$cr"
					visited.add(Point(x + 1, y))
					if (map[y][x - 1] == '.') {
						exit = Point(x - 1, y)
						enter = Point(x, y)
						inner = (x + 2 != row.length - 1)
					} else if (map[y][x + 2] == '.') {
						exit = Point(x + 2, y)
						enter = Point(x + 1, y)
						inner = (x - 1 != 0)
					} else error("Wrong teleport: $name ($x, $y)")
				} else if (cu.isUpperCase()) {
					name = "$cu$c"
					visited.add(Point(x, y - 1))
					if (map[y - 2][x] == '.') {
						exit = Point(x, y - 2)
						enter = Point(x, y - 1)
						inner = (y + 1 != map.size - 1)
					} else if (map[y + 1][x] == '.') {
						exit = Point(x, y + 1)
						enter = Point(x, y)
						inner = (y - 2 != 0)
					} else error("Wrong teleport: $name ($x, $y)")
				} else if (cd.isUpperCase()) {
					name = "$c$cd"
					visited.add(Point(x, y + 1))
					if (map[y - 1][x] == '.') {
						exit = Point(x, y - 1)
						enter = Point(x, y)
						inner = (y + 2 != map.size - 1)
					} else if (map[y + 2][x] == '.') {
						exit = Point(x, y + 2)
						enter = Point(x, y + 1)
						inner = (y - 1 != 0)
					} else error("Wrong teleport: $name ($x, $y)")
				} else error("Wrong teleport: $c ($x, $y)")
				teleports.getOrPut(name) { mutableListOf() }.add(Teleport(name, enter, exit, inner))
			}
		}
		return teleports
	}

	fun connectTeleports(teleports: Map<String, MutableList<Teleport>>): Map<Point, Point> {
		val connections = mutableMapOf<Point, Point>()
		for (tels in teleports.values) {
			if (tels.size != 2) continue
			val (a, b) = tels
			connections[a.enter] = b.exit
			connections[b.enter] = a.exit
		}
		return connections
	}

	fun findInnerEnters(teleports: Map<String, MutableList<Teleport>>): Set<Point> {
		val innerEnters = mutableSetOf<Point>()
		for (tels in teleports.values) {
			if (tels.size != 2) continue
			val (a, b) = tels
			if (a.inner) innerEnters.add(a.enter)
			if (b.inner) innerEnters.add(b.enter)
		}
		return innerEnters
	}

	fun bfs(map: List<String>, start: Point, stop: Point, connections: Map<Point, Point>): Int {
		val distances = Array(map.size) { IntArray(map[it].length) { -1 } }
		val queue = mutableListOf<Point>()
		distances[start.y][start.x] = 0
		queue.add(start)
		while (queue.isNotEmpty()) {
			val curr = queue.removeFirst()
			if (curr == stop) return distances[curr.y][curr.x]
			val neighbors = listOf(
				Point(curr.x - 1, curr.y),
				Point(curr.x + 1, curr.y),
				Point(curr.x, curr.y - 1),
				Point(curr.x, curr.y + 1)
			).map { connections[it] ?: it }.filter { map[it.y][it.x] == '.' && distances[it.y][it.x] == -1 }
			for (next in neighbors) {
				distances[next.y][next.x] = distances[curr.y][curr.x] + 1
				queue.add(next)
			}
		}
		return -1
	}

	fun bfs3D(map: List<String>, start: Point3D, stop: Point3D,
			  connections: Map<Point, Point>, innerEnters: Set<Point>): Int {
		val distances = MutableList(1) { Array(map.size) { Array(map[it].length) { -1 } } }
		val queue = mutableListOf<Point3D>()
		distances[0][start.y][start.x] = 0
		queue.add(start)
		while (queue.isNotEmpty()) {
			val curr = queue.removeFirst()
			if (curr == stop) return distances[curr.level][curr.y][curr.x]
			val neighbors = listOf(
				Point(curr.x - 1, curr.y),
				Point(curr.x + 1, curr.y),
				Point(curr.x, curr.y - 1),
				Point(curr.x, curr.y + 1)
			)
			val neighbors3D = mutableListOf<Point3D>()
			for (n in neighbors) {
				if (n in connections) {
					if (n in innerEnters) {
						neighbors3D.add(connections[n]!!.to3D(curr.level + 1))
						if (distances.size - 1 < curr.level + 1) {
							distances.add(Array(map.size) { Array(map[it].length) { -1 } })
						}
					} else if (curr.level > 0) {
						neighbors3D.add(connections[n]!!.to3D(curr.level - 1))
					}
				} else {
					neighbors3D.add(n.to3D(curr.level))
				}
			}
			val goodNeighbors3D = neighbors3D.filter { map[it.y][it.x] == '.' && distances[it.level][it.y][it.x] == -1 }
			for (next in goodNeighbors3D) {
				distances[next.level][next.y][next.x] = distances[curr.level][curr.y][curr.x] + 1
				queue.add(next)
			}
		}
		return -1
	}

	fun part1(input: List<String>): Int {
		val map = enlargeMap(input)
		val teleports = findTeleports(map)
		val start = teleports["AA"]!!.single().exit
		val stop = teleports["ZZ"]!!.single().exit
		val connections = connectTeleports(teleports)
		return bfs(map, start, stop, connections)
	}

	fun part2(input: List<String>): Int {
		val map = enlargeMap(input)
		val teleports = findTeleports(map)
		val start = teleports["AA"]!!.single().exit.to3D(0)
		val stop = teleports["ZZ"]!!.single().exit.to3D(0)
		val connections = connectTeleports(teleports)
		val innerEnters = findInnerEnters(teleports)
		return bfs3D(map, start, stop, connections, innerEnters)
	}

	val testInput = readInput("Day20_test")
	val testInput2 = readInput("Day20_test2")
	val testInput3 = readInput("Day20_test3")
	check(part1(testInput) == 23)
	check(part1(testInput2) == 58)
	check(part2(testInput) == 26)
	check(part2(testInput3) == 396)

	val input = readInput("Day20")
	part1(input).println() // 560
	part2(input).println() // 6642
}
