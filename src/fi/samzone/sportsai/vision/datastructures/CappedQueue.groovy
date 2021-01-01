package fi.samzone.sportsai.vision.datastructures

class CappedQueue implements Serializable {

	LinkedList elements = new LinkedList()
	int capSize

	public CappedQueue() {
		this(100)
	}

	public CappedQueue(int capSize) {
		this.capSize = capSize
	}

	public CappedQueue push(newElement) {
		elements.push(newElement)
		if (elements.size() > capSize)
			elements = elements.init()
		return this
	}

	public CappedQueue leftShift(newElement) {
		return this.push(newElement)
	}

	public Integer size() {
		return elements.size()
	}

	public String toString() {
		return elements.toString()
	}

	public static void main(String[] args) {
		CappedQueue l = new CappedQueue(3)
		l << 1 << 2 << 3 << 4
		println l
	}

}
