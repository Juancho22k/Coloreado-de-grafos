package graphcoloringapp.model;

/**
 * Enumeración de los modos de interacción disponibles en el canvas.
 * Permite al usuario cambiar el comportamiento del clic/toque.
 *
 * Patrón: Strategy – el controlador delega el comportamiento del
 * evento al modo activo.
 */
public enum InteractionMode {
    ADD_NODE("Agregar Nodo", "Clic en canvas para agregar nodo"),
    ADD_EDGE("Agregar Arista", "Clic en nodo A, luego nodo B para conectar"),
    DELETE("Eliminar", "Clic en nodo o arista para eliminar"),
    MOVE("Mover", "Arrastrar nodo para reposicionar");

    private final String label;
    private final String hint;

    InteractionMode(String label, String hint) {
        this.label = label;
        this.hint  = hint;
    }

    public String getLabel() { return label; }
    public String getHint()  { return hint;  }
}
