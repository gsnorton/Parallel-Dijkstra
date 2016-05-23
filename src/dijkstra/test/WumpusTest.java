/*
 * Test Dijkstra's Algorithm modules using a maze like the one generated for
 * "Hunt the Wumpus".
 */

/*
 * H/T: Generation of Wumpus maze is based on ideas presented in the "Grand 
 * Theft Wumpus" chapter of Conrad Barski's "Land of Lisp". Copyright (C) 2011.
 * 
 */

package dijkstra.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import dijkstra.engine.DijkstraAlgorithm;
import dijkstra.engine.ParallelDijkstraAlgorithm;
import dijkstra.model.Edge;
import dijkstra.model.Graph;
import dijkstra.model.Vertex;

public class WumpusTest {
	
	private static final int NODE_COUNT = 1000;
	private static final int LANE_COUNT = 10000;
	
	private List<Vertex> nodes;
	private List<Edge> edges;
	
	private static Random rand = new Random(System.currentTimeMillis());
	
	@Test
	public void test() {
		generateMaze();
		
		List<Vertex> path = null;
		
		Graph graph = new Graph(nodes, edges);
		
		DijkstraAlgorithm dijkstra = 
				new DijkstraAlgorithm(graph);
		
//		ParallelDijkstraAlgorithm dijkstra = 
//			new ParallelDijkstraAlgorithm(graph);
		
		int source = 0, start = 0;
		
		for (int n = 0; n < 100; n++) {
			source = rand.nextInt(NODE_COUNT);
			dijkstra.execute(source);
			
			start = source;
			while (start == source)
				start = rand.nextInt(NODE_COUNT);
			
			path = dijkstra.getPath(start);
		}
		
		System.out.println(start + " to " + source);
		
		assertNotNull(path);
		assertTrue(path.size() > 0);
		
		for (Vertex vertex : path) {
			System.out.println(vertex);
		}
	}
	
	private void generateMaze() {
		nodes = new ArrayList<Vertex>(NODE_COUNT);
		edges = new ArrayList<Edge>(LANE_COUNT*2);
		
		for(int n = 0; n < NODE_COUNT; n++) {
			Vertex node = new Vertex(String.format("%d", n));
			nodes.add(node);
		}
		
		int lanes_populated = 0;
		
		while (lanes_populated < LANE_COUNT) {
			int source_num = 0;
			int dest_num = 0;

			while (source_num == dest_num) {
				source_num = rand.nextInt(NODE_COUNT);
				dest_num = rand.nextInt(NODE_COUNT);
			}
			
			String edgeId = String.format("%d_%d", source_num, dest_num);
			Edge edge1 = new Edge(edgeId, nodes.get(source_num), nodes.get(dest_num), 100);
			
			if (edges.contains(edge1)) {
				//System.out.println("REJECTED " + edge1);
				continue;
			}
			
			edgeId = String.format("%d_%d", source_num, dest_num);
			Edge edge2 = new Edge(edgeId, nodes.get(source_num), nodes.get(dest_num), 100);
			
			if (edges.contains(edge2)) {
				//System.out.println("REJECTED " + edge2);
				continue;
			}
						
			//System.out.println("ADDED " + edge1);
			
			edges.add(edge1); edges.add(edge2);
			lanes_populated += 1;
		}
		
		/* Look for "islands" */
		
		int node_num = 0;
		
		for (Vertex node : nodes) {
			boolean is_island = true;
			
			int edge_count = 0;
			
			for (Edge edge : edges) {
				if (edge.getSource() == node || edge.getDestination() == node) {
					is_island = false;
					edge_count += 1;
				}
			}
						
			if(is_island) {
				System.out.println("ISLAND " + node);
				
				Vertex dest = node;
				
				int dest_num = -1;
				
				while(dest == node) {
					dest_num = rand.nextInt(NODE_COUNT);
					dest = nodes.get(dest_num);
				}
				
				String edgeId = String.format("%d_%d", node_num, dest_num);
				Edge edge = new Edge(edgeId, node, dest, 100);
				edges.add(edge);
				
				edgeId = String.format("%d_%d", dest_num, node_num);
				edge = new Edge(edgeId, dest, node, 100);
				edges.add(edge);
			}
			
			node_num += 1;
		}
	}
}
