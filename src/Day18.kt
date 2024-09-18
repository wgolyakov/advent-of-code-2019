import kotlin.math.pow
import kotlin.random.Random

fun main() {
	data class Point(val x: Int, val y: Int)
	data class Way(val distance: Int, val transitKeys: Set<Char>, val doors: Set<Char>)

	fun findKeys(map: List<String>): Map<Char, Point> {
		val result = mutableMapOf<Char, Point>()
		for ((y, row) in map.withIndex()) {
			for ((x, c) in row.withIndex()) {
				if (c.isLowerCase() || c == '@') result[c] = Point(x, y)
			}
		}
		return result
	}

	fun bfs(start: Point, map: List<String>): Array<IntArray> {
		val distances = Array(map.size) { IntArray(map[it].length) { -1 } }
		val queue = mutableListOf<Point>()
		distances[start.y][start.x] = 0
		queue.add(start)
		while (queue.isNotEmpty()) {
			val curr = queue.removeFirst()
			val neighbors = listOf(
				Point(curr.x - 1, curr.y),
				Point(curr.x + 1, curr.y),
				Point(curr.x, curr.y - 1),
				Point(curr.x, curr.y + 1)
			).filter { map[it.y][it.x] != '#' && distances[it.y][it.x] == -1 }
			for (next in neighbors) {
				distances[next.y][next.x] = distances[curr.y][curr.x] + 1
				queue.add(next)
			}
		}
		return distances
	}

	fun findItemsOnWay(start: Point, stop: Point, map: List<String>,
					   distances: Array<IntArray>): Pair<Set<Char>, Set<Char>> {
		var p = start
		var distance = distances[p.y][p.x]
		val keys = mutableSetOf<Char>()
		val doors = mutableSetOf<Char>()
		while (p != stop) {
			if (p != start) {
				val c = map[p.y][p.x]
				if (c.isLowerCase() || c == '@') keys.add(c)
				if (c.isUpperCase()) doors.add(c.lowercaseChar())
			}
			p = listOf(
				Point(p.x - 1, p.y),
				Point(p.x + 1, p.y),
				Point(p.x, p.y - 1),
				Point(p.x, p.y + 1)
			).first { distances[it.y][it.x] == distance - 1 }
			distance = distances[p.y][p.x]
		}
		return keys to doors
	}

	fun findWays(key: Char, point: Point, map: List<String>, keys: Map<Char, Point>): Map<Char, Way> {
		val ways = mutableMapOf<Char, Way>()
		val distances = bfs(point, map)
		for ((k, p) in keys) {
			if (k == key) continue
			if (distances[p.y][p.x] == -1) continue
			val (wKeys, wDoors) = findItemsOnWay(p, point, map, distances)
			ways[k] = Way(distances[p.y][p.x], wKeys, wDoors)
		}
		return ways
	}

	fun createGraph(map: List<String>): Map<Char, Map<Char, Way>> {
		val keys = findKeys(map)
		//println(keys.keys)
		val graph = mutableMapOf<Char, Map<Char, Way>>()
		val accessible = findWays('@', keys['@']!!, map, keys).keys + '@'
		for (key in accessible) {
			val ways = findWays(key, keys[key]!!, map, keys)
			if (ways.isNotEmpty()) graph[key] = ways
		}
		return graph
	}

	fun antGraph(graph: Map<Char, Map<Char, Way>>): Array<IntArray> {
		val g = Array(graph.size) { IntArray(graph.size) }
		val keys = graph.keys.sorted()
		for ((i, key1) in keys.withIndex()) {
			for ((j, key2) in keys.withIndex()) {
				g[i][j] = graph[key1]!![key2]?.distance ?: -1
			}
		}
		return g
	}

	fun antItems(graph: Map<Char, Map<Char, Way>>): Pair<Array<Array<Set<Int>>>, Array<Array<Set<Int>>>> {
		val doors = Array(graph.size) { Array(graph.size) { emptySet<Int>() } }
		val transitKeys = Array(graph.size) { Array(graph.size) { emptySet<Int>() } }
		val keys = graph.keys.sorted()
		val key2Num = keys.withIndex().associate { (i, k) -> k to i }
		for ((i, key1) in keys.withIndex()) {
			for ((j, key2) in keys.withIndex()) {
				doors[i][j] = graph[key1]!![key2]?.doors?.map { key2Num[it]!! }?.toSet() ?: emptySet()
				transitKeys[i][j] = graph[key1]!![key2]?.transitKeys?.map { key2Num[it]!! }?.toSet() ?: emptySet()
			}
		}
		return doors to transitKeys
	}

	class Ant(private val trailSize: Int) {
		val trail: IntArray = IntArray(trailSize)
		val visited: BooleanArray = BooleanArray(trailSize)

		fun visitKey(currentIndex: Int, key: Int) {
			trail[currentIndex + 1] = key
			visited[key] = true
		}

		fun visited(i: Int) = visited[i]

		fun trailLength(graph: Array<IntArray>): Int {
			//var length = graph[trail[trailSize - 1]][trail[0]]
			var length = 0
			for (i in 0 until trailSize - 1) {
				length += graph[trail[i]][trail[i + 1]]
			}
			return length
		}

		fun clear() {
			for (i in 0 until trailSize) {
				visited[i] = false
			}
		}
	}

	class AntColonyOptimization(private val graph: Array<IntArray>,
								private val doors: Array<Array<Set<Int>>>,
								private val transitKeys: Array<Array<Set<Int>>>) {
		// Initial pheromones
		private val c = 1.0
		// Coefficient influencing the calculation of the amount of pheromones
		private val alpha = 1.0
		// Coefficient influencing the consideration of the distance between keys
		private val beta = 5.0
		// Pheromone evaporation factor
		private val evaporation = 0.5
		// Empirically selected constant
		private val q = 500.0
		// Factor for calculating the number of ants by number of keys
		private val antFactor = 0.8
		// Probability of ant random move
		private val randomFactor = 0.1
		private val maxIterations = 1000
		private val numberOfKeys = graph.size
		private val numberOfAnts = (numberOfKeys * antFactor).toInt()
		private val trails = Array(numberOfKeys) { DoubleArray(numberOfKeys) }
		private val ants: MutableList<Ant> = MutableList(numberOfAnts) { Ant(numberOfKeys) }
		private val random = Random(123)
		private val probabilities = DoubleArray(numberOfKeys)
		private var bestTourOrder = IntArray(numberOfKeys) { it }
		private var bestTourLength = Int.MAX_VALUE

		fun solve(): Int {
			clearTrails()
			for (i in 0 until maxIterations) {
				setupAnts()
				moveAnts()
				updateTrails()
				updateBest()
			}
			//println("Best tour length: $bestTourLength")
			//println("Best tour order: ${bestTourOrder.contentToString()}")
			return bestTourLength
		}

		private fun setupAnts() {
			for (ant in ants) {
				ant.clear()
				ant.visitKey(-1, 0)
			}
		}

		private fun moveAnts() {
			for (i in 0 until numberOfKeys - 1) {
				for (ant in ants) {
					ant.visitKey(i, selectNextKey(ant, i))
				}
			}
		}

		private fun canOpenDoors(ant: Ant, i: Int, j: Int): Boolean {
			val trailDoors = doors[i][j]
			if (trailDoors.isEmpty()) return true
			return trailDoors.all { ant.visited(it) }
		}

		private fun canTransit(ant: Ant, i: Int, j: Int): Boolean {
			val trailKeys = transitKeys[i][j]
			if (trailKeys.isEmpty()) return true
			return trailKeys.all { ant.visited(it) }
		}

		private fun canGo(ant: Ant, i: Int, j: Int): Boolean {
			return !ant.visited(j) && graph[i][j] != -1 && canOpenDoors(ant, i, j) && canTransit(ant, i, j)
		}

		// Select next key for each ant
		private fun selectNextKey(ant: Ant, currentIndex: Int): Int {
			val s = ant.trail[currentIndex]
			val t = random.nextInt(numberOfKeys)
			if (random.nextDouble() < randomFactor) {
				if (canGo(ant, s, t)) return t
			}
			calculateProbabilities(ant, currentIndex)
			val r = random.nextDouble()
			var total = 0.0
			for (i in 0 until numberOfKeys) {
				total += probabilities[i]
				if (total >= r) return i
			}
			throw RuntimeException("There are no other keys")
		}

		// Calculate the next key picks probabilities
		private fun calculateProbabilities(ant: Ant, currentIndex: Int) {
			val i = ant.trail[currentIndex]
			var pheromone = 0.0
			for (l in 0 until numberOfKeys) {
				if (canGo(ant, i, l)) {
					pheromone += trails[i][l].pow(alpha) * (1.0 / graph[i][l]).pow(beta)
				}
			}
			for (j in 0 until numberOfKeys) {
				if (!canGo(ant, i, j)) {
					probabilities[j] = 0.0
				} else {
					val numerator = trails[i][j].pow(alpha) * (1.0 / graph[i][j]).pow(beta)
					probabilities[j] = numerator / pheromone
				}
			}
		}

		// Update trails that ants used
		private fun updateTrails() {
			for (i in 0 until numberOfKeys) {
				for (j in 0 until numberOfKeys) {
					trails[i][j] *= evaporation
				}
			}
			for (ant in ants) {
				val contribution = q / ant.trailLength(graph)
				for (i in 0 until numberOfKeys - 1) {
					trails[ant.trail[i]][ant.trail[i + 1]] += contribution
				}
				//trails[ant.trail[numberOfKeys - 1]][ant.trail[0]] += contribution
			}
		}

		private fun updateBest() {
			for (ant in ants) {
				val tl = ant.trailLength(graph)
				if (tl < bestTourLength) {
					bestTourLength = tl
					bestTourOrder = ant.trail.clone()
				}
			}
		}

		private fun clearTrails() {
			for (i in 0 until numberOfKeys) {
				for (j in 0 until numberOfKeys) {
					trails[i][j] = c
				}
			}
		}
	}

	// ...    @#@
	// .@. -> ###
	// ...    @#@
	fun updateMap(input: List<String>): List<List<String>> {
		val cy = input.size / 2
		val cx = input[cy].length / 2
		val centerArea = listOf(
			".#.",
			"###",
			".#."
		)
		val maps = mutableListOf<List<String>>()
		val entrances = listOf(
			Point(cx - 1, cy - 1),
			Point(cx + 1, cy - 1),
			Point(cx - 1, cy + 1),
			Point(cx + 1, cy + 1)
		)
		for (entrance in entrances) {
			val map = input.map { StringBuilder(it) }
			for (dy in -1..1) {
				for (dx in -1..1) {
					map[cy + dy][cx + dx] = centerArea[dy + 1][dx + 1]
				}
			}
			map[entrance.y][entrance.x] = '@'
			maps.add(map.map { it.toString() })
		}
		return maps
	}

	fun findMinPath(graph: Map<Char, Map<Char, Way>>): Int {
		var minPathLen = Int.MAX_VALUE
		var minPath = mutableSetOf<Char>()
		for (i in 0 until 1_000_000) {
			var key = '@'
			val path = mutableSetOf('@')
			val unvisited = (graph.keys - '@').toMutableSet()
			while (unvisited.isNotEmpty()) {
				val sorted = unvisited.filter {
					val way = graph[key]!![it]!!
					val canOpenDoors = way.doors.isEmpty() || (way.doors - path).isEmpty()
					val canTransit = way.transitKeys.isEmpty() || (way.transitKeys - path).isEmpty()
					canOpenDoors && canTransit
				}.sortedBy { graph[key]!![it]!!.distance }
				var r = 0
				while(Random.nextDouble() < 0.5 && r < sorted.size - 1) { r++ }
				val next = sorted[r]
				path.add(next)
				unvisited.remove(next)
				key = next
			}
			val pathLen = path.windowed(2).sumOf { (k1, k2) -> graph[k1]!![k2]!!.distance }
			if (pathLen < minPathLen) {
				minPathLen = pathLen
				minPath = path
			}
		}
		//println("Min path: $minPath - $minPathLen")
		return minPathLen
	}

	fun findMinPath(graphs: List<Map<Char, Map<Char, Way>>>): Int {
		var minPathLen = Int.MAX_VALUE
		var minPath = mutableSetOf<Char>()
		val allKeys = graphs.map { it.keys }.reduce { k1, k2 -> k1 + k2 } - '@'
		for (i in 0 until 1000) {
			val currKeys = CharArray(graphs.size) { '@' }
			val path = mutableSetOf('@')
			val paths = Array(graphs.size) { mutableSetOf('@') }
			val allUnvisited = allKeys.toMutableSet()
			val unvisited = graphs.map { (it.keys - '@').toMutableSet() }
			while (allUnvisited.isNotEmpty()) {
				val n = graphs.indices.random()
				val graph = graphs[n]
				val key = currKeys[n]
				val sorted = unvisited[n].filter {
					val way = graph[key]!![it]!!
					val canOpenDoors = way.doors.isEmpty() || (way.doors - path).isEmpty()
					val canTransit = way.transitKeys.isEmpty() || (way.transitKeys - path).isEmpty()
					canOpenDoors && canTransit
				}.sortedBy { graph[key]!![it]!!.distance }
				if (sorted.isEmpty()) continue
				var r = 0
				while(Random.nextDouble() < 0.5 && r < sorted.size - 1) { r++ }
				val next = sorted[r]
				path.add(next)
				paths[n].add(next)
				allUnvisited.remove(next)
				unvisited[n].remove(next)
				currKeys[n] = next
			}
			val pathLen = paths.withIndex().sumOf { (n, p) ->
				p.windowed(2).sumOf { (k1, k2) -> graphs[n][k1]!![k2]!!.distance } }
			if (pathLen < minPathLen) {
				minPathLen = pathLen
				minPath = path
			}
		}
		//println("Min path: $minPath - $minPathLen")
		return minPathLen
	}

	fun part1(input: List<String>): Int {
		val graph = createGraph(input)
		val antGraph = antGraph(graph)
		val (antDoors, antTransitKeys) = antItems(graph)
		return AntColonyOptimization(antGraph, antDoors, antTransitKeys).solve()
	}

	fun part2(input: List<String>): Int {
		val maps = updateMap(input)
		val graphs = maps.map {	createGraph(it) }
		return findMinPath(graphs)
	}

	val testInput = readInput("Day18_test")
	val testInput2 = readInput("Day18_test2")
	val testInput3 = readInput("Day18_test3")
	val testInput4 = readInput("Day18_test4")
	val testInput5 = readInput("Day18_test5")
	check(part1(testInput) == 8)
	check(part1(testInput2) == 86)
	check(part1(testInput3) == 132)
	check(part1(testInput4) == 136)
	check(part1(testInput5) == 81)

	val testInput6 = readInput("Day18_test6")
	val testInput7 = readInput("Day18_test7")
	val testInput8 = readInput("Day18_test8")
	val testInput9 = readInput("Day18_test9")
	check(part2(testInput6) == 8)
	check(part2(testInput7) == 24)
	check(part2(testInput8) == 32)
	check(part2(testInput9) == 72)

	val input = readInput("Day18")
	part1(input).println() // 3586
	part2(input).println() // 1974
}
