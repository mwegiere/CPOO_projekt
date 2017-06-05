package pl.cpoo.utils;

import org.opencv.core.Point;

public class LineSegment extends Line implements Comparable<LineSegment>{

	public double length;

    public LineSegment(Point offset, double angle, double length) {
        super(offset, angle);
        this.length = length;
    }

    public void melt(LineSegment segment) {
        Point point = new Point();
        point.x += Math.cos(angle) * length;
        point.y += Math.sin(angle) * length;
        point.x += Math.cos(segment.angle) * segment.length;
        point.y += Math.sin(segment.angle) * segment.length;

        angle = Math.atan2(point.y, point.x);
        offset.x = (offset.x * length + segment.offset.x * segment.length) / (length + segment.length);
        offset.y = (offset.y * length + segment.offset.y * segment.length) / (length + segment.length);

        length += segment.length;
    }

   
    
    @Override
    public int compareTo(LineSegment other) throws ClassCastException {
        if (!(other instanceof LineSegment)) {
            throw new ClassCastException("A LineSegment object expected.");
        }
        return (int) (((LineSegment) other).length - this.length);    
    }

}
