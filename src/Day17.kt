import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

private enum class Dir(val dx: Int, val dy: Int) {
	Up(0, -1),
	Down(0, 1),
	Left(-1, 0),
	Right(1, 0);

	fun left(): Dir {
		return when (this) {
			Up -> Left
			Down -> Right
			Left -> Down
			Right -> Up
		}
	}

	fun right(): Dir {
		return when (this) {
			Up -> Right
			Down -> Left
			Left -> Up
			Right -> Down
		}
	}

	fun back(): Dir {
		return when (this) {
			Up -> Down
			Down -> Up
			Left -> Right
			Right -> Left
		}
	}

	fun getTurn(to: Dir) = when (to) {
		left() -> 'L'
		right() -> 'R'
		else -> null
	}

	companion object {
		fun dir(c: Char) = when(c) {
			'^' -> Up
			'v' -> Down
			'<' -> Left
			'>' -> Right
			else -> error("Wrong robot symbol: $c")
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

	suspend fun loadMap(input: List<String>): List<StringBuilder> {
		val program = parse(input)
		val inQueue = Channel<Long>(10)
		val outQueue = Channel<Long>(10)
		val map = mutableListOf(StringBuilder())
		coroutineScope {
			val job = launch {
				var row = map.first()
				while (isActive) {
					val code = outQueue.receive().toInt()
					if (code == 10) {
						row = StringBuilder()
						map.add(row)
						continue
					}
					row.append(code.toChar())
				}
			}
			run(program, inQueue, outQueue)
			delay(100)
			job.cancelAndJoin()
		}
		return map.filter { it.isNotEmpty() }
	}

	// .#.
	// ###
	// .#.
	fun findIntersections(map: List<StringBuilder>): List<Pair<Int, Int>> {
		val result = mutableListOf<Pair<Int, Int>>()
		for ((y, row) in map.withIndex()) {
			if (y == 0 || y == map.size - 1) continue
			for ((x, c) in row.withIndex()) {
				if (x == 0 || x == row.length - 1) continue
				if (c == '#' && row[x - 1] == '#' && row[x + 1] == '#'
					&& map[y - 1][x] == '#' && map[y + 1][x] == '#') {
					result.add(x to y)
				}
			}
		}
		return result
	}

	fun findRobot(map: List<CharSequence>): Pair<Int, Int> {
		val robotSymbols = setOf('^', 'v', '<', '>')
		for ((y, row) in map.withIndex()) {
			for ((x, c) in row.withIndex()) {
				if (c in robotSymbols) return x to y
			}
		}
		error("Can't find robot")
	}

	fun isScaffold(x: Int, y: Int, dir: Dir, map: List<CharSequence>): Boolean {
		val a = x + dir.dx
		val b = y + dir.dy
		return a >= 0 && b >=0 && b < map.size && a < map[b].length && map[b][a] == '#'
	}

	fun findPath(map: List<CharSequence>): String {
		var (x, y) = findRobot(map)
		var dir = Dir.dir(map[y][x])
		val path = mutableListOf<String>()
		while (true) {
			val ways = (Dir.entries - dir.back()).filter { isScaffold(x, y, it, map) }
			if (ways.isEmpty()) break
			val nextDir = ways.single()
			val turnSymbol = dir.getTurn(nextDir)
			if (turnSymbol != null) path.add(turnSymbol.toString())
			dir = nextDir
			var step = 0
			while (isScaffold(x, y, dir, map)) {
				step++
				x += dir.dx
				y += dir.dy
			}
			path.add(step.toString())
		}
		return path.joinToString(",")
	}

	fun cut(path: List<String>, s: String): List<String> {
		val result = mutableListOf<String>()
		for (p in path) {
			if (p.contains(s)) {
				val list = p.split(s).map { if (it.startsWith(',')) it.drop(1) else it }
					.map { if (it.endsWith(',')) it.dropLast(1) else it }
					.filter { it.isNotEmpty() }
				result.addAll(list)
			} else {
				result.add(p)
			}
		}
		return result
	}

	fun split(path: String): List<String> {
		for (s in 20 downTo 7) {
			if (path[s] != ',') continue
			val a = path.substring(0, s)
			if (!a.last().isDigit()) continue
			val path2 = cut(listOf(path), a).sortedBy { it.length }
			val b = path2.first()
			if (b.length > 20) continue
			val path3 = cut(path2, b)
			val c = path3.first()
			val path4 = cut(path3, c)
			if (path4.isNotEmpty()) continue
			return listOf(a, b, c)
		}
		error("Can't split")
	}

	fun createMainRoutine(path: String, movementFunctions: List<String>): String {
		val (a, b, c) = movementFunctions
		val result = StringBuilder()
		var s = path
		while (s.isNotEmpty()) {
			when (s) {
				a -> {
					result.append("A")
					s = ""
				}
				b -> {
					result.append("B")
					s = ""
				}
				c -> {
					result.append("C")
					s = ""
				}
				else -> {
					if (s.startsWith(a)) {
						result.append("A,")
						s = s.substringAfter("$a,")
					} else if (s.startsWith(b)) {
						result.append("B,")
						s = s.substringAfter("$b,")
					} else if (s.startsWith(c)) {
						result.append("C,")
						s = s.substringAfter("$c,")
					}
				}
			}
		}
		return result.toString()
	}

	fun encode(command: String): List<Long> {
		return command.map { it.code.toLong() } + 10L
	}

	suspend fun runRobot(input: List<String>, mainRoutine: String, movementFunctions: List<String>): Int {
		val program = parse(input)
		program[0] = 2
		val inQueue = Channel<Long>(10)
		val outQueue = Channel<Long>(10)
		var dust = 0
		coroutineScope {
			val job = launch {
				while (isActive) {
					dust = outQueue.receive().toInt()
					// print(dust.toChar())
				}
			}
			launch {
				for (n in encode(mainRoutine)) inQueue.send(n)
				for (f in movementFunctions) {
					for (n in encode(f)) inQueue.send(n)
				}
				for (n in encode("n")) inQueue.send(n)
			}
			run(program, inQueue, outQueue)
			delay(100)
			job.cancelAndJoin()
		}
		return dust
	}

	suspend fun part1(input: List<String>): Int {
		val map = loadMap(input)
		// println(map.joinToString("\n"))
		val intersections = findIntersections(map)
		return intersections.sumOf { (x, y) -> x * y }
	}

	suspend fun part2(input: List<String>): Int {
		val map = loadMap(input)
		val path = findPath(map)
		val movementFunctions = split(path)
		val mainRoutine = createMainRoutine(path, movementFunctions)
		return runRobot(input, mainRoutine, movementFunctions)
	}

	val testInput = readInput("Day17_test")
	val testPath = findPath(testInput)
	check(testPath == "R,8,R,8,R,4,R,4,R,8,L,6,L,2,R,4,R,4,R,8,R,8,R,8,L,6,L,2")
	val testMovementFunctions = split(testPath)
	check(testMovementFunctions == listOf("R,8,R,8", "R,8,L,6,L,2", "R,4,R,4"))
	val testMainRoutine = createMainRoutine(testPath, testMovementFunctions)
	check(testMainRoutine == "A,C,B,C,A,B")

	val input = readInput("Day17")
	part1(input).println() // 4600
	part2(input).println() // 1113411
}
