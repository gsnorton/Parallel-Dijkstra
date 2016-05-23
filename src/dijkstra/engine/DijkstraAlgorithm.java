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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import dijkstra.model.Edge;
import dijkstra.model.Graph;
import dijkstra.model.Vertex;

public class DijkstraAlgorithm {
	
	private class UnsettledNode implements Comparable<UnsettledNode> {

		private final int distance;
		private final Vertex node;
		
		public UnsettledNode(final int distance, final Vertex node) {
			this.distance = distance; this.node = node;
		}

		@Override
		public int compareTo(final UnsettledNode other) {
			return (distance - other.distance);
		}
	}
	
	private final List<Vertex> nodes;
	private final Map<Vertex, List<Edge>> adjacencies;
	
	private Map<Vertex, Vertex> predecessors;

	private Queue<UnsettledNode> unsettled_nodes_queue;
	private Map<Vertex, Integer> distances_from_source;
	
	private Set<Vertex> settledNodes = new HashSet<Vertex>();
	
	public DijkstraAlgorithm(final Graph graph) {
		nodes = new ArrayList<Vertex>(graph.getVertexes());
		adjacencies = new HashMap<Vertex, List<Edge>>(graph.getAdjacencies());	
	}
	
	public DijkstraAlgorithm(final DijkstraAlgorithm other) {
		nodes = new ArrayList<Vertex>(other.nodes);
		adjacencies = new HashMap<Vertex, List<Edge>>(other.adjacencies);
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
		predecessors = new HashMap<Vertex, Vertex>();
		
		unsettled_nodes_queue = new PriorityQueue<UnsettledNode>();
		distances_from_source = new HashMap<Vertex, Integer>();

		unsettled_nodes_queue.add(new UnsettledNode(0, source));
		distances_from_source.put(source, 0);
		
		settledNodes.clear();
		
		while(false == unsettled_nodes_queue.isEmpty()) {
			UnsettledNode us_node = unsettled_nodes_queue.poll();
			Vertex node = us_node.node;
			settledNodes.add(node);
			findMinimalDistances(node);
		}
	}
	
	public void execute(final int node_num) {
		this.execute(nodes.get(node_num));
	}

	private void findMinimalDistances(final Vertex node) {
		List<Vertex> adjacentNodes = getNeighbors(node);
		for (Vertex target : adjacentNodes) {
			int dist = getShortestDistance(node) + getDistance(node, target);
			if (getShortestDistance(target) > dist) {
				distances_from_source.put(target, dist);
				predecessors.put(target, node);
				unsettled_nodes_queue.add(new UnsettledNode(dist, target));
			}
		}
	}

	private int getDistance(final Vertex node, final Vertex target) {
		for(Edge edge : adjacencies.get(node))
			if(edge.getDestination().equals(target))
				return edge.getWeight();
		
		throw new RuntimeException("Should not happen");
	}

	private List<Vertex> getNeighbors(final Vertex node) {
		List<Vertex> node_neighbors = new ArrayList<Vertex>();
		
		for(Edge edge : adjacencies.get(node))
			if(!settledNodes.contains(edge.getDestination()))
				node_neighbors.add(edge.getDestination());
		
		return node_neighbors;
	}

	private int getShortestDistance(final Vertex destination) {
		Integer d = distances_from_source.get(destination);
		if (d == null) {
			return Integer.MAX_VALUE;
		} else {
			return d;
		}
	}

	/*
	 * These methods return the path from the source to the selected target and
	 * NULL if no path exists
	 */
	
	public List<Vertex> getPath(final Vertex target) {
		LinkedList<Vertex> path = new LinkedList<Vertex>();
		Vertex step = target;
		// check if a path exists
		if (predecessors.get(step) == null) {
			return null;
		}
		path.add(step);
		while (predecessors.get(step) != null) {
			step = predecessors.get(step);
			path.add(step);
		}
		return path;
	}
	
	public List<Vertex> getPath(final int node_num)
	{
		return getPath(nodes.get(node_num));
	}
}

