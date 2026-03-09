package graphcoloringapp.controller;

import graphcoloringapp.model.*;
import graphcoloringapp.view.GraphCanvas;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.ButtonBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import java.util.Optional;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.net.URL;
import java.util.*;

// Controlador principal – patrón MVC.
// Coordina eventos del canvas, animación de algoritmos y actualización de la vista.
public class MainController implements Initializable {

    // Componentes FXML
    @FXML private StackPane   canvasPane;
    @FXML private TextField   tfNodeId, tfEdgeU, tfEdgeV;
    @FXML private Label       lblNodes, lblEdges, lblColors, lblChromatic;
    @FXML private Label       lblStatus, lblMode, lblCanvasHint;
    @FXML private ListView<String> logList;
    @FXML private VBox        legendBox;
    @FXML private ToggleGroup algoGroup;
    @FXML private ToggleButton btnBFS, btnDFS;
    @FXML private Button      btnStep;
    @FXML private ToggleGroup modeGroup;
    @FXML private ToggleButton btnModeAdd, btnModeEdge, btnModeDelete, btnModeMove;
    @FXML private Label       lblModeDesc;

    // Modelo y vista
    private final Graph model  = new Graph();
    private GraphCanvas canvas;

    // Estado del controlador
    private int               nextAutoId    = 0;
    private InteractionMode   currentMode   = InteractionMode.ADD_NODE;
    private NodeView          edgeStartNode = null;
    private boolean           stepMode      = false;
    private List<ColoringStep> pendingSteps = new ArrayList<>();
    private int               stepIndex     = 0;
    private Timeline          autoTimeline;

    private static final String[] COLOR_NAMES = {
        "Rojo","Azul","Verde","Naranja","Violeta",
        "Turquesa","Naranja Osc.","Gris Azul","Rojo Osc.","Verde Osc."
    };

    // ── Inicialización ─────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        canvas = new GraphCanvas(model);
        canvasPane.getChildren().add(0, canvas);
        // El canvas se redimensiona con el panel contenedor
        canvas.widthProperty().bind(canvasPane.widthProperty());
        canvas.heightProperty().bind(canvasPane.heightProperty());
        canvas.widthProperty().addListener(e  -> canvas.draw());
        canvas.heightProperty().addListener(e -> canvas.draw());
        canvas.draw();
        updateStats();
        updateModeUI();
    }

    // ── Botones de modo ────────────────────────────────────────────────

    @FXML public void onModeAdd()    { setMode(InteractionMode.ADD_NODE); }
    @FXML public void onModeEdge()   { setMode(InteractionMode.ADD_EDGE); }
    @FXML public void onModeDelete() { setMode(InteractionMode.DELETE); }
    @FXML public void onModeMove()   { setMode(InteractionMode.MOVE); }

    private void setMode(InteractionMode m) {
        currentMode   = m;
        edgeStartNode = null;
        canvas.setEdgeSource(null);
        canvas.setHoverEdge(-1, -1);
        canvas.clearHighlights();
        canvas.draw();
        updateModeUI();
    }

    private void updateModeUI() {
        lblModeDesc.setText(currentMode.getHint());
        lblMode.setText("Modo activo: " + currentMode.getLabel());
        lblCanvasHint.setText(currentMode.getHint());

        // Sincroniza el toggle button con el modo actual
        if (modeGroup != null) {
            switch (currentMode) {
                case ADD_NODE: if (btnModeAdd    != null) btnModeAdd.setSelected(true);    break;
                case ADD_EDGE: if (btnModeEdge   != null) btnModeEdge.setSelected(true);   break;
                case DELETE:   if (btnModeDelete != null) btnModeDelete.setSelected(true); break;
                case MOVE:     if (btnModeMove   != null) btnModeMove.setSelected(true);   break;
            }
        }
    }

    // ── Eventos del canvas ─────────────────────────────────────────────

    @FXML
    public void onCanvasPressed(MouseEvent e) {
        double x = e.getX(), y = e.getY();
        NodeView clicked = canvas.getNodeAt(x, y);

        switch (currentMode) {
            case ADD_NODE:
                if (clicked == null) addNodeAt(x, y);
                else canvas.setDragging(clicked); // permite arrastrar en este modo
                break;

            case ADD_EDGE:
                if (clicked != null) handleEdgeDrawing(clicked);
                break;

            case DELETE:
                if (clicked != null) {
                    final int id = clicked.getId();
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirmar eliminacion");
                    confirm.setHeaderText("Eliminar Nodo " + id);
                    confirm.setContentText("¿Deseas eliminar el nodo " + id +
                            " y todas sus aristas (" + model.getNeighbors(id).size() + " conexiones)?");
                    ButtonType btnSi = new ButtonType("Si, eliminar");
                    ButtonType btnNo = new ButtonType("No, cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
                    confirm.getButtonTypes().setAll(btnSi, btnNo);
                    confirm.getDialogPane().setStyle("-fx-background-color:#16213e; -fx-font-family:Arial;");
                    Optional<ButtonType> result = confirm.showAndWait();
                    if (result.isPresent() && result.get() == btnSi) {
                        model.removeNode(id);
                        canvas.removeNodeView(id);
                        edgeStartNode = null;
                        canvas.setEdgeSource(null);
                        model.resetColors(); canvas.syncColors();
                        redrawAll();
                        setStatus("Nodo " + id + " eliminado correctamente.");
                    } else {
                        setStatus("Eliminacion cancelada.");
                    }
                } else {
                    // Intenta eliminar arista si el clic no fue sobre un nodo
                    int[] edge = canvas.getEdgeAt(x, y);
                    if (edge != null) {
                        final int eu = edge[0], ev = edge[1];
                        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                        confirm.setTitle("Confirmar eliminacion");
                        confirm.setHeaderText("Eliminar Arista " + eu + " - " + ev);
                        confirm.setContentText("¿Deseas eliminar la arista entre el nodo " + eu + " y el nodo " + ev + "?");
                        ButtonType btnSi = new ButtonType("Si, eliminar");
                        ButtonType btnNo = new ButtonType("No, cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
                        confirm.getButtonTypes().setAll(btnSi, btnNo);
                        confirm.getDialogPane().setStyle("-fx-background-color:#16213e; -fx-font-family:Arial;");
                        Optional<ButtonType> result = confirm.showAndWait();
                        if (result.isPresent() && result.get() == btnSi) {
                            model.removeEdge(eu, ev);
                            model.resetColors(); canvas.syncColors();
                            canvas.setHoverEdge(-1, -1);
                            redrawAll();
                            setStatus("Arista " + eu + " - " + ev + " eliminada correctamente.");
                        } else {
                            setStatus("Eliminacion cancelada.");
                        }
                    }
                }
                break;

            case MOVE:
                if (clicked != null) canvas.setDragging(clicked);
                break;
        }
    }

    @FXML
    public void onCanvasDragged(MouseEvent e) {
        canvas.setMousePos(e.getX(), e.getY());
        NodeView dragging = canvas.getDragging();
        if (dragging != null) {
            dragging.setX(e.getX()); dragging.setY(e.getY());
        }
        canvas.draw();
    }

    @FXML
    public void onCanvasReleased(MouseEvent e) {
        canvas.setDragging(null);
    }

    @FXML
    public void onCanvasMoved(MouseEvent e) {
        canvas.setMousePos(e.getX(), e.getY());
        if (currentMode == InteractionMode.DELETE) {
            // Resalta la arista bajo el cursor
            int[] edge = canvas.getEdgeAt(e.getX(), e.getY());
            if (edge != null) canvas.setHoverEdge(edge[0], edge[1]);
            else              canvas.setHoverEdge(-1, -1);
            canvas.draw();
        } else if (currentMode == InteractionMode.ADD_EDGE && edgeStartNode != null) {
            canvas.draw(); // actualiza la línea punteada
        }
    }

    // ── Panel lateral ──────────────────────────────────────────────────

    @FXML public void onAddNode() {
        String txt = tfNodeId.getText().trim();
        int id;
        if (txt.isEmpty()) { while (model.hasNode(nextAutoId)) nextAutoId++; id = nextAutoId; }
        else { try { id = Integer.parseInt(txt); } catch (NumberFormatException ex) { setStatus("ID invalido."); return; } }
        if (model.hasNode(id)) { setStatus("El nodo " + id + " ya existe."); return; }
        double cx = canvas.getWidth()/2  + (Math.random()-0.5)*200;
        double cy = canvas.getHeight()/2 + (Math.random()-0.5)*200;
        addNodeInternal(id, cx, cy);
        tfNodeId.clear();
    }

    @FXML public void onAddEdge() {
        try {
            int u = Integer.parseInt(tfEdgeU.getText().trim());
            int v = Integer.parseInt(tfEdgeV.getText().trim());
            if (!model.hasNode(u) || !model.hasNode(v)) { setStatus("Nodo(s) no existen."); return; }
            if (model.addEdge(u, v)) {
                model.resetColors(); canvas.syncColors(); redrawAll();
                setStatus("Arista " + u + " - " + v + " agregada.");
            } else { setStatus("Arista ya existe o es auto-lazo."); }
            tfEdgeU.clear(); tfEdgeV.clear();
        } catch (NumberFormatException ex) { setStatus("IDs invalidos."); }
    }

    // ── Coloreado y animación ──────────────────────────────────────────

    @FXML public void onColorize() {
        if (model.nodeCount() == 0) { setStatus("Grafo vacio."); return; }
        stopTimeline();
        List<ColoringStep> steps = computeSteps();
        logList.getItems().clear();
        int[] idx = {0};
        // Timeline que aplica un paso cada 220 ms
        autoTimeline = new Timeline(new KeyFrame(Duration.millis(220), ev -> {
            if (idx[0] < steps.size()) {
                ColoringStep s = steps.get(idx[0]++);
                applyStepColor(s);
                canvas.draw();
                logList.getItems().add(s.toString());
                logList.scrollTo(logList.getItems().size()-1);
                updateStats(); updateLegend();
            } else {
                canvas.clearHighlights(); canvas.draw(); autoTimeline.stop();
                setStatus("Completado. Colores usados: " + model.getColorsUsed());
            }
        }));
        autoTimeline.setCycleCount(steps.size()+1);
        autoTimeline.play();
    }

    @FXML public void onStepButton() {
        if (stepMode) { advanceStep(); return; }
        // Inicia el modo paso a paso
        stopTimeline();
        model.resetColors(); canvas.syncColors(); canvas.clearHighlights(); canvas.draw();
        logList.getItems().clear();
        pendingSteps = computeSteps();
        stepIndex = 0; stepMode = true;
        btnStep.setText("Siguiente Paso");
        setStatus("Paso a paso. Pulsa 'Siguiente Paso'.");
        updateStats(); updateLegend();
    }

    private void advanceStep() {
        if (!stepMode || pendingSteps.isEmpty()) return;
        if (stepIndex < pendingSteps.size()) {
            ColoringStep s = pendingSteps.get(stepIndex++);
            canvas.clearHighlights();
            applyStepColor(s);
            canvas.draw();
            logList.getItems().add(s.toString());
            logList.scrollTo(logList.getItems().size()-1);
            updateStats(); updateLegend();
            setStatus("Paso " + stepIndex + "/" + pendingSteps.size() + " — " + s.getDescription());
        }
        if (stepIndex >= pendingSteps.size()) {
            stepMode = false; canvas.clearHighlights(); canvas.draw();
            btnStep.setText("Paso a Paso");
            setStatus("Completado. Colores: " + model.getColorsUsed());
        }
    }

    // Calcula los pasos del algoritmo seleccionado con el modelo limpio
    private List<ColoringStep> computeSteps() {
        boolean useBFS = !btnDFS.isSelected();
        model.resetColors(); canvas.syncColors();
        return useBFS ? model.colorWithBFS() : model.colorWithDFS();
    }

    // Aplica el color de un paso al NodeView correspondiente
    private void applyStepColor(ColoringStep s) {
        graphcoloringapp.model.NodeView nv = canvas.getNodeViews().get(s.getNode());
        if (nv != null) { nv.setColorIndex(s.getColor()); nv.setHighlighted(true); }
    }

    @FXML public void onResetColors() {
        stopTimeline(); stepMode = false; btnStep.setText("Paso a Paso");
        model.resetColors(); canvas.syncColors(); canvas.clearHighlights(); canvas.draw();
        logList.getItems().clear(); updateStats(); updateLegend();
        setStatus("Colores reiniciados.");
    }

    @FXML public void onLoadExample() {
        stopTimeline(); stepMode = false; btnStep.setText("Paso a Paso");
        model.loadExample(); canvas.clearViews(); canvas.arrangeCircular();
        logList.getItems().clear(); edgeStartNode = null; canvas.setEdgeSource(null);
        nextAutoId = model.getNodes().stream().mapToInt(i->i).max().orElse(-1)+1;
        redrawAll(); setStatus("Grafo de ejemplo cargado.");
    }

    @FXML public void onClearAll() {
        stopTimeline(); stepMode = false; btnStep.setText("Paso a Paso");
        model.clear(); canvas.clearViews(); logList.getItems().clear();
        nextAutoId = 0; edgeStartNode = null; canvas.setEdgeSource(null);
        canvas.setHoverEdge(-1,-1); canvas.draw(); updateStats(); updateLegend();
        setStatus("Grafo limpiado.");
    }

    // ── Métodos auxiliares ─────────────────────────────────────────────

    private void addNodeAt(double x, double y) {
        while (model.hasNode(nextAutoId)) nextAutoId++;
        addNodeInternal(nextAutoId, x, y);
    }

    private void addNodeInternal(int id, double x, double y) {
        model.addNode(id); canvas.addNodeView(id, x, y);
        while (model.hasNode(nextAutoId)) nextAutoId++;
        redrawAll(); setStatus("Nodo " + id + " agregado.");
    }

    private void handleEdgeDrawing(NodeView nv) {
        if (edgeStartNode == null) {
            // Primer clic: selecciona el nodo origen
            edgeStartNode = nv;
            canvas.setEdgeSource(nv);
            canvas.setHighlighted(nv.getId());
            canvas.draw();
            setStatus("Nodo " + nv.getId() + " seleccionado. Toca otro nodo para conectar.");
        } else {
            // Segundo clic: crea la arista si es un nodo distinto
            if (edgeStartNode.getId() != nv.getId()) {
                if (model.addEdge(edgeStartNode.getId(), nv.getId())) {
                    model.resetColors(); canvas.syncColors();
                    setStatus("Arista " + edgeStartNode.getId() + " - " + nv.getId() + " agregada.");
                } else { setStatus("Arista ya existe."); }
            }
            edgeStartNode = null;
            canvas.setEdgeSource(null);
            canvas.setHighlighted(-1);
            canvas.clearHighlights();
            redrawAll();
        }
    }

    private void redrawAll() {
        canvas.syncColors(); canvas.draw(); updateStats(); updateLegend();
    }

    private void updateStats() {
        lblNodes.setText(String.valueOf(model.nodeCount()));
        lblEdges.setText(String.valueOf(model.edgeCount()));
        int c = model.getColorsUsed();
        lblColors.setText(c > 0 ? String.valueOf(c) : "-");
        lblChromatic.setText(c > 0 ? ">= " + c : "-");
    }

    private void updateLegend() {
        legendBox.getChildren().clear();
        for (int i = 0; i < model.getColorsUsed(); i++) {
            javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(8);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            Circle dot = new Circle(8);
            dot.setFill(GraphCanvas.PALETTE[i % GraphCanvas.PALETTE.length]);
            dot.setStroke(Color.web("#2c3e50")); dot.setStrokeWidth(1);
            Label lbl = new Label("Color " + i + " – " + COLOR_NAMES[i % COLOR_NAMES.length]);
            lbl.setStyle("-fx-text-fill:#ecf0f1;-fx-font-size:11px;");
            row.getChildren().addAll(dot, lbl);
            legendBox.getChildren().add(row);
        }
    }

    private void setStatus(String msg) { lblStatus.setText(msg); }

    private void stopTimeline() { if (autoTimeline != null) autoTimeline.stop(); }
}