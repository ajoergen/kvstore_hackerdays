def generateId(n: Int): Stream[Int] = n #:: generateId(n + 1)
generateId(0) take 1
