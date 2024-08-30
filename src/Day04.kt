fun main() {
	fun meetCriteria(password: Int): Boolean {
		val list = password.toString().map { it.digitToInt() }
		if (list.size != 6) return false
		if (!list.windowed(2).any { it[0] == it[1] }) return false
		for ((a, b) in list.windowed(2)) {
			if (b < a) return false
		}
		return true
	}

	fun meetCriteria2(password: Int): Boolean {
		val list = password.toString().map { it.digitToInt() }
		if (list.size != 6) return false
		for ((a, b) in list.windowed(2)) {
			if (b < a) return false
		}
		val group = mutableListOf<Int>()
		for (n in list) {
			if (group.isNotEmpty() && group[0] != n) {
				if (group.size == 2) break
				group.clear()
			}
			group.add(n)
		}
		return group.size == 2
	}

	fun part1(input: List<String>): Int {
		val (a, b) = input[0].split('-').map { it.toInt() }
		return (a..b).count { meetCriteria(it) }
	}

	fun part2(input: List<String>): Int {
		val (a, b) = input[0].split('-').map { it.toInt() }
		return (a..b).count { meetCriteria2(it) }
	}

	check(meetCriteria(111111))
	check(!meetCriteria(223450))
	check(!meetCriteria(123789))
	check(meetCriteria2(112233))
	check(!meetCriteria2(123444))
	check(meetCriteria2(111122))

	val input = readInput("Day04")
	part1(input).println()
	part2(input).println()
}
