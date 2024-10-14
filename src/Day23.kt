import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

@ExperimentalCoroutinesApi
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

	suspend fun part1(input: List<String>): Int {
		val program = parse(input)
		val inQueues = Array(50) { Channel<Long>(200) }
		val outQueues = Array(50) { Channel<Long>(200) }
		var result = -1
		coroutineScope {
			val jobs = Array(50) { launch { run(program.toMutableList(), inQueues[it], outQueues[it]) } }
			for (i in 0 until 50) inQueues[i].send(i.toLong())
			while (result == -1) {
				for (i in 0 until 50) {
					val outQueue = outQueues[i]
					val res = outQueue.tryReceive()
					if (!res.isSuccess) continue
					val address = res.getOrThrow().toInt()
					val x = outQueue.receive()
					val y = outQueue.receive()
					if (address == 255) {
						result = y.toInt()
						break
					}
					val inQueue = inQueues[address]
					inQueue.send(x)
					inQueue.send(y)
				}
				for (i in 0 until 50) if (inQueues[i].isEmpty) inQueues[i].send(-1)
				for (i in 0 until 50)	while (!inQueues[i].isEmpty) delay(10)
			}
			for (job in jobs) job.cancelAndJoin()
		}
		return result
	}

	suspend fun part2(input: List<String>): Long {
		val program = parse(input)
		val inQueues = Array(50) { Channel<Long>(200) }
		val outQueues = Array(50) { Channel<Long>(200) }
		var result = -1L
		var natX = -1L
		var natY = -1L
		var sentNatY = -1L
		coroutineScope {
			val jobs = Array(50) { launch { run(program.toMutableList(), inQueues[it], outQueues[it]) } }
			for (i in 0 until 50) inQueues[i].send(i.toLong())
			while (result == -1L) {
				var idle = true
				for (i in 0 until 50) {
					val outQueue = outQueues[i]
					val res = outQueue.tryReceive()
					if (!res.isSuccess) continue
					val address = res.getOrThrow().toInt()
					val x = outQueue.receive()
					val y = outQueue.receive()
					idle = false
					if (address == 255) {
						natX = x
						natY = y
						continue
					}
					val inQueue = inQueues[address]
					inQueue.send(x)
					inQueue.send(y)
				}
				if (idle && natX != -1L) {
					inQueues[0].send(natX)
					inQueues[0].send(natY)
					if (sentNatY == natY) {
						result = natY
						break
					}
					sentNatY = natY
				}
				for (i in 0 until 50) if (inQueues[i].isEmpty) inQueues[i].send(-1)
				for (i in 0 until 50)	while (!inQueues[i].isEmpty) delay(10)
			}
			for (job in jobs) job.cancelAndJoin()
		}
		return result
	}

	val input = readInput("Day23")
	part1(input).println() // 17949
	part2(input).println() // 12326
}
