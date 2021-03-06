package com.melsovereign.svgtopoints;


import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
 
import com.kitfox.svg.SVGCache;
import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGElement;

/*
 * Converts SVG data to a flat list of Pointts.
 * Pointt is a custom vector class that could
 * be substituted without too much work.
 * Uses the SVG Salamander library.
 * Point conversion technique from: http://blog.gemserk.com/2011/03/03/svg-path-traversal-in-java/
 */
public class SVGToPointt {
	
	PointtSet pointSet = new PointtSet();
	private URI fileUri;

	public SVGToPointt(URI _fileUri) {
		fileUri = _fileUri;
	}
	
	private List<Pointt> LoadPointsFromSVG(URI _fileUri) {  
		if (_fileUri == null) return null;
	    SVGDiagram diagram = SVGCache.getSVGUniverse().getDiagram(_fileUri);
	    return GetAllPoints(diagram);
	}  
	
	private ArrayList<Pointt> GetAllPoints(SVGDiagram diagram) {
		ArrayList<Pointt> points = new ArrayList<Pointt>();
		ArrayList<SVGElement> elems = new ArrayList<SVGElement>();
	    childrenRecursive(diagram.getRoot(), elems);
	    for (SVGElement el : elems) {
	    	com.kitfox.svg.Path svgPath = CastToPath(el);
	    	if (svgPath != null) {
	    		addPathPoints(svgPath, points);
	    	}
	    }	    
	    return points;
	}

	private void childrenRecursive( SVGElement node, ArrayList<SVGElement> result) {
		if (result == null) result = new ArrayList<SVGElement>();
		result.add(node);
		List<SVGElement> children = new ArrayList<SVGElement>();
	    node.getChildren(children);
	    for (int i=0; i < children.size(); ++i) {
			SVGElement childNode = (SVGElement) children.get(i);
			childrenRecursive(childNode, result);
		}
	}
	
	private void addPathPoints(com.kitfox.svg.Path pathSVG, ArrayList<Pointt> points) {
		 // get the AWT Shape  
	    Shape shape = pathSVG.getShape();      
	    // iterate over the shape using a path iterator discretizing with distance 0.001 units     
	    PathIterator pathIterator = shape.getPathIterator(null, 0.001d);  
	    float[] coords = new float[2];  
	    while (!pathIterator.isDone()) {  
		    pathIterator.currentSegment(coords);
		    Pointt p = new Pointt(coords[0], coords[1]);
		    points.add(p);  
		    pointSet.updateMin(p);
		    pointSet.updateMax(p);
		    pathIterator.next();  
	    }  
	}
	
	private com.kitfox.svg.Path CastToPath(SVGElement node) {
		com.kitfox.svg.Path result = null;
		try{
			result = (com.kitfox.svg.Path) node;
		} catch (ClassCastException cce) {
		}
		return result;
	}

	public List<Pointt> getPoints() {
		if (pointSet.points == null) {
			pointSet.points = LoadPointsFromSVG(fileUri);
		}
		return pointSet.points;
	}

	public Pointt getMinPoint() {
		return pointSet.getMinPoint();
	}

	public Pointt getMaxPoint() {
		return pointSet.getMaxPoint();
	}
	public PointtSet getPointSet() {
		if (pointSet == null) {
			pointSet.points = LoadPointsFromSVG(fileUri);
		}
		return pointSet;
	}
	
	/*
	 * Scalable point set.
	 */
	public class PointtSet
	{
		private List<Pointt> points = null;
		private Pointt minPoint = null;
		private Pointt maxPoint = null;
		
		public List<Pointt> getPoints() {
			if (points == null) {
				points = LoadPointsFromSVG(fileUri);
			}
			return points;
		}
		
		public void updateMin(Pointt p) {
			minPoint = Pointt.Min(p, minPoint);
		}
		public void updateMax(Pointt p) {
			maxPoint = Pointt.Max(p, maxPoint);
		}

		public Pointt getMinPoint() {
			return minPoint;
		}

		public Pointt getMaxPoint() {
			return maxPoint;
		}
		
		public ArrayList<Pointt> scaleToFitNewMinMax(Pointt newMin, Pointt newMax) {
			Pointt newDim = newMax.minus(newMin);
			Pointt oldDim = maxPoint.minus(minPoint);
			
			double proportion = newDim.y/oldDim.y;
			double proportionx = newDim.x /oldDim.x;
			proportion = (proportion > proportionx ) ? proportionx : proportion;
			
			for (int i=0; i < points.size(); ++i) {
				Pointt p = points.get(i);
				p = p.minus(minPoint);
				p = p.multiply(proportion);
				p = p.plus(newMin);
				points.set(i, p);
			}
			minPoint = newMin;
			maxPoint = newMax;
			
			return (ArrayList<Pointt>) points;
			
		}
	}
	

	

}
