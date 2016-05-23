/*
 * Dijkstra Java code from
 * 
 * http://www.vogella.com/articles/JavaAlgorithmsDijkstra/article.html
 * 
 * Version 1.1 - Copyright 2009, 2010, 2011, 2011 Lars Vogel
 * 
 * Eclipse Public License
 */

package dijkstra.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Vertex {
	
	final private String id;

	public Vertex(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}
	
	static final private Pattern int_pattern = Pattern.compile("\\d+");
	
	public int getIdInteger() {
		int id_int = -1;
		
		Matcher matcher = int_pattern.matcher(id);
		if(matcher.find())
			id_int = Integer.valueOf(matcher.group());
			
		return id_int;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Vertex other = (Vertex) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return id;
	}
}
