import kotlin.math.abs

fun main() {
	val pattern1 = listOf(0, 1, 0, -1)

	fun getFromPattern(e: Int, i: Int): Int {
		val a = e + 1
		val patternSize = pattern1.size * a
		val b = (i + 1) % patternSize
		return pattern1[b / a]
	}

	fun doPhase(signal: List<Int>): List<Int> {
		val result = mutableListOf<Int>()
		for (element in signal.indices) {
			var r = 0
			for ((i, d) in signal.withIndex()) {
				r += d * getFromPattern(element, i)
			}
			result.add(abs(r) % 10)
		}
		return result
	}

	fun doPhase10000(signalTail: IntArray, signalSize: Int, msgOffset: Int): IntArray {
		val result = IntArray(signalSize - msgOffset)
		var r = 0
		// All elements in pattern for tail digits is 1
		for (i in signalSize - 1 downTo msgOffset) {
			r += signalTail[i - msgOffset]
			result[i - msgOffset] = abs(r) % 10
		}
		return result
	}

	fun part1(input: String, phases: Int = 100): String {
		var signal = input.map { it.digitToInt() }
		for (phase in 1..phases) {
			signal = doPhase(signal)
		}
		return signal.take(8).joinToString("")
	}

	fun part2(input: String): String {
		val signal1 = input.map { it.digitToInt() }
		val signalSize = signal1.size * 10000
		val msgOffset = input.substring(0, 7).toInt()
		var signalTail = IntArray(signalSize - msgOffset) { signal1[(it + msgOffset) % signal1.size] }
		for (phase in 1..100) {
			signalTail = doPhase10000(signalTail, signalSize, msgOffset)
		}
		return signalTail.take(8).joinToString("")
	}

	val testInput = readInput("Day16_test")
	check(part1(testInput[0], 4) == "01029498")
	check(part1(testInput[1]) == "24176176")
	check(part1(testInput[2]) == "73745418")
	check(part1(testInput[3]) == "52432133")
	check(part2(testInput[4]) == "84462026")
	check(part2(testInput[5]) == "78725270")
	check(part2(testInput[6]) == "53553731")

	val input = readInput("Day16")
	part1(input[0]).println()
	part2(input[0]).println()
}
