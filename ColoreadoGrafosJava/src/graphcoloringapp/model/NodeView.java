package graphcoloringapp.model;

// Representación visual de un nodo: posición en el canvas y estado de renderizado.
public class NodeView {

    public static final double RADIUS = 24;

    private final int id;
    private double  x;
    private double  y;
    private int     colorIndex  = -1;   // -1 = sin color asignado
    private boolean highlighted = false;

    public NodeView(int id, double x, double y) {
        this.id = id;
        this.x  = x;
        this.y  = y;
    }

    public int     getId()                    { return id; }
    public double  getX()                     { return x; }
    public double  getY()                     { return y; }
    public void    setX(double x)             { this.x = x; }
    public void    setY(double y)             { this.y = y; }
    public int     getColorIndex()            { return colorIndex; }
    public void    setColorIndex(int c)       { colorIndex = c; }
    public boolean isHighlighted()            { return highlighted; }
    public void    setHighlighted(boolean h)  { highlighted = h; }

    // Verifica si el punto (px, py) cae dentro del círculo del nodo
    public boolean contains(double px, double py) {
        double dx = px - x;
        double dy = py - y;
        return Math.sqrt(dx*dx + dy*dy) <= RADIUS;
    }
}