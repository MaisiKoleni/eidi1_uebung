package ueb_blatt_3.loesung;

import java.util.ArrayList;
import java.util.List;

public class PenguinAstronaut extends Thread {
	private static Object[] locks;

	private static synchronized void initLocks(int size) {
		// Initialise lock objects only once
		if (locks == null) {
			locks = new Object[size];
			for (int i = 0; i < locks.length; i++) {
				locks[i] = new Object();
			}
		}
	}

	private Space space;
	private int sizeOfSpace;
	private int start;
	private int to;
	private boolean[] visited;

	// For stopping all Threads at the end
	private PenguinAstronaut parentThread;
	private List<PenguinAstronaut> childThreads;

	// For telling the way afterward
	private List<Integer> visitedLocations;

	// Standard constructor for first Pingu
	public PenguinAstronaut(Space space, int start, int to) {
		this(space, start, to, null, new ArrayList<>());
	}

	// Constructor for every other Pingu
	public PenguinAstronaut(Space space, int start, int to, PenguinAstronaut parentThread, List<Integer> visitedLocs) {
		this.space = space;
		sizeOfSpace = space.getAdjacencyMatrix().length;
		this.start = start;
		this.to = to;
		visited = new boolean[sizeOfSpace];

		this.parentThread = parentThread;
		childThreads = new ArrayList<>();

		this.visitedLocations = visitedLocs;

		initLocks(sizeOfSpace);
	}

	@Override
	public void run() {
		visit(start);
	}

	private void visit(int index) {
		// Way was already found
		if (this.isInterrupted())
			return;

		// Needs to be declared before synchronized
		int nextVisit = 0;
		// Lock current location
		synchronized (locks[index]) {
			// Found location of starfleet
			if (index == to) {
				tellStory();
				stopSearching(this);
				return;
			}
			visitedLocations.add(index);
			visited[index] = true;

			// New Pingus through wormholes.
			/*
			 * matrix[index][i] = 1 means there is a way from the current location to the
			 * beacon with the index i
			 */
			for (int i = 0; i < sizeOfSpace; i++) {
				if (space.getAdjacencyMatrix()[index][i] == 2 && !visited[i]) {
					PenguinAstronaut p = new PenguinAstronaut(space, i, to, this, new ArrayList<>(visitedLocations));
					childThreads.add(p);
					p.start();
				}
			}
			// Visit next beacons
			nextVisit = nextVisit(nextVisit, index);
		}

		while (nextVisit != -1) {
			visit(nextVisit);
			// We're back from a dead end
			synchronized (locks[index]) {
				visitedLocations.remove(visitedLocations.size() - 1);
				// Determine if and where to go next
				nextVisit = nextVisit(nextVisit + 1, index);
				// Possibly going back one step, unlock current beacon
			}
		}
	}

	private int nextVisit(int seachStart, int index) {
		// Looking for the next Place to visit
		for (int i = seachStart; i < sizeOfSpace; i++) {
			if (space.getAdjacencyMatrix()[index][i] == 1 && !visited[i])
				return i;
		}
		return -1;
	}

	private void stopSearching(PenguinAstronaut stopping) {
		// Stop parent if it's not the one stopping
		if (parentThread != null && parentThread != stopping) {
			parentThread.stopSearching(this);
		}
		// Stop every child that is not the one stopping
		for (PenguinAstronaut p : childThreads) {
			if (p != stopping) {
				p.stopSearching(this);
			}
		}
		// Interrupt this Pingu's search
		this.interrupt();
	}

	private void tellStory() {
		for (Integer i : visitedLocations) {
			System.out.println("I was at beacon " + i);
		}
	}
}