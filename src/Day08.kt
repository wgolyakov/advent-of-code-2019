fun main() {
	fun part1(input: List<String>, width: Int = 25, height: Int = 6): Int {
		val ls = width * height
		val minZeroLayer = input[0].windowed(ls, ls).minBy { layer -> layer.count { it == '0' } }
		val oneCount = minZeroLayer.count { it == '1' }
		val twoCount = minZeroLayer.count { it == '2' }
		return oneCount * twoCount
	}

	fun part2(input: List<String>, width: Int = 25, height: Int = 6): String {
		val ls = width * height
		val image = Array(height) { StringBuilder("2".repeat(width)) }
		for (layer in input[0].windowed(ls, ls).reversed()) {
			for (y in 0 until height) {
				for (x in 0 until width) {
					val c = layer[y * width + x]
					if (c == '2') continue
					image[y][x] = c
				}
			}
		}
		for (y in 0 until height) {
			for (x in 0 until width) {
				image[y][x] = if (image[y][x] == '0') ' ' else '#'
			}
		}
		return image.joinToString("\n")
	}

	val testInput = readInput("Day08_test")
	check(part2(testInput, 2, 2) == " #\n# ")

	val input = readInput("Day08")
	part1(input).println()
	part2(input).println()
}
