import kotlin.math.abs

fun main() {
	data class Point(val x: Int, val y: Int) {
		fun distance() = abs(x) + abs(y)
	}

	fun loadPath(line: String): List<Point> {
		var x = 0
		var y = 0
		val path = mutableListOf(Point(x, y))
		for (s in line.split(',')) {
			val dir = s[0]
			val n = s.drop(1).toInt()
			for (i in 0 until n) {
				when (dir) {
					'R' -> x++
					'U' -> y--
					'L' -> x--
					'D' -> y++
					else -> error("Wrong direction: $dir")
				}
				path.add(Point(x, y))
			}
		}
		return path
	}

	fun part1(input: List<String>): Int {
		val wire1 = loadPath(input[0])
		val wire2 = loadPath(input[1])
		val intersections = wire1.intersect(wire2.toSet()) - Point(0, 0)
		return intersections.minOf { it.distance() }
	}

	fun part2(input: List<String>): Int {
		val wire1 = loadPath(input[0])
		val wire2 = loadPath(input[1])
		val intersections = wire1.intersect(wire2.toSet()) - Point(0, 0)
		return intersections.minOf { wire1.indexOf(it) + wire2.indexOf(it) }
	}

	val testInput = readInput("Day03_test")
	val testInput2 = readInput("Day03_test2")
	check(part1(testInput) == 159)
	check(part1(testInput2) == 135)
	check(part2(testInput) == 610)
	check(part2(testInput2) == 410)

	val input = readInput("Day03")
	part1(input).println()
	part2(input).println()
}
