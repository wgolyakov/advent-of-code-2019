fun main() {
	fun fuel(mass: Int): Int {
		val f = mass / 3 - 2
		if (f <= 0) return 0
		return f
	}

	fun fuel2(mass: Int): Int {
		var m = mass
		var sum = 0
		while (m > 0) {
			m = fuel(m)
			sum += m
		}
		return sum
	}

	fun part1(input: List<String>): Int {
		return input.map { it.toInt() }.sumOf { fuel(it) }
	}

	fun part2(input: List<String>): Int {
		return input.map { it.toInt() }.sumOf { fuel2(it) }
	}

	val testInput = readInput("Day01_test")
	check(part1(testInput) == 2 + 2 + 654 + 33583)
	check(part2(testInput) == 2 + 2 + 966 + 50346)

	val input = readInput("Day01")
	part1(input).println()
	part2(input).println()
}
