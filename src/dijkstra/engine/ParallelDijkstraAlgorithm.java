/*
 * BASED ON Dijkstra Java code from
 * 
 * http://www.vogella.com/articles/JavaAlgorithmsDijkstra/article.html
 * 
 * Version 1.1 - Copyright � 2009, 2010, 2011, 2011 Lars Vogel
 * 
 * MODIFIED BY Gregory Norton to address performance concerns and eliminate
 * requirement to store nodes external to the object.
 * 
 * Eclipse Public License
 */

package dijkstra.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

import dijkstra.model.Edge;
import dijkstra.model.Graph;
import dijkstra.model.Vertex;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class ParallelDijkstraAlgorithm {
	
	private static ForkJoinPool fork_join_pool = 
			dijkstra.resources.Concurrency.getForkJoinPool();
	
	private class UnsettledNode extends RecursiveAction 
	implements Comparable<UnsettledNode> {

		private final int distance;
		private final Vertex node;
		private final Vertex predecessor;
		
		public UnsettledNode(final int distance, final Vertex node,
					         final Vertex predecessor) {
			this.distance = distance; 
			this.node = node;
			this.predecessor = predecessor;
		}
		
		public UnsettledNode(final int distance, final Vertex node) {
			this(distance, node, null);
		}

		@Override
		public int compareTo(final UnsettledNode other) {
			return (distance - other.distance);
		}
		
		@Override
		public void compute() {
			
		}
		
		public void findMinimalDistances() {
			for (Vertex target : getNeighbors(node)) {
				int dist_target = distance + getDistance(node, target);
				if (getShortestDistance(target) > dist_target) {
					setShortestDistance(target, dist_target);
					UnsettledNode us_node = 
							new UnsettledNode(dist_target, target, node);
					unsettled_nodes_queue.add(us_node);
				}
			}
		}
		
		private int getDistance(final Vertex node, final Vertex target) {
			for(Edge edge : adjacencies.get(node))
				if(edge.getDestination().equals(target))
					return edge.getWeight();
			
			throw new RuntimeException("Should not happen");
		}
		
		private int getShortestDistance(final Vertex destination) {
			Integer d = distances_from_source.get(destination);
			return (d == null) ? Integer.MAX_VALUE : d;
		}
		
		private void setShortestDistance(final Vertex destination, 
				                         final int distance) {
			distances_from_source.put(destination, distance);
		}
	}
	
	private List<Vertex> nodes;
	private Map<Vertex, List<Edge>> adjacencies;
	
	private Queue<UnsettledNode> unsettled_nodes_queue =
		new PriorityQueue<UnsettledNode>();
	
	private Map<Vertex, Integer> distances_from_source;
	private Map<Vertex, Vertex> predecessors;
	
	public ParallelDijkstraAlgorithm(final Graph graph) {
		nodes = new ArrayList<Vertex>(graph.getVertexes());
		adjacencies = new HashMap<Vertex, List<Edge>>(graph.getAdjacencies());	
	}
	
	public ParallelDijkstraAlgorithm(final ParallelDijkstraAlgorithm other) {
		nodes = new ArrayList<Vertex>(other.nodes);
		adjacencies = new HashMap<Vertex, List<Edge>>(other.adjacencies);
	}

	private List<Vertex> getNeighbors(final Vertex node) {
		LinkedList<Vertex> node_neighbors = new LinkedList<Vertex>();
		
		for(Edge edge : adjacencies.get(node))
			node_neighbors.add(edge.getDestination());
				
		return node_neighbors;
	}
	
	public void removeNode(final int node_num) {
		// System.out.println("REMOVING " + node_num);
		Vertex node = nodes.get(node_num);
		for(Vertex neighbor : getNeighbors(node)) {
			List<Edge> edges = adjacencies.get(neighbor);
			List<Edge> new_edges = new ArrayList<Edge>(edges.size());
			for(Edge edge : edges) 
				if(edge.getDestination() != node)
					new_edges.add(edge);
			adjacencies.put(neighbor, new_edges);
		}
	}
	
	public void execute(final Vertex source) {
		predecessors = 
			new HashMap<Vertex, Vertex>(nodes.size());
		
		distances_from_source = 
			new HashMap<Vertex, Integer>(nodes.size());
		distances_from_source.put(source, 0);
		
		unsettled_nodes_queue.add(new UnsettledNode(0, source));
				
		while(false == unsettled_nodes_queue.isEmpty()) {
			UnsettledNode us_node = unsettled_nodes_queue.poll();
			predecessors.put(us_node.node, us_node.predecessor);
			us_node.findMinimalDistances();
		}
	}
	
	public void execute(final int node_num) {
		this.execute(nodes.get(node_num));
	}

	/*
	 * These methods return the path from the source to the selected target and
	 * NULL if no path exists
	 */
	
	public List<Vertex> getPath(final Vertex target) {
		LinkedList<Vertex> path = new LinkedList<Vertex>();
		Vertex step = target;
		// check if a path exists
		if (predecessors.get(step) == null)
			return null;
		path.add(step);
		while (predecessors.get(step) != null) {
			step = predecessors.get(step);
			path.add(step);
		}
		return Collections.unmodifiableList(path);
	}
	
	public List<Vertex> getPath(final int node_num)
	{
		return getPath(nodes.get(node_num));
	}
}
