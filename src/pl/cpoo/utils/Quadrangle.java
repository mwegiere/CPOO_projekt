package pl.cpoo.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opencv.core.*;

import org.opencv.imgproc.Imgproc;

public class Quadrangle {

	    static int
	        TOP = 0,
	        RIGHT = 1,
	        BOTTOM = 2,
	        LEFT = 3;

	    public Line[] lines = new Line[4];

	    public Quadrangle() {

	    }

	    private static double getAngle(Point p1, Point p2) {
	        return Math.atan2(p2.y - p1.y, p2.x - p1.x);
	    }

	    private static double getLength(Point p1, Point p2) {
	        return Math.sqrt(Math.pow(p2.x - p1.x, 2) + Math.pow(p2.y - p1.y, 2));
	    }

	    private static double roundAngle(double angle) {
	        return angle - (2*Math.PI) * Math.round(angle / (2 * Math.PI));
	    }

	    public static Quadrangle fromContour(MatOfPoint contour) {
	        List<Point> points = contour.toList();
	        List<LineSegment> segments = new ArrayList<LineSegment>(); 

	        // Create line segments
	        int counter = 0;
	        for (int i = 0; i < points.size(); i++) 
	        {
	            double angle = getAngle(points.get(i), points.get((i + counter) % points.size())); 
	            double length = getLength(points.get(i), points.get((i + counter) % points.size())); 
	            // LINE SEGMENT MUST TO BE MORE THAN 100 POINTS
	            if(length>=100)
	            {
	            	segments.add(new LineSegment(points.get(i), angle, length));
	            	i = i + counter;
	            	counter = 0;
	            }else{
	            	counter++;
	            }
	            
	        }

	     // Connect line segments
	        double angleDiffMax = 2 * Math.PI / 100;
	        List<LineSegment> output = new ArrayList<LineSegment>();
	        for (LineSegment segment : segments) {
	            if (output.isEmpty()) {
	                output.add(segment);
	            } else {
	                LineSegment top = output.get(output.size() - 1);
	                double d = roundAngle(segment.angle - top.angle);
	                if (Math.abs(d) < angleDiffMax) {
	                    top.melt(segment);
	                } else {
	                    output.add(segment);
	                }
	            }
	        }

	        Collections.sort(output);
	        Quadrangle quad = new Quadrangle();

	        for (int o = 0; o < 4; o += 1) {
	            for (int i = 0; i < 4; i++) {
	                if (Math.abs(roundAngle(output.get(i).angle - (2 * Math.PI * o / 4))) < Math.PI / 4) {
	                    quad.lines[o] = output.get(i);
	                }
	            }
	        }


	        return quad;
	    }
	    
	    public void scale(double factor) {
	        for (int i = 0; i < 4; i++) {
	            lines[i].scale(factor);
	        }
	    }

	    public Mat warp(Mat src) {

	        if(lines[TOP]==null)
	        {
	        	lines[TOP] = new Line(new Point(5,src.rows()-5), 0.0) ;
	        }
	        
	        if(lines[RIGHT]==null)
	        {
	        	lines[RIGHT] = new Line(new Point(5,5), 1.5) ;
	        }
	        
	        if(lines[BOTTOM]==null)
	        {
	        	lines[BOTTOM] = new Line(new Point(src.cols()-5,5), -3.1) ;
	        }
	        
	        if(lines[LEFT]==null)
	        {
	        	lines[LEFT] = new Line(new Point(src.cols()-5, src.rows()-5), -1.5) ;
	        }
        	System.out.printf("TOP: " +lines[TOP].angle + lines[TOP].offset.toString()+ "\n");
        	Imgproc.circle(src, lines[TOP].offset, 30, new Scalar(200, 100, 100), 8);
        	Imgproc.line(src, lines[TOP].get(-5000), lines[TOP].get(5000), new Scalar(200, 100, 100), 8);
	        
	        System.out.printf("RIGHT: " +lines[RIGHT].angle + lines[RIGHT].offset.toString()+ "\n");
	        Imgproc.circle(src, lines[RIGHT].offset, 30, new Scalar(0, 255, 0), 8);
	        Imgproc.line(src, lines[RIGHT].get(-5000), lines[RIGHT].get(5000), new Scalar(0, 255, 0), 8);
	        
	        System.out.printf("BOTTOM: " +lines[BOTTOM].angle + lines[BOTTOM].offset.toString()+ "\n");
	        Imgproc.circle(src, lines[BOTTOM].offset, 30, new Scalar(255, 0, 0), 8);
	        Imgproc.line(src, lines[BOTTOM].get(-5000), lines[BOTTOM].get(5000), new Scalar(255, 0, 0), 8);
	        
	        System.out.printf("LEFT: " +lines[LEFT].angle + lines[LEFT].offset.toString()+ "\n");
	        Imgproc.circle(src, lines[LEFT].offset, 30, new Scalar(0, 0, 255), 8);
        	Imgproc.line(src, lines[LEFT].get(-5000), lines[LEFT].get(5000), new Scalar(0, 0, 255), 8);

	        double width = src.cols();
	        double height = src.rows();

	        Point[] srcProjection = new Point[4], dstProjection = new Point[4];
	        srcProjection[0] = Line.intersect(lines[TOP], lines[LEFT]);
	        srcProjection[1] = Line.intersect(lines[TOP], lines[RIGHT]);
	        srcProjection[2] = Line.intersect(lines[BOTTOM], lines[LEFT]);
	        srcProjection[3] = Line.intersect(lines[BOTTOM], lines[RIGHT]);

	        dstProjection[0] = new Point(0, 0);
	        dstProjection[1] = new Point(width - 1, 0);
	        dstProjection[2] = new Point(0, height - 1);
	        dstProjection[3] = new Point(width - 1, height - 1); 


	        Mat warp = Imgproc.getPerspectiveTransform(new MatOfPoint2f(srcProjection), new MatOfPoint2f(dstProjection));
	        Mat rotated = new Mat();
	        Size size = new Size(width, height);
	        Imgproc.warpPerspective(src, rotated, warp, size, Imgproc.INTER_LINEAR);
	        Mat matrix = Imgproc.getRotationMatrix2D(new Point(rotated.cols()/2,rotated.rows()/2),180,1);
	        Imgproc.warpAffine(rotated,rotated,matrix, rotated.size());
	        return rotated;
	    }
	    
	    public void clear() 
	    {
	    	lines[0] = null;
	    	lines[1] = null;
	    	lines[2] = null;
	    	lines[3] = null;
	    }
	}
	    

