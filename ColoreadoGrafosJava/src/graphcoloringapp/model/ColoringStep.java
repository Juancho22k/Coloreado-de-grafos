package graphcoloringapp.model;

// Representa un paso individual del algoritmo de coloreado.
// Actúa como DTO: transfiere el estado de cada paso del modelo al controlador
// sin acoplar la lógica de animación con la lógica del grafo.
public class ColoringStep {

    private final int    node;
    private final int    color;
    private final String algorithm;
    private final String description;

    public ColoringStep(int node, int color, String algorithm, String description) {
        this.node        = node;
        this.color       = color;
        this.algorithm   = algorithm;
        this.description = description;
    }

    public int    getNode()        { return node; }
    public int    getColor()       { return color; }
    public String getAlgorithm()   { return algorithm; }
    public String getDescription() { return description; }

    @Override
    public String toString() {
        return "[" + algorithm + "] " + description;
    }
}