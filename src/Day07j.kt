import java.util.concurrent.ArrayBlockingQueue
import kotlin.concurrent.thread

fun main() {
	fun parse(line: String) = line.split(',').map { it.toInt() }.toMutableList()

	fun value(memory: List<Int>, address: Int, paramMode: Int): Int {
		return when (paramMode) {
			0 -> memory[memory[address]]
			1 -> memory[address]
			else -> error("Wrong parameter mode: $paramMode")
		}
	}

	fun run(memory: MutableList<Int>, input: ArrayBlockingQueue<Int>, output: ArrayBlockingQueue<Int>) {
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
					memory[memory[i + 1]] = input.take()
					i += 2
				}
				4 -> {
					output.put(value(memory, i + 1, paramMode1))
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
	}

	fun run(memory: MutableList<Int>, input: List<Int>): Int {
		val inQueue = ArrayBlockingQueue(10, false, input)
		val outQueue = ArrayBlockingQueue<Int>(10)
		run(memory, inQueue, outQueue)
		return outQueue.take()
	}

	fun part1(line: String): Int {
		val memory = parse(line)
		var maxSignal = 0
		for (a in 0..4) {
			val s1 = run(memory.toMutableList(), listOf(a, 0))
			for (b in 0..4) {
				if (b == a) continue
				val s2 = run(memory.toMutableList(), listOf(b, s1))
				for (c in 0..4) {
					if (c == b || c == a) continue
					val s3 = run(memory.toMutableList(), listOf(c, s2))
					for (d in 0..4) {
						if (d == c || d == b || d == a) continue
						val s4 = run(memory.toMutableList(), listOf(d, s3))
						for (e in 0..4) {
							if (e == d || e == c || e == b || e == a) continue
							val s5 = run(memory.toMutableList(), listOf(e, s4))
							if (s5 > maxSignal) maxSignal = s5
						}
					}
				}
			}
		}
		return maxSignal
	}

	fun part2(line: String): Int {
		val memory = parse(line)
		var maxSignal = 0
		for (a in 5..9) {
			for (b in 5..9) {
				if (b == a) continue
				for (c in 5..9) {
					if (c == b || c == a) continue
					for (d in 5..9) {
						if (d == c || d == b || d == a) continue
						for (e in 5..9) {
							if (e == d || e == c || e == b || e == a) continue
							val queue1 = ArrayBlockingQueue(10, false, listOf(a, 0))
							val queue2 = ArrayBlockingQueue(10, false, listOf(b))
							val queue3 = ArrayBlockingQueue(10, false, listOf(c))
							val queue4 = ArrayBlockingQueue(10, false, listOf(d))
							val queue5 = ArrayBlockingQueue(10, false, listOf(e))
							thread { run(memory.toMutableList(), queue1, queue2) }
							thread { run(memory.toMutableList(), queue2, queue3) }
							thread { run(memory.toMutableList(), queue3, queue4) }
							thread { run(memory.toMutableList(), queue4, queue5) }
							val last = thread { run(memory.toMutableList(), queue5, queue1) }
							last.join()
							val signal = queue1.take()
							if (signal > maxSignal) maxSignal = signal
						}
					}
				}
			}
		}
		return maxSignal
	}

	val testInput = readInput("Day07_test")
	check(part1(testInput[0]) == 43210)
	check(part1(testInput[1]) == 54321)
	check(part1(testInput[2]) == 65210)
	check(part2(testInput[3]) == 139629729)
	check(part2(testInput[4]) == 18216)

	val input = readInput("Day07")
	part1(input[0]).println()
	part2(input[0]).println()
}
