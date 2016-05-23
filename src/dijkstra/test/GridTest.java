/*
 * BASED ON Dijkstra Java code from
 * 
 * http://www.vogella.com/articles/JavaAlgorithmsDijkstra/article.html
 * 
 * Version 1.1 - Copyright 2009, 2010, 2011, 2011 Lars Vogel
 * 
 * MODIFIED BY Gregory Norton to address performance concerns.
 * 
 * Eclipse Public License
 */

package dijkstra.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

import org.junit.Test;

import dijkstra.engine.ParallelDijkstraAlgorithm;
import dijkstra.engine.DijkstraAlgorithm;

import dijkstra.model.Edge;
import dijkstra.model.Graph;
import dijkstra.model.Vertex;

public class GridTest {

	private List<Vertex> nodes;
	private List<Edge> edges;
	
	private Random rand = new Random();

	@Test
	public void test() {
		nodes = new ArrayList<Vertex>();
		edges = new ArrayList<Edge>();
		
		final int x = 20;
		final int y = 20;
		
		for (int j = 0; j < y; j++) {
			for (int i = 0; i < x; i++) {
				int n = j*x + i;
				
				Vertex location = new Vertex("Node_" + n);
				nodes.add(location);
				
				if(0 < i) addLane(n, n-1, 100);
				if(0 < j) addLane(n, n-x, 100);
				if((0 < j) && (0 < i)) addLane(n, n-x-1, 141);
				if ((0 < j) && ((x - 1) > i)) addLane(n, n-x+1, 141);
			}
		}

		List<Vertex> path = null;
		
		Graph graph = new Graph(nodes, edges);
		
		DijkstraAlgorithm dijkstra = 
				new DijkstraAlgorithm(graph);
		
//		ParallelDijkstraAlgorithm dijkstra = 
//			new ParallelDijkstraAlgorithm(graph);	
			
		int source = 0, start = x*y;
		
		for (int n = 0; n < 100; n++) {
			source = rand.nextInt(x*y);
			dijkstra.execute(source);
			
			start = source;
			while (start == source)
				start = rand.nextInt(x*y);
			
			path = dijkstra.getPath(start);
		}
		
		System.out.println(start + " to " + source);
		
		assertNotNull(path);
		assertTrue(path.size() > 0);
		
		for (Vertex vertex : path) {
			System.out.println(vertex);
		}
	}

	private void addLane(int sourceLocNo, int destLocNo, int duration) {
		String laneId = String.format("lane_%d_%d", sourceLocNo, destLocNo);
		Edge lane = new Edge(laneId, nodes.get(sourceLocNo),
				nodes.get(destLocNo), duration);
		edges.add(lane);

		laneId = String.format("lane_%d_%d", destLocNo, sourceLocNo);
		lane = new Edge(laneId, nodes.get(destLocNo), nodes.get(sourceLocNo),
				duration);
		edges.add(lane);
	}
}
