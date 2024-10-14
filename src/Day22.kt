fun main() {
	abstract class Shuffle(val deckSize: Long) {
		abstract fun shuffle(i: Long): Long
	}

	class DealIntoNewStack(deckSize: Long): Shuffle(deckSize) {
		override fun shuffle(i: Long) = deckSize - 1 - i
	}

	class Cut(deckSize: Long, val n: Long): Shuffle(deckSize) {
		override fun shuffle(i: Long) = (i + deckSize - n) % deckSize
	}

	class DealWithIncrement(deckSize: Long, val n: Long): Shuffle(deckSize) {
		override fun shuffle(i: Long) = ((i.toBigInteger() * n.toBigInteger()) % deckSize.toBigInteger()).toLong()
	}

	fun parse(input: List<String>, deckSize: Long): List<Shuffle> {
		val shuffles = mutableListOf<Shuffle>()
		for (line in input) {
			if (line == "deal into new stack") {
				shuffles.add(DealIntoNewStack(deckSize))
			} else if (line.startsWith("cut ")) {
				val n = line.substringAfterLast(' ').toLong()
				shuffles.add(Cut(deckSize, n))
			} else if (line.startsWith("deal with increment ")) {
				val n = line.substringAfterLast(' ').toLong()
				shuffles.add(DealWithIncrement(deckSize, n))
			} else error("Wrong shuffle: $line")
		}
		return shuffles
	}

	fun shuffleIndex(shuffles: List<Shuffle>, t: Long): Long {
		var i = t
		for (sh in shuffles) {
			i = sh.shuffle(i)
		}
		return i
	}

	fun dealIntoNewStack(deck: IntArray) {
		deck.reverse()
	}

	fun cut(deck: IntArray, n: Int) {
		if (n > 0) {
			val cutCards = deck.sliceArray(0 until n)
			deck.copyInto(deck, 0, n)
			cutCards.copyInto(deck, deck.size - n)
		} else {
			val yourDeck = deck.sliceArray(0 until deck.size + n)
			deck.copyInto(deck, 0, deck.size + n)
			yourDeck.copyInto(deck, -n)
		}
	}

	fun dealWithIncrement(deck: IntArray, n: Int) {
		val copy = deck.copyOf()
		for (i in copy.indices) {
			deck[i * n % deck.size] = copy[i]
		}
	}

	fun shuffle(input: List<String>, deckSize: Int): IntArray {
		val deck = IntArray(deckSize) { it }
		for (line in input) {
			if (line == "deal into new stack") {
				dealIntoNewStack(deck)
			} else if (line.startsWith("cut ")) {
				val n = line.substringAfterLast(' ').toInt()
				cut(deck, n)
			} else if (line.startsWith("deal with increment ")) {
				val n = line.substringAfterLast(' ').toInt()
				dealWithIncrement(deck, n)
			} else error("Wrong shuffle: $line")
		}
		return deck
	}

	fun simplify(shuffles: List<Shuffle>): List<Shuffle> {
		val deckSize = shuffles.first().deckSize
		val result = mutableListOf<Shuffle>()
		// Simplify "deal with increment"
		val n = shuffles.filterIsInstance<DealWithIncrement>().map { it.n }
			.reduce { n1, n2 -> ((n1.toBigInteger() * n2.toBigInteger()) % deckSize.toBigInteger()).toLong() }
		result.add(DealWithIncrement(deckSize, n))
		// Simplify "deal into new stack"
		val dinsCount = shuffles.count { it is DealIntoNewStack }
		if (dinsCount % 2 != 0) result.add(DealIntoNewStack(deckSize))
		// Simplify "cut"
		val i0 = shuffleIndex(shuffles, 0)
		val i1 = shuffleIndex(result, 0)
		val nc = (i1 - i0 + deckSize) % deckSize
		result.add(Cut(deckSize, nc))
		return result
	}

	fun simplifyTimes(shuffles: List<Shuffle>, t: Long): List<Shuffle> {
		val shufflesT = mutableListOf<Shuffle>()
		for (j in 1..t) shufflesT.addAll(shuffles)
		return simplify(shufflesT)
	}

	fun part1(input: List<String>): Int {
		val cards = shuffle(input, 10007)
		return cards.indexOf(2019)
	}

	fun part2(input: List<String>): Long {
		val deckSize = 119315717514047
		val backTimes = 101741582076661
		val period = deckSize - 1
		val times = period - backTimes
		val shuffles0 = parse(input, deckSize)
		val shuffles1 = simplify(shuffles0)
		val shuffleGroups = mutableListOf(shuffles1)
		var j = 0
		var t = 10L
		while (t <= times) {
			val shufflesT = simplifyTimes(shuffleGroups[j++], 10)
			shuffleGroups.add(shufflesT)
			t *= 10L
		}
		t /= 10L

		var i = 2020L
		var n = times
		while (n > 0) {
			while (t <= n) {
				i = shuffleIndex(shuffleGroups[j], i)
				n -= t
			}
			j--
			t /= 10L
		}
		return i
	}

	val testInput = readInput("Day22_test")
	val testInput2 = readInput("Day22_test2")
	val testInput3 = readInput("Day22_test3")
	val testInput4 = readInput("Day22_test4")
	check(shuffle(testInput, 10).toList() == listOf(0, 3, 6, 9, 2, 5, 8, 1, 4, 7))
	check(shuffle(testInput2, 10).toList() == listOf(3, 0, 7, 4, 1, 8, 5, 2, 9, 6))
	check(shuffle(testInput3, 10).toList() == listOf(6, 3, 0, 7, 4, 1, 8, 5, 2, 9))
	check(shuffle(testInput4, 10).toList() == listOf(9, 2, 5, 8, 1, 4, 7, 0, 3, 6))

	val input = readInput("Day22")
	part1(input).println() // 3589
	part2(input).println() // 4893716342290
}
