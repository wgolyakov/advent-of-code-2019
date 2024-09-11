import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private enum class Move(val dx: Int, val dy: Int) {
	North(0, -1),
	South(0, 1),
	West(-1, 0),
	East(1, 0);
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

	suspend fun exploreMap(input: List<String>): MutableMap<Pair<Int, Int>, Int> {
		val program = parse(input)
		val inQueue = Channel<Long>(10)
		val outQueue = Channel<Long>(10)
		val map = mutableMapOf<Pair<Int, Int>, Int>()
		coroutineScope {
			val job = launch { run(program, inQueue, outQueue) }
			var x = 0
			var y = 0
			var oxygenFound = false
			map[0 to 0] = 1
			var i = 0
			while (!oxygenFound || i < 1_000_000) {
				val move = Move.entries.random()
				inQueue.send(move.ordinal + 1L)
				val status = outQueue.receive().toInt()
				when (status) {
					0 -> map[x + move.dx to y + move.dy] = 0
					1 -> {
						x += move.dx
						y += move.dy
						map[x to y] = 1
					}
					2 -> {
						x += move.dx
						y += move.dy
						map[x to y] = 2
						oxygenFound = true
					}
				}
				i++
			}
			job.cancelAndJoin()
		}
		return map
	}

	fun bfsToOxygen(map: Map<Pair<Int, Int>, Int>): Int {
		val distances = mutableMapOf<Pair<Int, Int>, Int>()
		val queue = mutableListOf<Pair<Int, Int>>()
		distances[0 to 0] = 0
		queue.add(0 to 0)
		while (queue.isNotEmpty()) {
			val curr = queue.removeFirst()
			if (map[curr] == 2) return distances[curr]!!
			val (x, y) = curr
			val neighbors = listOf(x + 1 to y, x - 1 to y, x to y + 1, x to y - 1).filter { (map[it] ?: 0) != 0 }
			for (next in neighbors) {
				if (next !in distances) {
					distances[next] = distances[curr]!! + 1
					queue.add(next)
				}
			}
		}
		return -1
	}

	fun bfsFromOxygen(map: Map<Pair<Int, Int>, Int>): Int {
		val ox = map.filterValues { it == 2 }.keys.single()
		val distances = mutableMapOf<Pair<Int, Int>, Int>()
		val queue = mutableListOf<Pair<Int, Int>>()
		distances[ox] = 0
		queue.add(ox)
		while (queue.isNotEmpty()) {
			val curr = queue.removeFirst()
			val (x, y) = curr
			val neighbors = listOf(x + 1 to y, x - 1 to y, x to y + 1, x to y - 1).filter { (map[it] ?: 0) != 0 }
			for (next in neighbors) {
				if (next !in distances) {
					distances[next] = distances[curr]!! + 1
					queue.add(next)
				}
			}
		}
		return distances.values.max()
	}

	suspend fun part1(input: List<String>): Int {
		val map = exploreMap(input)
		return bfsToOxygen(map)
	}

	suspend fun part2(input: List<String>): Int {
		val map = exploreMap(input)
		return bfsFromOxygen(map)
	}

	val input = readInput("Day15")
	part1(input).println() // 262
	part2(input).println() // 314
}
