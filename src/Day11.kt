import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private enum class Direction(val dx: Int, val dy: Int) {
	Up(0, -1),
	Down(0, 1),
	Left(-1, 0),
	Right(1, 0);

	fun left(): Direction {
		return when (this) {
			Up -> Left
			Down -> Right
			Left -> Down
			Right -> Up
		}
	}

	fun right(): Direction {
		return when (this) {
			Up -> Right
			Down -> Left
			Left -> Up
			Right -> Down
		}
	}
}

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

	suspend fun paint(input: List<String>, firstColor: Long): MutableMap<Pair<Int, Int>, Long> {
		val program = parse(input)
		val inQueue = Channel<Long>(10)
		val outQueue = Channel<Long>(10)
		val map = mutableMapOf<Pair<Int, Int>, Long>()
		map[0 to 0] = firstColor
		coroutineScope {
			val job = launch {
				var x = 0
				var y = 0
				var direction = Direction.Up
				while (isActive) {
					inQueue.send(map[x to y] ?: 0)
					map[x to y] = outQueue.receive()
					val turn = outQueue.receive()
					direction = if (turn == 0L) direction.left() else direction.right()
					x += direction.dx
					y += direction.dy
				}
			}
			run(program, inQueue, outQueue)
			job.cancelAndJoin()
		}
		return map
	}

	suspend fun part1(input: List<String>): Int {
		val map = paint(input, 0)
		return map.size
	}

	suspend fun part2(input: List<String>): String {
		val map = paint(input, 1)
		val minX = map.keys.minOf { it.first }
		val minY = map.keys.minOf { it.second }
		val maxX = map.keys.maxOf { it.first }
		val maxY = map.keys.maxOf { it.second }
		val width = maxX - minX + 1
		val height = maxY - minY + 1
		val grid = Array(height) {StringBuilder(" ".repeat(width))}
		for ((coordinates, color) in map) {
			val (x, y) = coordinates
			grid[y - minY][x - minX] = if (color == 1L) '#' else ' '
		}
		return grid.joinToString("\n")
	}

	val input = readInput("Day11")
	part1(input).println()
	part2(input).println()
}
