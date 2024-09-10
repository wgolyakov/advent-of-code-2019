fun main() {
	data class Reaction(val input: MutableMap<String, Int>, val output: Int)

	fun parse(input: List<String>): Map<String, Reaction> {
		val reactions = mutableMapOf<String, Reaction>()
		for (line in input) {
			val (sInp, sOut) = line.split(" => ")
			val inp = sInp.split(", ").map { it.split(' ') }
				.associate { (q, n) -> n to q.toInt() }.toMutableMap()
			val out = sOut.split(' ')
			reactions[out[1]] = Reaction(inp, out[0].toInt())
		}
		return reactions
	}

	fun simplify(reactions: Map<String, Reaction>) {
		var changed: Boolean
		do {
			changed = false
			for (reaction in reactions.values) {
				val inp = reaction.input
				for ((chemical, quantity) in inp.entries.toList()) {
					if (chemical == "ORE") continue
					val r = reactions[chemical]!!
					if (quantity % r.output != 0) continue
					val n = quantity / r.output
					for ((c, q) in r.input) {
						inp[c] = (inp[c] ?: 0) + n * q
					}
					inp.remove(chemical)
					changed = true
				}
			}
		} while (changed)
	}

	fun makeFuel(reactions: Map<String, Reaction>, remainders: MutableMap<String, Int> = mutableMapOf()): Int {
		val inp = reactions["FUEL"]!!.input.toMutableMap()
		while (inp.size > 1) {
			for ((chemical, quantity) in inp.entries.toList()) {
				if (chemical == "ORE") continue
				val r = reactions[chemical]!!
				val rem = remainders[chemical] ?: 0
				if (quantity <= rem) {
					remainders[chemical] = rem - quantity
				} else {
					val need = quantity - rem
					val n = if (need % r.output == 0) need / r.output else (need / r.output) + 1
					for ((c, q) in r.input) {
						inp[c] = (inp[c] ?: 0) + n * q
					}
					remainders[chemical] = n * r.output - need
				}
				inp.remove(chemical)
			}
		}
		return inp["ORE"]!!
	}

	fun part1(input: List<String>): Int {
		val reactions = parse(input)
		simplify(reactions)
		return makeFuel(reactions)
	}

	fun part2(input: List<String>): Int {
		val reactions = parse(input)
		simplify(reactions)
		val remainders = mutableMapOf<String, Int>()
		val ore1 = makeFuel(reactions, remainders).toLong()
		var ore = ore1
		var fuel = 1

		// For faster test 3
		for (i in 0 until 215) ore += makeFuel(reactions, remainders)
		fuel += 215

		while (ore * 2 + ore1 < 1000000000000) {
			ore *= 2
			fuel *= 2
			for ((c, r) in remainders) remainders[c] = r * 2
			// For spend the remainders
			for (i in 0 until 100) ore += makeFuel(reactions, remainders)
			fuel += 100
		}

		while (ore < 1000000000000) {
			ore += makeFuel(reactions, remainders)
			fuel++
		}
		return fuel - 1
	}

	val testInput = readInput("Day14_test")
	val testInput2 = readInput("Day14_test2")
	val testInput3 = readInput("Day14_test3")
	val testInput4 = readInput("Day14_test4")
	val testInput5 = readInput("Day14_test5")
	check(part1(testInput) == 31)
	check(part1(testInput2) == 165)
	check(part1(testInput3) == 13312)
	check(part1(testInput4) == 180697)
	check(part1(testInput5) == 2210736)
	check(part2(testInput3) == 82892753)
	check(part2(testInput4) == 5586022)
	check(part2(testInput5) == 460664)

	val input = readInput("Day14")
	part1(input).println()
	part2(input).println()
}
