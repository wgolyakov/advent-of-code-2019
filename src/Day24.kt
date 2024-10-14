fun main() {
	data class Point(val x: Int, val y: Int)
	data class Point3D(val x: Int, val y: Int, val level: Int)

	fun biodiversityRating(input: List<StringBuilder>): Int {
		var result = 0
		var i = 1
		for (row in input) {
			for (c in row) {
				if (c == '#') result += i
				i *= 2
			}
		}
		return result
	}

	fun live(grid: List<StringBuilder>): List<StringBuilder> {
		val result = grid.map { StringBuilder(it) }
		for ((y, row) in grid.withIndex()) {
			for ((x, c) in row.withIndex()) {
				val adj = listOf(Point(x - 1, y), Point(x + 1, y), Point(x, y - 1), Point(x, y + 1))
					.filter { it.x >= 0 && it.y >= 0 && it.x < row.length && it.y < grid.size }
					.count { grid[it.y][it.x] == '#' }
				if (c == '#') {
					if (adj != 1) result[y][x] = '.'
				} else {
					if (adj == 1 || adj == 2) result[y][x] = '#'
				}
			}
		}
		return result
	}

	fun live2(space: Map<Int, List<StringBuilder>>): Map<Int, List<StringBuilder>> {
		val adjCount = mutableMapOf<Int, List<IntArray>>()
		for ((level, grid) in space) adjCount[level] = grid.map { IntArray(it.length) }
		for ((level, grid) in space) {
			for ((y, row) in grid.withIndex()) {
				for ((x, c) in row.withIndex()) {
					if (c != '#') continue
					val adjPoints = listOf(
						Point(x - 1, y), Point(x + 1, y), Point(x, y - 1), Point(x, y + 1))
					val adjPoints3D = mutableListOf<Point3D>()
					for (p in adjPoints) {
						if (p.x < 0) {
							adjPoints3D.add(Point3D(row.length / 2 - 1, grid.size / 2, level - 1))
						} else if (p.y < 0) {
							adjPoints3D.add(Point3D(row.length / 2, grid.size / 2 - 1, level - 1))
						} else if (p.x >= row.length) {
							adjPoints3D.add(Point3D(row.length / 2 + 1, grid.size / 2, level - 1))
						} else if (p.y >= grid.size) {
							adjPoints3D.add(Point3D(row.length / 2, grid.size / 2 + 1, level - 1))
						} else if (p.x == row.length / 2 && p.y == grid.size / 2) {
							if (p.x < x) {
								for (i in grid.indices) adjPoints3D.add(Point3D(row.length - 1, i, level + 1))
							} else if (p.y < y) {
								for (i in row.indices) adjPoints3D.add(Point3D(i, grid.size - 1, level + 1))
							} else if (p.x > x) {
								for (i in grid.indices) adjPoints3D.add(Point3D(0, i, level + 1))
							} else if (p.y > y) {
								for (i in row.indices) adjPoints3D.add(Point3D(i, 0, level + 1))
							}
						} else {
							adjPoints3D.add(Point3D(p.x, p.y, level))
						}
					}
					for (p in adjPoints3D) {
						val adjGrid = adjCount.getOrPut(p.level) { MutableList(grid.size) { IntArray(row.length) } }
						adjGrid[p.y][p.x]++
					}
				}
			}
		}
		val result = mutableMapOf<Int, List<StringBuilder>>()
		for ((level, grid) in adjCount) result[level] = grid.map { StringBuilder(".".repeat(it.size)) }
		for ((level, grid) in adjCount) {
			for ((y, row) in grid.withIndex()) {
				for ((x, adj) in row.withIndex()) {
					val c = space[level]?.get(y)?.get(x) ?: '.'
					if (c == '#') {
						result[level]!![y][x] = if (adj != 1) '.' else '#'
					} else {
						result[level]!![y][x] = if (adj == 1 || adj == 2) '#' else '.'
					}
				}
			}
		}
		return result
	}

	fun part1(input: List<String>): Int {
		var grid = input.map { StringBuilder(it) }
		var raiting = biodiversityRating(grid)
		val ratings = mutableSetOf<Int>()
		while (raiting !in ratings) {
			ratings.add(raiting)
			grid = live(grid)
			raiting = biodiversityRating(grid)
		}
		return raiting
	}

	fun part2(input: List<String>, minutes: Int = 200): Int {
		var space = mapOf(0 to input.map { StringBuilder(it) })
		for (t in 0 until minutes) {
			space = live2(space)
		}
		return space.values.sumOf { it.sumOf { row -> row.count { c -> c == '#' } } }
	}

	val testInput = readInput("Day24_test")
	check(part1(testInput) == 2129920)
	check(part2(testInput, 10) == 99)

	val input = readInput("Day24")
	part1(input).println() // 32513278
	part2(input).println() // 1912
}
