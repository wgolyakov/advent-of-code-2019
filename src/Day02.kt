fun main() {
	fun parse(line: String) = line.split(',').map { it.toInt() }.toMutableList()
	fun parse(input: List<String>) = parse(input[0])

	fun run(memory: MutableList<Int>) {
		for (i in memory.indices step 4) {
			when (memory[i]) {
				1 -> memory[memory[i + 3]] = memory[memory[i + 1]] + memory[memory[i + 2]]
				2 -> memory[memory[i + 3]] = memory[memory[i + 1]] * memory[memory[i + 2]]
				99 -> return
			}
		}
	}

	fun run(line: String): MutableList<Int> {
		val memory = parse(line)
		run(memory)
		return memory
	}

	fun run(memory: MutableList<Int>, noun: Int, verb: Int): Int {
		memory[1] = noun
		memory[2] = verb
		run(memory)
		return memory[0]
	}

	fun part1(input: List<String>): Int {
		val memory = parse(input)
		return run(memory, 12, 2)
	}

	fun part2(input: List<String>): Int {
		val memory = parse(input)
		for (noun in 0 .. 99) {
			for (verb in 0 .. 99) {
				val output = run(memory.toMutableList(), noun, verb)
				if (output == 19690720) return 100 * noun + verb
			}
		}
		return -1
	}

	val testInput = readInput("Day02_test")
	check(run(testInput[0]) == listOf(2,0,0,0,99))
	check(run(testInput[1]) == listOf(2,3,0,6,99))
	check(run(testInput[2]) == listOf(2,4,4,5,99,9801))
	check(run(testInput[3]) == listOf(30,1,1,4,2,5,6,0,99))

	val input = readInput("Day02")
	part1(input).println()
	part2(input).println()
}
