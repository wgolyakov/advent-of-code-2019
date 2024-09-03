fun main() {
	fun parse(line: String) = line.split(',').map { it.toLong() }.toMutableList()
	fun parse(input: List<String>) = parse(input[0])

	fun value(program: List<Long>, memory: Map<Long, Long>, address: Int, paramMode: Int, relativeBase: Long): Long {
		val x = program[address]
		return when (paramMode) {
			0 -> if (x < program.size) program[x.toInt()] else memory[x] ?: 0
			1 -> x
			2 -> {
				val r = relativeBase + x
				if (r < program.size) program[r.toInt()] else memory[r] ?: 0
			}
			else -> error("Wrong parameter mode: $paramMode")
		}
	}

	fun setValue(program: MutableList<Long>, memory: MutableMap<Long, Long>,
				 address: Int, paramMode: Int, relativeBase: Long, value: Long) {
		val x = program[address]
		if (paramMode == 0) {
			if (x < program.size) program[x.toInt()] = value else memory[x] = value
		} else if (paramMode == 2) {
			val r = relativeBase + x
			if (r < program.size) program[r.toInt()] = value else memory[r] = value
		} else error("Wrong parameter mode: $paramMode")
	}

	fun run(program: MutableList<Long>, input: Long): List<Long> {
		val memory = mutableMapOf<Long ,Long>()
		val output = mutableListOf<Long>()
		var relativeBase = 0L
		var i = 0
		while (i < program.size) {
			val instr = program[i]
			val opcode = (instr % 100).toInt()
			val paramMode1 = (instr % 1000).toInt() / 100
			val paramMode2 = (instr % 10000).toInt() / 1000
			val paramMode3 = (instr % 1000000).toInt() / 10000
			when (opcode) {
				1 -> {
					val p1 = value(program, memory, i + 1, paramMode1, relativeBase)
					val p2 = value(program, memory, i + 2, paramMode2, relativeBase)
					setValue(program, memory, i + 3, paramMode3, relativeBase, p1 + p2)
					i += 4
				}
				2 -> {
					val p1 = value(program, memory, i + 1, paramMode1, relativeBase)
					val p2 = value(program, memory, i + 2, paramMode2, relativeBase)
					setValue(program, memory, i + 3, paramMode3, relativeBase, p1 * p2)
					i += 4
				}
				3 -> {
					setValue(program, memory, i + 1, paramMode1, relativeBase, input)
					i += 2
				}
				4 -> {
					output.add(value(program, memory, i + 1, paramMode1, relativeBase))
					i += 2
				}
				5 -> {
					val p1 = value(program, memory, i + 1, paramMode1, relativeBase)
					val p2 = value(program, memory, i + 2, paramMode2, relativeBase).toInt()
					i = if (p1 != 0L) p2 else i + 3
				}
				6 -> {
					val p1 = value(program, memory, i + 1, paramMode1, relativeBase)
					val p2 = value(program, memory, i + 2, paramMode2, relativeBase).toInt()
					i = if (p1 == 0L) p2 else i + 3
				}
				7 -> {
					val p1 = value(program, memory, i + 1, paramMode1, relativeBase)
					val p2 = value(program, memory, i + 2, paramMode2, relativeBase)
					setValue(program, memory, i + 3, paramMode3, relativeBase, if (p1 < p2) 1 else 0)
					i += 4
				}
				8 -> {
					val p1 = value(program, memory, i + 1, paramMode1, relativeBase)
					val p2 = value(program, memory, i + 2, paramMode2, relativeBase)
					setValue(program, memory, i + 3, paramMode3, relativeBase, if (p1 == p2) 1 else 0)
					i += 4
				}
				9 -> {
					relativeBase += value(program, memory, i + 1, paramMode1, relativeBase)
					i += 2
				}
				99 -> break
			}
		}
		return output
	}

	fun run(line: String): List<Long> {
		val program = parse(line)
		return run(program, 0)
	}

	fun part1(input: List<String>): Long {
		val program = parse(input)
		return run(program, 1).single()
	}

	fun part2(input: List<String>): Long {
		val program = parse(input)
		return run(program, 2).single()
	}

	val testInput = readInput("Day09_test")
	check(run(testInput[0]) == listOf<Long>(109,1,204,-1,1001,100,1,100,1008,100,16,101,1006,101,0,99))
	check(run(testInput[1]) == listOf(34915192L * 34915192L))
	check(run(testInput[2]) == listOf(1125899906842624))

	val input = readInput("Day09")
	part1(input).println()
	part2(input).println()
}
