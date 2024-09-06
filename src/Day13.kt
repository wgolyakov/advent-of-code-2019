import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sign

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

	suspend fun paint(input: List<String>): MutableMap<Pair<Int, Int>, Int> {
		val program = parse(input)
		val inQueue = Channel<Long>(10)
		val outQueue = Channel<Long>(10)
		val map = mutableMapOf<Pair<Int, Int>, Int>()
		coroutineScope {
			val job = launch {
				while (isActive) {
					val x = outQueue.receive().toInt()
					val y = outQueue.receive().toInt()
					val tileId = outQueue.receive().toInt()
					map[x to y] = tileId
				}
			}
			run(program, inQueue, outQueue)
			job.cancelAndJoin()
		}
		return map
	}

	suspend fun game(input: List<String>): Int {
		val program = parse(input)
		val inQueue = Channel<Long>(10)
		val outQueue = Channel<Long>(10)
		val map = mutableMapOf<Pair<Int, Int>, Int>()
		var blocks: Int
		var score = 0
		do {
			program[0] = 2
			coroutineScope {
				var ballX = -1
				var paddleX = -1
				val job = launch {
					while (isActive) {
						val x = outQueue.receive().toInt()
						val y = outQueue.receive().toInt()
						val tileId = outQueue.receive().toInt()
						if (x == -1 && y == 0) {
							score = tileId
							continue
						}
						map[x to y] = tileId
						if (tileId == 3) { // paddle
							if (paddleX == -1) {
								val joystick = (ballX - x).sign
								inQueue.send(joystick.toLong())
							}
							paddleX = x
						} else if (tileId == 4) { // ball
							if (paddleX != -1) {
								val joystick = (x - paddleX).sign
								inQueue.send(joystick.toLong())
							}
							ballX = x
						}
					}
				}
				run(program, inQueue, outQueue)
				job.cancelAndJoin()
			}
			blocks = map.count { (_, tileId) -> tileId == 2 }
		} while (blocks > 0)
		return score
	}

	suspend fun part1(input: List<String>): Int {
		val map = paint(input)
		return map.count { (_, tileId) -> tileId == 2 }
	}

	suspend fun part2(input: List<String>): Int {
		return game(input)
	}

	val input = readInput("Day13")
	part1(input).println() // 357
	part2(input).println() // 17468
}
