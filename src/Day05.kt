fun main() {
	fun parse(input: List<String>) = input[0].split(',').map { it.toInt() }.toMutableList()

	fun value(memory: List<Int>, address: Int, paramMode: Int): Int {
		return when (paramMode) {
			0 -> memory[memory[address]]
			1 -> memory[address]
			else -> error("Wrong parameter mode: $paramMode")
		}
	}

	fun run(memory: MutableList<Int>, input: Int): Int {
		var output = -1
		var i = 0
		while (i < memory.size) {
			val instr = memory[i]
			val opcode = instr % 100
			val paramMode1 = (instr % 1000) / 100
			val paramMode2 = (instr % 10000) / 1000
			when (opcode) {
				1 -> {
					val p1 = value(memory, i + 1, paramMode1)
					val p2 = value(memory, i + 2, paramMode2)
					memory[memory[i + 3]] = p1 + p2
					i += 4
				}
				2 -> {
					val p1 = value(memory, i + 1, paramMode1)
					val p2 = value(memory, i + 2, paramMode2)
					memory[memory[i + 3]] = p1 * p2
					i += 4
				}
				3 -> {
					memory[memory[i + 1]] = input
					i += 2
				}
				4 -> {
					output = value(memory, i + 1, paramMode1)
					i += 2
				}
				5 -> {
					val p1 = value(memory, i + 1, paramMode1)
					val p2 = value(memory, i + 2, paramMode2)
					i = if (p1 != 0) p2 else i + 3
				}
				6 -> {
					val p1 = value(memory, i + 1, paramMode1)
					val p2 = value(memory, i + 2, paramMode2)
					i = if (p1 == 0) p2 else i + 3
				}
				7 -> {
					val p1 = value(memory, i + 1, paramMode1)
					val p2 = value(memory, i + 2, paramMode2)
					memory[memory[i + 3]] = if (p1 < p2) 1 else 0
					i += 4
				}
				8 -> {
					val p1 = value(memory, i + 1, paramMode1)
					val p2 = value(memory, i + 2, paramMode2)
					memory[memory[i + 3]] = if (p1 == p2) 1 else 0
					i += 4
				}
				99 -> break
			}
		}
		return output
	}

	fun part1(input: List<String>): Int {
		val memory = parse(input)
		return run(memory, 1)
	}

	fun part2(input: List<String>): Int {
		val memory = parse(input)
		return run(memory, 5)
	}

	val input = readInput("Day05")
	part1(input).println()
	part2(input).println()
}
