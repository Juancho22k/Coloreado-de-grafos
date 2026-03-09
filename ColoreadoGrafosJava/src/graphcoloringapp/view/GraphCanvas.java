package graphcoloringapp.view;

import graphcoloringapp.model.Graph;
import graphcoloringapp.model.NodeView;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.*;

// Canvas que renderiza el grafo. Solo dibuja — la lógica de interacción
// es responsabilidad exclusiva del controlador.
public class GraphCanvas extends Canvas {

    // Paleta de colores para los nodos coloreados
    public static final Color[] PALETTE = {
        Color.web("#e74c3c"), Color.web("#3498db"), Color.web("#2ecc71"),
        Color.web("#f39c12"), Color.web("#9b59b6"), Color.web("#1abc9c"),
        Color.web("#e67e22"), Color.web("#34495e"), Color.web("#c0392b"),
        Color.web("#16a085"),
    };

    private static final Color COLOR_UNCOLORED = Color.web("#dfe6e9");
    private static final Color COLOR_BORDER    = Color.web("#2c3e50");
    private static final Color COLOR_HIGHLIGHT = Color.web("#f1c40f");
    private static final Color COLOR_EDGE      = Color.web("#636e72");
    private static final Color COLOR_EDGE_SEL  = Color.web("#fdcb6e");
    private static final Color COLOR_BG        = Color.web("#1a1a2e");
    private static final Font  FONT_NODE       = Font.font("Arial", 13);

    private final Graph model;
    private final Map<Integer, NodeView> nodeViews = new LinkedHashMap<>();

    private NodeView dragging        = null;
    private int      highlightedNode = -1;
    private NodeView edgeSource      = null; // nodo origen en modo ADD_EDGE
    private double   mouseX, mouseY;
    private int      hoverEdgeU = -1, hoverEdgeV = -1; // arista bajo el cursor en modo DELETE

    public GraphCanvas(Graph model) {
        super(800, 580);
        this.model = model;
        setFocusTraversable(true);
    }

    // ── Gestión de vistas de nodos ─────────────────────────────────────

    public void addNodeView(int id, double x, double y) { nodeViews.put(id, new NodeView(id, x, y)); }
    public void removeNodeView(int id)                  { nodeViews.remove(id); }
    public void clearViews()                            { nodeViews.clear(); }

    // Devuelve el nodo que contiene el punto (px, py), o null si ninguno
    public NodeView getNodeAt(double px, double py) {
        for (NodeView nv : nodeViews.values()) if (nv.contains(px, py)) return nv;
        return null;
    }

    // Devuelve {u, v} de la arista más cercana al punto, o null si está fuera del umbral
    public int[] getEdgeAt(double px, double py) {
        double threshold = 8.0;
        for (int u : model.getNodes()) {
            NodeView uv = nodeViews.get(u);
            if (uv == null) continue;
            for (int v : model.getNeighbors(u)) {
                if (v <= u) continue;
                NodeView vv = nodeViews.get(v);
                if (vv == null) continue;
                double dist = pointToSegmentDist(px, py, uv.getX(), uv.getY(), vv.getX(), vv.getY());
                if (dist < threshold) return new int[]{u, v};
            }
        }
        return null;
    }

    // Distancia de un punto a un segmento (para detectar clic sobre arista)
    private double pointToSegmentDist(double px, double py,
                                      double ax, double ay,
                                      double bx, double by) {
        double dx = bx - ax, dy = by - ay;
        double lenSq = dx*dx + dy*dy;
        if (lenSq == 0) return Math.hypot(px-ax, py-ay);
        double t = Math.max(0, Math.min(1, ((px-ax)*dx + (py-ay)*dy) / lenSq));
        return Math.hypot(px - (ax + t*dx), py - (ay + t*dy));
    }

    // ── Estado de interacción ──────────────────────────────────────────

    public NodeView getDragging()               { return dragging; }
    public void     setDragging(NodeView nv)    { dragging = nv; }
    public void     setHighlighted(int id)      { highlightedNode = id; }
    public void     setEdgeSource(NodeView nv)  { edgeSource = nv; }
    public NodeView getEdgeSource()             { return edgeSource; }
    public void     setMousePos(double x, double y) { mouseX = x; mouseY = y; }
    public void     setHoverEdge(int u, int v)  { hoverEdgeU = u; hoverEdgeV = v; }

    // ── Sincronización de colores ──────────────────────────────────────

    // Actualiza todos los NodeView con los colores actuales del modelo
    public void syncColors() {
        for (Map.Entry<Integer, NodeView> e : nodeViews.entrySet())
            e.getValue().setColorIndex(model.getColor(e.getKey()));
    }

    // Aplica el color de un nodo específico y lo resalta (usado en animación)
    public void applyColor(int nodeId) {
        NodeView nv = nodeViews.get(nodeId);
        if (nv != null) { nv.setColorIndex(model.getColor(nodeId)); nv.setHighlighted(true); }
    }

    public void clearHighlights() {
        for (NodeView nv : nodeViews.values()) nv.setHighlighted(false);
        highlightedNode = -1;
    }

    // ── Disposición circular ───────────────────────────────────────────

    // Distribuye los nodos en círculo centrado en el canvas
    public void arrangeCircular() {
        List<Integer> ids = new ArrayList<>(model.getNodes());
        int n = ids.size(); if (n == 0) return;
        double cx = getWidth()/2, cy = getHeight()/2;
        double r  = Math.min(cx, cy) * 0.70;
        for (int i = 0; i < n; i++) {
            double angle = 2*Math.PI*i/n - Math.PI/2;
            addNodeView(ids.get(i), cx + r*Math.cos(angle), cy + r*Math.sin(angle));
        }
    }

    // ── Dibujo principal ───────────────────────────────────────────────

    public void draw() {
        double w = getWidth(), h = getHeight();
        GraphicsContext gc = getGraphicsContext2D();

        gc.setFill(COLOR_BG);
        gc.fillRect(0, 0, w, h);

        // Dibuja aristas
        for (int u : model.getNodes()) {
            NodeView uv = nodeViews.get(u); if (uv == null) continue;
            for (int v : model.getNeighbors(u)) {
                if (v <= u) continue;
                NodeView vv = nodeViews.get(v); if (vv == null) continue;
                boolean isHover = (u==hoverEdgeU && v==hoverEdgeV) || (u==hoverEdgeV && v==hoverEdgeU);
                gc.setStroke(isHover ? COLOR_EDGE_SEL : COLOR_EDGE);
                gc.setLineWidth(isHover ? 3.5 : 1.8);
                gc.strokeLine(uv.getX(), uv.getY(), vv.getX(), vv.getY());
            }
        }

        // Línea punteada desde el nodo origen hasta el cursor (modo ADD_EDGE)
        if (edgeSource != null) {
            gc.setStroke(Color.web("#fdcb6e", 0.7));
            gc.setLineWidth(2);
            gc.setLineDashes(8, 4);
            gc.strokeLine(edgeSource.getX(), edgeSource.getY(), mouseX, mouseY);
            gc.setLineDashes(0);
        }

        // Dibuja nodos
        for (NodeView nv : nodeViews.values()) {
            double cx = nv.getX(), cy = nv.getY(), r = NodeView.RADIUS;
            Color fill = nv.getColorIndex() < 0 ? COLOR_UNCOLORED : PALETTE[nv.getColorIndex() % PALETTE.length];
            // Sombra
            gc.setFill(Color.color(0,0,0,0.28)); gc.fillOval(cx-r+3, cy-r+4, r*2, r*2);
            gc.setFill(fill); gc.fillOval(cx-r, cy-r, r*2, r*2);
            // Borde: dorado si está resaltado, oscuro si no
            if (nv.isHighlighted() || nv.getId() == highlightedNode) {
                gc.setStroke(COLOR_HIGHLIGHT); gc.setLineWidth(3.5);
            } else {
                gc.setStroke(COLOR_BORDER); gc.setLineWidth(1.8);
            }
            gc.strokeOval(cx-r, cy-r, r*2, r*2);
            // Etiqueta con el ID del nodo
            gc.setFill(nv.getColorIndex() < 0 ? Color.web("#2c3e50") : Color.WHITE);
            gc.setFont(FONT_NODE); gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText(String.valueOf(nv.getId()), cx, cy+5);
        }
    }

    public Map<Integer, NodeView> getNodeViews() { return nodeViews; }
}