/*
 * BASED ON Dijkstra Java code from
 * 
 * http://www.vogella.com/articles/JavaAlgorithmsDijkstra/article.html
 * 
 * Version 1.1 - Copyright 2009, 2010, 2011, 2011 Lars Vogel
 * 
 * MODIFIED BY Gregory Norton to address performance concerns and eliminate
 * requirement to store nodes external to the object. Further modifications
 * added the parallelization.
 * 
 * Eclipse Public License
 * 
 * HT for Fork/Join HowTo: http://www.javacodegeeks.com/2011/02/
 * 							 java-forkjoin-parallel-programming.html
 * 
 * HT for CyclicBarrier HowTo: http://www.javamex.com/tutorials/
 * 								threads/CyclicBarrier_parallel_sort_2.shtml
 * 
 * HT for BlockingQueue HowTo: http://www.javamex.com/tutorials/
 * 								synchronization_producer_consumer_2.shtml
 */

package dijkstra.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

import dijkstra.model.Edge;
import dijkstra.model.Graph;
import dijkstra.model.Vertex;

public class ParallelDijkstraAlgorithm {
	
	private static final int MAX_PROCESSING_SPLIT_COUNT = 2;
	
	private Set<Vertex> settled_nodes;
	private Map<Vertex, Vertex> predecessors;
	
	private class UnsettledNode implements Comparable<UnsettledNode> {

		int distance;
		Vertex node;
		Vertex predecessor;

		public UnsettledNode(final int distance, final Vertex node,
				final Vertex predecessor) {
			this.distance = distance;
			this.node = node;
			this.predecessor = predecessor;
		}

		@Override
		public int compareTo(final UnsettledNode other) {
			return (distance - other.distance);
		}
		
		public boolean isFartherAwayThan(final UnsettledNode other) {
			return (this.compareTo(other) > 0);
		}
	}
	
	/* -------------------------------------------------------------------- */
	
	private List<UnsettledNode> global_new_unsettled_nodes;
	
	private UnsettledNode winner;
	
	private List<ProcessingTask> processing_tasks;
	
	private CyclicBarrier processing_task_barrier;
	private CyclicBarrier reexecute_task_barrier;
	private CyclicBarrier leaves_done_barrier;
	
	private BlockingQueue<Integer> notify_queue;

	private int leaf_processing_task_count;
	
	private class ProcessingTask extends RecursiveAction {
		
		ProcessingTask pt1;
		ProcessingTask pt2;
		
		Map<Vertex, List<Edge>> adjacencies;
		
		Map<Vertex, Integer> distances_from_source;
		Queue<UnsettledNode> unsettled_nodes_queue;
		List<UnsettledNode> new_unsettled_nodes;
		
		boolean leaf_task = true;
		
		public ProcessingTask(final Map<Vertex, List<Edge>> adjacencies) {
			this(adjacencies, 0);
		}
		
		public ProcessingTask(final Map<Vertex, List<Edge>> adjacencies,
				final int level) {

			if(level < MAX_PROCESSING_SPLIT_COUNT) {
								
				List<Map<Vertex, List<Edge>>> split_adjacencies =
						Graph.splitAdjacencies(adjacencies);
				
				pt1 = new ProcessingTask(split_adjacencies.get(0), level + 1);
				pt2 = new ProcessingTask(split_adjacencies.get(1), level + 1);

				leaf_task = false;
				
			} else {
				this.adjacencies = adjacencies;				
			}
			
			distances_from_source = new HashMap<Vertex, Integer>();
			unsettled_nodes_queue = new PriorityQueue<UnsettledNode>();
			new_unsettled_nodes = new ArrayList<UnsettledNode>();
			
			if (leaf_task) {
				leaf_processing_task_count += 1;
				
				processing_task_barrier = 
						new CyclicBarrier(leaf_processing_task_count,
								query_tasks_for_winner);
				
				leaves_done_barrier =
						new CyclicBarrier(leaf_processing_task_count,
								signal_leaves_done);
				
				reexecute_task_barrier = 
						new CyclicBarrier(leaf_processing_task_count + 1);
			}
			
			processing_tasks.add(this);
		}
		
		/* ---------------------------------------------------------------- */

		public void setSource(final Vertex source) {
			
			/* This method is called *once* in the root processing node. The
			 * routine sets the first winner (the source) and adds the
			 * node to the settled list.
			 */
			
			UnsettledNode us_node = new UnsettledNode(0, source, null);
			winner = us_node;
			
			settled_nodes.add(source);
		}
		
		private void findWinner() {
			
			/* The CyclicBarrier leaf_barrier waits for all "leaf" tasks to
			 * reach this point before continuing. Once all tasks reach the
			 * barrier, the query_tasks_for_winner Runnable executes once.
			 */
			
			try {
				processing_task_barrier.await();
			} catch (InterruptedException ex) {
				return;
			} catch (BrokenBarrierException ex) {
				return;
			}

			processWinnerAndUnsettledNodes();
		}
		
		private Runnable query_tasks_for_winner = new Runnable() {
			
			/* Called once per round as part of the functionality of 
			 * processing_task_barrier. */
			
			public void run() {
				queryTasksForWinner();
			}
		};
		
		private void queryTasksForWinner() {
			UnsettledNode potential_winner = null;
			
			global_new_unsettled_nodes.clear();
			
			for (ProcessingTask pt : processing_tasks) {				
				UnsettledNode us_node = pt.unsettled_nodes_queue.peek();
								
				if (null == us_node)
					continue;

				if ((null == potential_winner)
						|| potential_winner.isFartherAwayThan(us_node))
					potential_winner = us_node;
								
				global_new_unsettled_nodes.addAll(pt.new_unsettled_nodes);
			}

			winner = potential_winner;
			
			if (null != winner) {
				
//				System.out.println("Winner:" + winner.node.getId() + ","
//						+ winner.distance + "," + System.currentTimeMillis());
				
				settled_nodes.add(winner.node);
				predecessors.put(winner.node, winner.predecessor);
			}
		}
		
		private void processWinnerAndUnsettledNodes() {
			if ((null != winner) && (0 == winner.distance)) 
				reset(winner.node);
			else if (winner == unsettled_nodes_queue.peek())
				unsettled_nodes_queue.poll();
			
			for(UnsettledNode us_node : global_new_unsettled_nodes)
				distances_from_source.put(us_node.node, us_node.distance);
		}
		
		private void reset(final Vertex node) {
			unsettled_nodes_queue.clear();
			distances_from_source.clear();
			distances_from_source.put(node, 0);
		}
		
		/* ---------------------------------------------------------------- */
		
		private boolean is_active;
		
		@Override
		public void compute() {
			is_active = true;
	
			/* The method is considered to be in the reexecuting state if 
			 * called by a thread outside of the fork/join pool. What we
			 * want to take place in that situation is to fall through to
			 * the cyclic barrier below and trigger the leaf nodes to
			 * run the algorithm.
			 */
			
			boolean reexecuting = !inForkJoinPool();
									
			while (true) {
				
				/* Skip this section if we are reentering the compute() to
				 * trigger another execution.
				 */
				
				if(false == reexecuting) {
					if (leaf_task) {
						
						/* Leaf task */

						processWinnerAndUnsettledNodes();

						while (null != winner) {
							relax(winner.node, winner.distance);
							findWinner();
						}

					} else {

						/* Branch task */
						
						pt1.fork();
						pt2.compute();
												
						break; /* Shouldn't see this, but just in case ... */
					}
				}

				/*
				 * Wait for all of the leaves to reach this common point. This
				 * will signal the completion of processing.
				 */
				
				try {
					if(false == reexecuting)
						leaves_done_barrier.await();
				} catch (InterruptedException ex) {
					return;
				} catch (BrokenBarrierException ex) {
					return;
				}
				
				/*
				 * Wait for reexecution triggered by calling compute() from a
				 * thread outside the fork/join pool.
				 */
				
				try {
					reexecute_task_barrier.await();
				} catch (InterruptedException ex) {
					return;
				} catch (BrokenBarrierException ex) {
					return;
				}
								
				/* Reexecuting trigger? Return */
				
				if (reexecuting) break;
			}
		}
		
		private Runnable signal_leaves_done = new Runnable() {
			
			/* Called once per round as part of the functionality of 
			 * leaves_done_barrier. */
			
			public void run() {
				try {
					notify_queue.put(1);
				} catch (Exception e) {
					return;
				}
			}
		};

		private void relax(final Vertex node, int dist_to_node) {
			new_unsettled_nodes.clear();
			
			for (Vertex target : getNeighbors(node)) {
				int dist = dist_to_node + getDistance(node, target);
				if (getShortestDistance(target) > dist) {
					
//					System.out.println(getShortestDistance(target) + ">" + dist
//							+ " => " + target + "->" + node);
					
					distances_from_source.put(target, dist);
					new_unsettled_nodes.add(new UnsettledNode(dist, target,	node));
				}
			}
			
			unsettled_nodes_queue.addAll(new_unsettled_nodes);
		}
		
		private int getShortestDistance(final Vertex destination) {
			Integer d = distances_from_source.get(destination);
			return (d == null) ? Integer.MAX_VALUE : d;
		}

		private int getDistance(final Vertex node, final Vertex target) {
			for (Edge edge : adjacencies.get(node))
				if (edge.getDestination().equals(target))
					return edge.getWeight();

			throw new RuntimeException("Should not happen");
		}

		private List<Vertex> getNeighbors(final Vertex node) {
			List<Vertex> node_neighbors = new ArrayList<Vertex>();
			for (Edge edge : adjacencies.get(node)) {
				Vertex destination = edge.getDestination();
				if (!settled_nodes.contains(destination))
					node_neighbors.add(destination);
			}

			return Collections.unmodifiableList(node_neighbors);
		}
	}
	
	/* -------------------------------------------------------------------- */
	
	private static ForkJoinPool fork_join_pool = 
			dijkstra.resources.Concurrency.getForkJoinPool();
	
	private final List<Vertex> nodes;
	
	private ProcessingTask root_processing_task;

	public ParallelDijkstraAlgorithm(final Graph graph) {
		nodes = graph.getVertexes();
		
		settled_nodes = new HashSet<Vertex>();
		predecessors = new HashMap<Vertex, Vertex>();
		global_new_unsettled_nodes = new ArrayList<UnsettledNode>();
		
		processing_tasks = new ArrayList<ProcessingTask>();
		root_processing_task = new ProcessingTask(graph.getAdjacencies());
		
		notify_queue = new LinkedBlockingQueue<Integer>();
	}

	public void execute(final Vertex source) {
	
		settled_nodes.clear();
		predecessors.clear();
				
		if (null != root_processing_task) {
			root_processing_task.setSource(source);
			
			if (false == root_processing_task.is_active) 
				fork_join_pool.execute(root_processing_task);
			else 
				root_processing_task.compute();
			
			try {
				notify_queue.take();
			} catch (Exception e) {
				return;
			}
		}
	}
	
	public void execute(final int node_num) {
		execute(nodes.get(node_num));
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
	
	public List<Vertex> getPath(final int node_num) {
		return getPath(nodes.get(node_num));
	}
	
	public void terminate() {
		winner = null;
		root_processing_task = null;
		
		for(ProcessingTask pt : processing_tasks)
			pt.cancel(true);
	}
}
