import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

suspend fun main() {
	fun parse(input: List<String>) = input[0].split(',').map { it.toLong() }.toMutableList()

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

	suspend fun run(program: MutableList<Long>, input: Channel<Long>, output: Channel<Long>) {
		val memory = mutableMapOf<Long ,Long>()
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
					setValue(program, memory, i + 1, paramMode1, relativeBase, input.receive())
					i += 2
				}
				4 -> {
					output.send(value(program, memory, i + 1, paramMode1, relativeBase))
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
	}

	fun encode(command: String): List<Long> {
		return command.map { it.code.toLong() } + 10L
	}

	suspend fun moveSpringDroid(input: List<String>, script: List<String>): Int {
		val program = parse(input)
		val inQueue = Channel<Long>(10)
		val outQueue = Channel<Long>(10)
		var result = 0
		coroutineScope {
			val job = launch {
				while (isActive) {
					result = outQueue.receive().toInt()
					//print(result.toChar())
				}
			}
			launch {
				for (line in script) {
					for (n in encode(line)) inQueue.send(n)
				}
			}
			run(program, inQueue, outQueue)
			delay(100)
			job.cancelAndJoin()
		}
		return result
	}

	suspend fun part1(input: List<String>): Int {
		val script = listOf(
			// #####.###########
			"NOT A J",
			// #####..#.########
			"NOT B T",
			"OR T J",
			"NOT C T",
			"OR T J",
			// #####...#########
			"AND D J",
			"WALK"
		)
		return moveSpringDroid(input, script)
	}

	suspend fun part2(input: List<String>): Int {
		val script = listOf(
			// #####.###########
			"NOT A J",
			// #####..#.########
			"NOT B T",
			"OR T J",
			"NOT C T",
			"OR T J",
			// #####...#########
			"AND D J",
			// #####.#.##.#.####
			"NOT E T",
			"NOT T T", // <- main trick
			"OR H T",
			"AND T J",
			"RUN"
		)
		return moveSpringDroid(input, script)
	}

	val input = readInput("Day21")
	part1(input).println() // 19362259
	part2(input).println() // 1141066762
}
