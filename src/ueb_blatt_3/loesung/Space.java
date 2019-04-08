package ueb_blatt_3.loesung;

public class Space {
	// 0 = keine Verbindung; 1 = Verbindung; 2 = Wurmloch
	private int[][] adjacencyMatrix;

	public int[][] getAdjacencyMatrix() {
		return adjacencyMatrix;
	}

	public Space(int[][] adjacencyMatrix) {
		this.adjacencyMatrix = adjacencyMatrix;
	}

	public static void main(String[] args) {
		//@formatter:off
		int[][] matrix = { { 0, 1, 0, 1, 0 }, 
						   { 0, 1, 2, 1, 0 }, 
						   { 1, 0, 1, 0, 1 }, 
						   { 0, 1, 0, 0, 0 },
						   { 1, 0, 2, 0, 0 } };
		//@formatter:on
		// 0 -> 1
		// 1 -> 1, 1 -> 3, 1 => 2
		// 2 -> 0, 2 -> 2, 2 -> 4
		// 3 -> 1
		// 4 -> 1, 4 => 2
		// -> normal connection, => wormhole

		Space space = new Space(matrix);

		PenguinAstronaut pingu = new PenguinAstronaut(space, 0, 4);
		pingu.start();
	}
}