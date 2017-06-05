package pl.cpoo.utils;
import org.opencv.core.Point;

public class Line 
{
    public Point offset;
    public double angle;

    public Line(Point offset, double angle) 
    {
        this.offset = offset.clone();
        this.angle = angle;
    }

    public Point get(int length) 
    {
        Point result = offset.clone();
        result.x += Math.cos(angle) * length;
        result.y += Math.sin(angle) * length;
        return result;
    }

    public Point getStart() 
    {
        return get(-5000);
    }

    public Point getEnd() 
    {
        return get(5000);
    }

    public void scale(double factor) 
    {
        offset.x *= factor;
        offset.y *= factor;
    }
    
    public static Point intersect(Line l1, Line l2) 
    {
        return getLineLineIntersection(l1.getStart().x, l1.getStart().y, l1.getEnd().x, l1.getEnd().y,
                l2.getStart().x, l2.getStart().y, l2.getEnd().x, l2.getEnd().y
                );
    }
    
    public static Point getLineLineIntersection(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) 
    {
        double det1And2 = det(x1, y1, x2, y2);
        double det3And4 = det(x3, y3, x4, y4);
        double x1LessX2 = x1 - x2;
        double y1LessY2 = y1 - y2;
        double x3LessX4 = x3 - x4;
        double y3LessY4 = y3 - y4;
        double det1Less2And3Less4 = det(x1LessX2, y1LessY2, x3LessX4, y3LessY4);
        if (det1Less2And3Less4 == 0){
           // the denominator is zero so the lines are parallel and there's either no solution (or multiple solutions if the lines overlap) so return null.
           return null;
        }
        double x = (det(det1And2, x1LessX2,
              det3And4, x3LessX4) /
              det1Less2And3Less4);
        double y = (det(det1And2, y1LessY2,
              det3And4, y3LessY4) /
              det1Less2And3Less4);
        return new Point(x, y);
     }
     protected static double det(double a, double b, double c, double d) 
     {
        return a * d - b * c;
     }
 }
	
