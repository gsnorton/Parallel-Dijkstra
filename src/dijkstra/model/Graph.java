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
 * 
 * HT for Fork/Join HowTo: http://www.javacodegeeks.com/2011/02/
 * 							 java-forkjoin-parallel-programming.html
 * 
 */

package dijkstra.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class Graph {
	
	private final List<Vertex> vertexes;
	private final List<Edge> edges;

	public Graph(List<Vertex> vertexes, List<Edge> edges) {
		this.vertexes = vertexes;
		this.edges = edges;		
	}
	
	public Graph(Graph other) {
		this.vertexes = new ArrayList<Vertex>(other.vertexes);
		this.edges = new ArrayList<Edge>(other.edges);
	}

	public List<Vertex> getVertexes() {
		return Collections.unmodifiableList(vertexes);
	}

	public List<Edge> getEdges() {
		return Collections.unmodifiableList(edges);
	}
	
	public static final void addLane(List<Edge> edges, final List<Vertex> nodes, 
			final int source, final int dest, final int cost) {
		String laneId = String.format("Lane_%d_%d", source, dest);
		Edge lane = new Edge(laneId, nodes.get(source), nodes.get(dest), cost);
		edges.add(lane);

		laneId = String.format("Lane_%d_%d", dest, source);
		lane = new Edge(laneId, nodes.get(dest), nodes.get(source), cost);
		edges.add(lane);
	}
	
	/* -------------------------------------------------------------------- */
	
	/* Tasking this was interesting but didn't save a significant amount of 
	 * time when testing against the map node count for the SmashTV game.
	 * Saved for future use (Project 2?)
	 */
	
	static ForkJoinPool fork_join_pool = 
			dijkstra.resources.Concurrency.getForkJoinPool();
	
	private class VertexAdjacenciesTask extends
			RecursiveTask<Map<Vertex, List<Edge>>> {

		private final List<Vertex> vertices;
		
		private VertexAdjacenciesTask va1;
		private VertexAdjacenciesTask va2;

		public VertexAdjacenciesTask(final List<Vertex> vertices) {
			this.vertices = vertices;
			
			int mid = vertices.size() / 2;
			
			if(vertices.size() > 8) {
				va1 = new VertexAdjacenciesTask(vertices
						.subList(0, mid));
				va2 = new VertexAdjacenciesTask(vertices
						.subList(mid, vertices.size()));
			}
		}

		@Override
		public Map<Vertex, List<Edge>> compute() {
			Map<Vertex, List<Edge>> adjacencies = 
					new HashMap<Vertex, List<Edge>>();

			if ((null != va1) && (null != va2)) {
				va1.fork();
				adjacencies.putAll(va2.compute());
				adjacencies.putAll(va1.join());
			} else {
				for (Vertex vertex : vertices)
					adjacencies.put(vertex, getVertexAdjacencies(vertex));
			}

			return Collections.unmodifiableMap(adjacencies);
		}
	}
	
	/* -------------------------------------------------------------------- */
	
	private List<Edge> getVertexAdjacencies(final Vertex vertex) {
		List<Edge> vertex_adjacencies = new ArrayList<Edge>();
		
		for(Edge edge : edges) 
			if(edge.getSource().equals(vertex))
				vertex_adjacencies.add(edge);			
		
		return Collections.unmodifiableList(vertex_adjacencies);
	}
	
	private Map<Vertex, List<Edge>> adjacencies;
	
	public Map<Vertex, List<Edge>> getAdjacencies() {

		if (adjacencies == null) {
			adjacencies = new HashMap<Vertex, List<Edge>>();

			// long start_time = System.nanoTime();

			// No tasking

			for (Vertex vertex : vertexes)
				adjacencies.put(vertex, getVertexAdjacencies(vertex));

			// Fork/Join. Comment out above 'for' to try it.

//			adjacencies = fork_join_pool
//					.invoke(new VertexAdjacenciesTask(vertexes));

			// System.out.println("getAdjacencies() time: "
			// + (System.nanoTime() - start_time));
		}

		return Collections.unmodifiableMap(adjacencies);
	}
	
	public static List<Map<Vertex, List<Edge>>> 
			splitAdjacencies(Map<Vertex, List<Edge>> adjacencies) {

		List<Map<Vertex, List<Edge>>> split_adjacencies = 
				new ArrayList<Map<Vertex, List<Edge>>>();

		Map<Vertex, List<Edge>> adjacencies1 = new HashMap<Vertex, List<Edge>>();
		Map<Vertex, List<Edge>> adjacencies2 = new HashMap<Vertex, List<Edge>>();

		for (Vertex v : adjacencies.keySet()) {
			List<Edge> edges = new ArrayList<Edge>(adjacencies.get(v));

			Collections.shuffle(edges);

			int size = edges.size();

			List<Edge> list1 = new ArrayList<Edge>(edges.subList(0, size / 2));
			adjacencies1.put(v, list1);

			List<Edge> list2 = new ArrayList<Edge>(
					edges.subList(size / 2, size));
			adjacencies2.put(v, list2);
		}

		split_adjacencies.add(Collections.unmodifiableMap(adjacencies1));
		split_adjacencies.add(Collections.unmodifiableMap(adjacencies2));

		return Collections.unmodifiableList(split_adjacencies);
	}
}
