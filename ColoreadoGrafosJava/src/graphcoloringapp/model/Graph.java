package graphcoloringapp.model;

import java.util.*;

// Grafo no dirigido con lista de adyacencia.
// Aplica SRP: solo gestiona datos y algoritmos, sin referencias a la vista.
public class Graph {

    private final Map<Integer, List<Integer>> adjacency = new LinkedHashMap<>();
    private final Map<Integer, Integer>       colorMap  = new HashMap<>();
    private int colorsUsed = 0;

    // ── Nodos ──────────────────────────────────────────────────────────

    public void addNode(int id) {
        adjacency.putIfAbsent(id, new ArrayList<>());
        colorMap.put(id, -1);
    }

    public boolean removeNode(int id) {
        if (!adjacency.containsKey(id)) return false;
        // Elimina el nodo de las listas de adyacencia de sus vecinos
        for (int nb : new ArrayList<>(adjacency.get(id)))
            adjacency.get(nb).remove(Integer.valueOf(id));
        adjacency.remove(id);
        colorMap.remove(id);
        return true;
    }

    // ── Aristas ────────────────────────────────────────────────────────

    public boolean addEdge(int u, int v) {
        if (!adjacency.containsKey(u) || !adjacency.containsKey(v)) return false;
        if (u == v) return false; // sin auto-lazos
        if (adjacency.get(u).contains(v)) return false; // arista ya existe
        adjacency.get(u).add(v);
        adjacency.get(v).add(u);
        return true;
    }

    public boolean removeEdge(int u, int v) {
        if (!adjacency.containsKey(u) || !adjacency.containsKey(v)) return false;
        boolean r = adjacency.get(u).remove(Integer.valueOf(v));
        adjacency.get(v).remove(Integer.valueOf(u));
        return r;
    }

    public boolean hasEdge(int u, int v) {
        return adjacency.containsKey(u) && adjacency.get(u).contains(v);
    }

    // ── Limpieza ───────────────────────────────────────────────────────

    public void clear() {
        adjacency.clear(); colorMap.clear(); colorsUsed = 0;
    }

    public void resetColors() {
        for (int k : colorMap.keySet()) colorMap.put(k, -1);
        colorsUsed = 0;
    }

    // ── Coloreado BFS ──────────────────────────────────────────────────
    // Recorre el grafo por niveles usando una cola.
    // A cada nodo le asigna el menor color no usado por sus vecinos (greedy).

    public List<ColoringStep> colorWithBFS() {
        resetColors();
        List<ColoringStep> steps = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();

        for (int start : adjacency.keySet()) {
            if (visited.contains(start)) continue;
            Queue<Integer> queue = new LinkedList<>();
            queue.add(start); visited.add(start);
            while (!queue.isEmpty()) {
                int node = queue.poll();
                int color = smallestFree(node);
                colorMap.put(node, color);
                if (color + 1 > colorsUsed) colorsUsed = color + 1;
                steps.add(new ColoringStep(node, color, "BFS",
                        "Visita(BFS) Nodo " + node + "  →  color " + color));
                for (int nb : adjacency.get(node))
                    if (!visited.contains(nb)) { visited.add(nb); queue.add(nb); }
            }
        }
        return steps;
    }

    // ── Coloreado DFS ──────────────────────────────────────────────────
    // Recorre el grafo en profundidad de forma recursiva.
    // Misma lógica greedy que BFS, distinto orden de visita.

    public List<ColoringStep> colorWithDFS() {
        resetColors();
        List<ColoringStep> steps = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        for (int start : adjacency.keySet())
            if (!visited.contains(start)) dfsColor(start, visited, steps);
        return steps;
    }

    private void dfsColor(int node, Set<Integer> visited, List<ColoringStep> steps) {
        visited.add(node);
        int color = smallestFree(node);
        colorMap.put(node, color);
        if (color + 1 > colorsUsed) colorsUsed = color + 1;
        steps.add(new ColoringStep(node, color, "DFS",
                "Visita(DFS) Nodo " + node + "  →  color " + color));
        for (int nb : adjacency.get(node))
            if (!visited.contains(nb)) dfsColor(nb, visited, steps);
    }

    // Devuelve el menor entero no usado por los vecinos ya coloreados
    private int smallestFree(int node) {
        Set<Integer> used = new HashSet<>();
        for (int nb : adjacency.get(node))
            if (colorMap.getOrDefault(nb, -1) != -1) used.add(colorMap.get(nb));
        int c = 0;
        while (used.contains(c)) c++;
        return c;
    }

    // ── Grafo de ejemplo ───────────────────────────────────────────────

    public void loadExample() {
        clear();
        for (int i = 0; i <= 7; i++) addNode(i);
        int[][] edges = {{0,1},{0,2},{1,3},{2,3},{3,4},{4,5},{4,6},{5,7},{6,7},{0,7},{1,6},{2,5}};
        for (int[] e : edges) addEdge(e[0], e[1]);
    }

    // ── Getters ────────────────────────────────────────────────────────

    public Set<Integer>  getNodes()          { return adjacency.keySet(); }
    public List<Integer> getNeighbors(int n) { return adjacency.getOrDefault(n, Collections.emptyList()); }
    public int           getColor(int n)     { return colorMap.getOrDefault(n, -1); }
    public int           getColorsUsed()     { return colorsUsed; }
    public boolean       hasNode(int id)     { return adjacency.containsKey(id); }
    public int           nodeCount()         { return adjacency.size(); }
    public int           edgeCount()         { int s=0; for(List<Integer> l:adjacency.values()) s+=l.size(); return s/2; }
}