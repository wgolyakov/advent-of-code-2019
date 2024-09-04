import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.round

fun main() {
	class BestAsteroid(val x: Int, val y: Int, val detected: Int)
	class Asteroid(val x: Int, val y: Int, val distance: Int)
	val e = 0.00001

	fun detectAsteroids(input: List<String>, x0: Int, y0: Int): Int {
		var detected = 0
		for ((y, row) in input.withIndex()) {
			for ((x, c) in row.withIndex()) {
				if (c != '#') continue
				if (x == x0 && y == y0) continue
				val dx = x - x0
				val dy = y - y0
				var found = false
				if (dx == 0) {
					if (dy == 1 || dy == -1) {
						detected++
						continue
					}
					val dy1 = if (dy > 0) 1 else -1
					for (i in 1 until abs(dy)) {
						val b = y0 + dy1 * i
						if (input[b][x0] == '#') {
							found = true
							break
						}
					}
				} else {
					val tg = dy.toDouble() / dx.toDouble()
					val dx1 = if (dx > 0) 1 else -1
					val dy1 = tg * dx1
					for (i in 1 until abs(dx)) {
						val a = x0 + dx1 * i
						val b = y0 + dy1 * i
						if (b - b.toInt() > e) continue
						if (input[b.toInt()][a] == '#') {
							found = true
							break
						}
					}
				}
				if (!found) detected++
			}
		}
		return detected
	}

	fun findBestAsteroid(input: List<String>): BestAsteroid {
		var bestAsteroid = BestAsteroid(-1, -1, -1)
		for ((y, row) in input.withIndex()) {
			for ((x, c) in row.withIndex()) {
				if (c != '#') continue
				val detected = detectAsteroids(input, x, y)
				if (detected > bestAsteroid.detected) bestAsteroid = BestAsteroid(x, y, detected)
			}
		}
		return bestAsteroid
	}

	@Suppress("KotlinConstantConditions")
	fun scanAsteroids(input: List<String>, x0: Int, y0: Int): MutableList<MutableList<Asteroid>> {
		val asteroids = mutableMapOf<Double, MutableList<Asteroid>>()
		for ((y, row) in input.withIndex()) {
			for ((x, c) in row.withIndex()) {
				if (c != '#') continue
				if (x == x0 && y == y0) continue
				val dx = x - x0
				val dy = y - y0
				val angle: Double
				val distance: Int
				if (dx == 0 && dy < 0) {
					angle = 0.0
					distance = dy * dy
				} else if (dx > 0 && dy < 0) {
					val tg = dx.toDouble() / -dy.toDouble()
					angle = atan(tg)
					distance = dx * dx + dy * dy
				} else if (dx > 0 && dy == 0) {
					angle = PI / 2
					distance = dx * dx
				} else if (dx > 0 && dy > 0) {
					val tg = dy.toDouble() / dx.toDouble()
					angle = PI / 2 + atan(tg)
					distance = dx * dx + dy * dy
				} else if (dx == 0 && dy > 0) {
					angle = PI
					distance = dy * dy
				} else if (dx < 0 && dy > 0) {
					val tg = -dx.toDouble() / dy.toDouble()
					angle = PI + atan(tg)
					distance = dx * dx + dy * dy
				} else if (dx < 0 && dy == 0) {
					angle = PI * 3 / 2
					distance = dx * dx
				} else if (dx < 0 && dy < 0) {
					val tg = -dy.toDouble() / -dx.toDouble()
					angle = PI * 3 / 2 + atan(tg)
					distance = dx * dx + dy * dy
				} else error("Wrong asteroid: ($x, $y)")
				val rAngle = round(angle / e) * e
				val aList = asteroids.getOrPut(rAngle) { mutableListOf() }
				aList.add(Asteroid(x, y, distance))
			}
		}
		return asteroids.entries.sortedBy { it.key }
			.map { it.value.sortedBy { a -> a.distance }.toMutableList() }.toMutableList()
	}

	fun part1(input: List<String>): Int {
		val st = findBestAsteroid(input)
		return st.detected
	}

	fun part2(input: List<String>): Int {
		val st = findBestAsteroid(input)
		val asteroids = scanAsteroids(input, st.x, st.y)
		var i = 0
		var a = 1
		while (asteroids.isNotEmpty()) {
			if (i >= asteroids.size) i = 0
			val aList = asteroids[i]
			val asteroid = aList.removeFirst()
			if (a++ == 200) return asteroid.x * 100 + asteroid.y
			if (aList.isEmpty()) {
				asteroids.removeAt(i)
			} else {
				i++
			}
		}
		return -1
	}

	val testInput = readInput("Day10_test")
	val testInput2 = readInput("Day10_test2")
	val testInput3 = readInput("Day10_test3")
	val testInput4 = readInput("Day10_test4")
	val testInput5 = readInput("Day10_test5")
	check(part1(testInput) == 8)
	check(part1(testInput2) == 33)
	check(part1(testInput3) == 35)
	check(part1(testInput4) == 41)
	check(part1(testInput5) == 210)
	check(part2(testInput5) == 802)

	val input = readInput("Day10")
	part1(input).println()
	part2(input).println()
}
