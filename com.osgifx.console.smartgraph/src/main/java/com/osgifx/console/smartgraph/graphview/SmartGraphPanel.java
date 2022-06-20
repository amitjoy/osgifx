/*******************************************************************************
 * Copyright 2021-2022 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.smartgraph.graphview;

import static com.osgifx.console.smartgraph.graphview.UtilitiesJavaFX.pick;
import static com.osgifx.console.smartgraph.graphview.UtilitiesPoint2D.attractiveForce;
import static com.osgifx.console.smartgraph.graphview.UtilitiesPoint2D.repellingForce;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.FrameworkUtil;

import com.google.common.collect.Maps;
import com.osgifx.console.smartgraph.graph.Digraph;
import com.osgifx.console.smartgraph.graph.Edge;
import com.osgifx.console.smartgraph.graph.Graph;
import com.osgifx.console.smartgraph.graph.Vertex;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;

/**
 * JavaFX {@link Pane} that is capable of plotting a {@link Graph} or
 * {@link Digraph}. <br>
 * Be sure to call {@link #init() } after the Stage is displayed. <br>
 * Whenever changes to the underlying graph are made, you should call
 * {@link #update()} to force the rendering of any new elements and, also, the
 * removal of others, if applicable. <br>
 * Vertices can be dragged by the user, if configured to do so. Consequently,
 * any connected edges will also adjust automatically to the new vertex
 * positioning.
 *
 * @param <V> Type of element stored at a vertex
 * @param <E> Type of element stored at an edge
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class SmartGraphPanel<V, E> extends Pane {

	/*
	 * CONFIGURATION PROPERTIES
	 */
	private final SmartGraphProperties graphProperties;

	/*
	 * INTERNAL DATA STRUCTURE
	 */
	private final Graph<V, E>                               theGraph;
	private final SmartPlacementStrategy                    placementStrategy;
	private final Map<Vertex<V>, SmartGraphVertexNode<V>>   vertexNodes;
	private final Map<Edge<E, V>, SmartGraphEdgeBase>       edgeNodes;
	private Map<Edge<E, V>, Tuple<Vertex<V>>>               connections;
	private final Map<Tuple<SmartGraphVertexNode>, Integer> placedEdges = Maps.newHashMap();
	private boolean                                         initialized;
	private final boolean                                   edgesWithArrows;

	/*
	 * INTERACTION WITH VERTICES AND EDGES
	 */
	private Consumer<SmartGraphVertex<V>>  vertexClickConsumer;
	private Consumer<SmartGraphEdge<E, V>> edgeClickConsumer;

	/*
	 * AUTOMATIC LAYOUT RELATED ATTRIBUTES
	 */
	public final BooleanProperty automaticLayoutProperty;
	private AnimationTimer       timer;
	private final double         repulsionForce;
	private final double         attractionForce;
	private final double         attractionScale;

	// This value was obtained experimentally
	private static final int AUTOMATIC_LAYOUT_ITERATIONS = 20;

	/**
	 * Constructs a visualization of the graph referenced by <code>theGraph</code>,
	 * using default properties and default random placement of vertices.
	 *
	 * @param theGraph underlying graph
	 *
	 * @see Graph
	 */
	public SmartGraphPanel(final Graph<V, E> theGraph) {
		this(theGraph, new SmartGraphProperties(), null);
	}

	/**
	 * Constructs a visualization of the graph referenced by <code>theGraph</code>,
	 * using custom properties and default random placement of vertices.
	 *
	 * @param theGraph   underlying graph
	 * @param properties custom properties
	 */
	public SmartGraphPanel(final Graph<V, E> theGraph, final SmartGraphProperties properties) {
		this(theGraph, properties, null);
	}

	/**
	 * Constructs a visualization of the graph referenced by <code>theGraph</code>,
	 * using default properties and custom placement of vertices.
	 *
	 * @param theGraph          underlying graph
	 * @param placementStrategy placement strategy, null for default
	 */
	public SmartGraphPanel(final Graph<V, E> theGraph, final SmartPlacementStrategy placementStrategy) {
		this(theGraph, null, placementStrategy);
	}

	/**
	 * Constructs a visualization of the graph referenced by <code>theGraph</code>,
	 * using custom properties and custom placement of vertices.
	 *
	 * @param theGraph          underlying graph
	 * @param properties        custom properties, null for default
	 * @param placementStrategy placement strategy, null for default
	 */
	public SmartGraphPanel(final Graph<V, E> theGraph, final SmartGraphProperties properties,
	        final SmartPlacementStrategy placementStrategy) {

		this(theGraph, properties, placementStrategy, null);
	}

	/**
	 * Constructs a visualization of the graph referenced by <code>theGraph</code>,
	 * using custom properties and custom placement of vertices.
	 *
	 * @param theGraph          underlying graph
	 * @param properties        custom properties, null for default
	 * @param placementStrategy placement strategy, null for default
	 * @param cssFile           alternative css file, instead of default
	 *                          'smartgraph.css'
	 */
	public SmartGraphPanel(final Graph<V, E> theGraph, final SmartGraphProperties properties,
	        final SmartPlacementStrategy placementStrategy, final URI cssFile) {

		if (theGraph == null) {
			throw new IllegalArgumentException("The graph cannot be null.");
		}
		this.theGraph          = theGraph;
		this.graphProperties   = properties != null ? properties : new SmartGraphProperties();
		this.placementStrategy = placementStrategy != null ? placementStrategy : new SmartRandomPlacementStrategy();

		this.edgesWithArrows = this.graphProperties.getUseEdgeArrow();

		this.repulsionForce  = this.graphProperties.getRepulsionForce();
		this.attractionForce = this.graphProperties.getAttractionForce();
		this.attractionScale = this.graphProperties.getAttractionScale();

		vertexNodes = Maps.newHashMap();
		edgeNodes   = Maps.newHashMap();
		connections = Maps.newHashMap();

		// set stylesheet and class
		loadStylesheet(cssFile);

		initNodes();

		enableDoubleClickListener();

		// automatic layout initializations
		timer = new AnimationTimer() {

			@Override
			public void handle(final long now) {
				runLayoutIteration();
			}
		};

		this.automaticLayoutProperty = new SimpleBooleanProperty(false);
		this.automaticLayoutProperty.addListener((observable, oldValue, newValue) -> {
			if (newValue) {
				timer.start();
			} else {
				timer.stop();
			}
		});

	}

	private synchronized void runLayoutIteration() {
		for (var i = 0; i < AUTOMATIC_LAYOUT_ITERATIONS; i++) {
			resetForces();
			computeForces();
			updateForces();
		}
		applyForces();
	}

	/**
	 * Runs the initial current vertex placement strategy.
	 * <p>
	 * This method should only be called once during the lifetime of the object and
	 * only after the underlying {@link Scene} is displayed.
	 *
	 * Further required updates should be performed through the {@link #update() }
	 * method.
	 *
	 * @throws IllegalStateException The exception is thrown if: (1) the Scene is
	 *                               not yet displayed; (2) It has zero width and/or
	 *                               height, and; (3) If this method was already
	 *                               called.
	 */
	public void init() throws IllegalStateException {
		if (this.initialized) {
			throw new IllegalStateException("Already initialized. Use update() method instead.");
		}

		final var width  = getPrefWidth();
		final var height = getPrefHeight();

		if (placementStrategy != null) {
			// call strategy to place the vertices in their initial locations
			placementStrategy.place(width, height, this.theGraph, this.vertexNodes.values());
		} else {
			// apply random placement
			new SmartRandomPlacementStrategy().place(width, height, this.theGraph, this.vertexNodes.values());
			// start automatic layout
			timer.start();
		}
		this.initialized = true;
	}

	/**
	 * Returns the property used to toggle the automatic layout of vertices.
	 *
	 * @return automatic layout property
	 */
	public BooleanProperty automaticLayoutProperty() {
		return this.automaticLayoutProperty;
	}

	/**
	 * Toggle the automatic layout of vertices.
	 *
	 * @param value true if enabling; false, otherwise
	 */
	public void setAutomaticLayout(final boolean value) {
		automaticLayoutProperty.set(value);
	}

	/**
	 * Forces a refresh of the visualization based on current state of the
	 * underlying graph, immediately returning to the caller.
	 *
	 * This method invokes the refresh in the graphical thread through
	 * Platform.runLater(), so its not guaranteed that the visualization is in sync
	 * immediately after this method finishes. That is, this method immediately
	 * returns to the caller without waiting for the update to the visualization.
	 * <p>
	 * New vertices will be added close to adjacent ones or randomly for isolated
	 * vertices.
	 */
	public void update() {
		if (getScene() == null) {
			throw new IllegalStateException("You must call this method after the instance was added to a scene.");
		}

		if (!this.initialized) {
			throw new IllegalStateException("You must call init() method before any updates.");
		}

		// this will be called from a non-javafx thread, so this must be guaranteed to
		// run of the graphics thread
		Platform.runLater(this::updateNodes);

	}

	/**
	 * Forces a refresh of the visualization based on current state of the
	 * underlying graph and waits for completion of the update.
	 *
	 * Use this variant only when necessary, e.g., need to style an element
	 * immediately after adding it to the underlying graph. Otherwise, use
	 * {@link #update() } instead for performance sake.
	 * <p>
	 * New vertices will be added close to adjacent ones or randomly for isolated
	 * vertices.
	 */
	public void updateAndWait() {
		if (getScene() == null) {
			throw new IllegalStateException("You must call this method after the instance was added to a scene.");
		}

		if (!this.initialized) {
			throw new IllegalStateException("You must call init() method before any updates.");
		}

		final FutureTask<?> update = new FutureTask<>(() -> {
			updateNodes();
			return true;
		});

		//
		if (!Platform.isFxApplicationThread()) {
			// this will be called from a non-javafx thread, so this must be guaranteed to
			// run of the graphics thread
			Platform.runLater(update);

			// wait for completion, only outside javafx thread; otherwise -> deadlock
			try {
				update.get(1, TimeUnit.SECONDS);
			} catch (InterruptedException | ExecutionException | TimeoutException ex) {
				Logger.getLogger(SmartGraphPanel.class.getName()).log(Level.SEVERE, null, ex);
			}
		} else {
			updateNodes();
		}

	}

	private synchronized void updateNodes() {
		removeNodes();
		insertNodes();
		updateLabels();
	}

	/*
	 * INTERACTION WITH VERTICES AND EDGES
	 */
	/**
	 * Sets the action that should be performed when a vertex is double clicked.
	 *
	 * @param action action to be performed
	 */
	public void setVertexDoubleClickAction(final Consumer<SmartGraphVertex<V>> action) {
		this.vertexClickConsumer = action;
	}

	/**
	 * Sets the action that should be performed when an edge is double clicked.
	 *
	 * @param action action to be performed
	 */
	public void setEdgeDoubleClickAction(final Consumer<SmartGraphEdge<E, V>> action) {
		this.edgeClickConsumer = action;
	}

	/*
	 * NODES CREATION/UPDATES
	 */
	private void initNodes() {

		/* create vertex graphical representations */
		for (final Vertex<V> vertex : listOfVertices()) {
			final var vertexAnchor = new SmartGraphVertexNode<>(vertex, 0, 0, graphProperties.getVertexRadius(),
			        graphProperties.getVertexAllowUserMove());
			final var styleClass   = getStyleClass(vertex, theGraph);
			vertexAnchor.setStyleClass(styleClass);
			vertexNodes.put(vertex, vertexAnchor);
		}

		/* create edges graphical representations between existing vertices */
		// this is used to guarantee that no duplicate edges are ever inserted
		final var edgesToPlace = listOfEdges();

		for (final Vertex<V> vertex : vertexNodes.keySet()) {

			final Iterable<Edge<E, V>> incidentEdges = theGraph.incidentEdges(vertex);

			for (final Edge<E, V> edge : incidentEdges) {

				// if already plotted, ignore edge.
				if (!edgesToPlace.contains(edge)) {
					continue;
				}

				final var oppositeVertex = theGraph.opposite(vertex, edge);

				final var graphVertexIn          = vertexNodes.get(vertex);
				final var graphVertexOppositeOut = vertexNodes.get(oppositeVertex);

				graphVertexIn.addAdjacentVertex(graphVertexOppositeOut);
				graphVertexOppositeOut.addAdjacentVertex(graphVertexIn);

				final var graphEdge = createEdge(edge, graphVertexIn, graphVertexOppositeOut);

				/* Track Edges already placed */
				connections.put(edge, new Tuple<>(vertex, oppositeVertex));
				addEdge(graphEdge, edge);

				if (this.edgesWithArrows) {
					final var arrow = new SmartArrow(this.graphProperties.getEdgeArrowSize());
					graphEdge.attachArrow(arrow);
					getChildren().add(arrow);
				}

				edgesToPlace.remove(edge);
			}

		}

		/* place vertices above lines */
		for (final Vertex<V> vertex : vertexNodes.keySet()) {
			final var v = vertexNodes.get(vertex);

			addVertex(v);
		}
	}

	private SmartGraphEdgeBase createEdge(final Edge<E, V> edge, final SmartGraphVertexNode<V> graphVertexInbound,
	        final SmartGraphVertexNode<V> graphVertexOutbound) {
		/*
		 * Even if edges are later removed, the corresponding index remains the same.
		 * Otherwise, we would have to regenerate the appropriate edges.
		 */
		var       edgeIndex = 0;
		final var counter   = placedEdges.get(new Tuple<>(graphVertexInbound, graphVertexOutbound));
		if (counter != null) {
			edgeIndex = counter;
		}

		SmartGraphEdgeBase graphEdge;

		if (getTotalEdgesBetween(graphVertexInbound.getUnderlyingVertex(), graphVertexOutbound.getUnderlyingVertex()) > 1
		        || graphVertexInbound == graphVertexOutbound) {
			graphEdge = new SmartGraphEdgeCurve<>(edge, graphVertexInbound, graphVertexOutbound, edgeIndex);
		} else {
			graphEdge = new SmartGraphEdgeLine<>(edge, graphVertexInbound, graphVertexOutbound);
		}

		edgeIndex++;
		placedEdges.put(new Tuple<>(graphVertexInbound, graphVertexOutbound), edgeIndex);

		return graphEdge;
	}

	private void addVertex(final SmartGraphVertexNode<V> v) {
		getChildren().add(v);

		final var labelText = generateVertexLabel(v.getUnderlyingVertex().element());

		if (graphProperties.getUseVertexTooltip()) {
			final var t = new Tooltip(labelText);
			Tooltip.install(v, t);
		}

		if (graphProperties.getUseVertexLabel()) {
			final var label = new SmartLabel(labelText);

			label.addStyleClass("vertex-label");
			getChildren().add(label);
			v.attachLabel(label);
		}
	}

	private void addEdge(final SmartGraphEdgeBase e, final Edge<E, V> edge) {
		// edges to the back
		getChildren().add(0, (Node) e);
		edgeNodes.put(edge, e);

		final var labelText = generateEdgeLabel(edge.element());

		if (graphProperties.getUseEdgeTooltip()) {
			final var t = new Tooltip(labelText);
			Tooltip.install((Node) e, t);
		}

		if (graphProperties.getUseEdgeLabel()) {
			final var label = new SmartLabel(labelText);

			label.addStyleClass("edge-label");
			getChildren().add(label);
			e.attachLabel(label);
		}
	}

	private void insertNodes() {
		final var unplottedVertices = unplottedVertices();

		List<SmartGraphVertexNode<V>> newVertices = null;

		final var bounds = getPlotBounds();
		final var mx     = bounds.getMinX() + bounds.getWidth() / 2.0;
		final var my     = bounds.getMinY() + bounds.getHeight() / 2.0;

		if (!unplottedVertices.isEmpty()) {

			newVertices = new LinkedList<>();

			for (final Vertex<V> vertex : unplottedVertices) {
				// create node
				// Place new nodes in the vicinity of existing adjacent ones;
				// Place them in the middle of the plot, otherwise.
				double    x, y;
				final var incidentEdges = theGraph.incidentEdges(vertex);
				if (incidentEdges.isEmpty()) {
					/* not (yet) connected, put in the middle of the plot */
					x = mx;
					y = my;
				} else {
					final var firstEdge = incidentEdges.iterator().next();
					final var opposite  = theGraph.opposite(vertex, firstEdge);
					final var existing  = vertexNodes.get(opposite);

					if (existing == null) {
						/*
						 * Updates may be coming too fast and we can get out of sync. The opposite
						 * vertex exists in the (di)graph, but we have not yet created it for the panel.
						 * Therefore, its position is unknown, so place the vertex representation in the
						 * middle.
						 */
						x = mx;
						y = my;
					} else {
						/* TODO: fix -- the placing point can be set out of bounds */
						final var p = UtilitiesPoint2D.rotate(existing.getPosition().add(50.0, 50.0), existing.getPosition(),
						        Math.random() * 360);

						x = p.getX();
						y = p.getY();
					}
				}

				final SmartGraphVertexNode newVertex = new SmartGraphVertexNode<>(vertex, x, y, graphProperties.getVertexRadius(),
				        graphProperties.getVertexAllowUserMove());

				// track new nodes
				newVertices.add(newVertex);
				// add to global mapping
				vertexNodes.put(vertex, newVertex);
			}

		}

		final var unplottedEdges = unplottedEdges();
		if (!unplottedEdges.isEmpty()) {
			for (final Edge<E, V> edge : unplottedEdges) {

				final var vertices = edge.vertices();
				final var u        = vertices[0];    // oubound if digraph, by javadoc requirement
				final var v        = vertices[1];    // inbound if digraph, by javadoc requirement

				final var graphVertexOut = vertexNodes.get(u);
				final var graphVertexIn  = vertexNodes.get(v);

				/*
				 * Updates may be coming too fast and we can get out of sync. Skip and wait for
				 * another update call, since they will surely be coming at this pace.
				 */
				if (graphVertexIn == null || graphVertexOut == null) {
					continue;
				}

				graphVertexOut.addAdjacentVertex(graphVertexIn);
				graphVertexIn.addAdjacentVertex(graphVertexOut);

				final var graphEdge = createEdge(edge, graphVertexIn, graphVertexOut);

				if (this.edgesWithArrows) {
					final var arrow = new SmartArrow(this.graphProperties.getEdgeArrowSize());
					graphEdge.attachArrow(arrow);
					getChildren().add(arrow);
				}

				/* Track edges */
				connections.put(edge, new Tuple<>(u, v));
				addEdge(graphEdge, edge);

			}
		}

		if (newVertices != null) {
			for (final SmartGraphVertexNode<V> v : newVertices) {
				addVertex(v);
			}
		}

	}

	private void removeNodes() {
		// remove edges (graphical elements) that were removed from the underlying graph
		final var removedEdges = removedEdges();
		for (final Edge<E, V> e : removedEdges) {
			final var edgeToRemove = edgeNodes.get(e);
			edgeNodes.remove(e);
			removeEdge(edgeToRemove); // remove from panel

			// when edges are removed, the adjacency between vertices changes
			// the adjacency is kept in parallel in an internal data structure
			final var vertexTuple = connections.get(e);

			if (getTotalEdgesBetween(vertexTuple.first, vertexTuple.second) == 0) {
				final var v0 = vertexNodes.get(vertexTuple.first);
				final var v1 = vertexNodes.get(vertexTuple.second);

				v0.removeAdjacentVertex(v1);
				v1.removeAdjacentVertex(v0);
			}

			connections.remove(e);
		}

		// remove vertices (graphical elements) that were removed from the underlying
		// graph
		final var removedVertices = removedVertices();
		for (final Vertex<V> removedVertex : removedVertices) {
			final var removed = vertexNodes.remove(removedVertex);
			removeVertex(removed);
		}

	}

	@SuppressWarnings("unlikely-arg-type")
	private void removeEdge(final SmartGraphEdgeBase e) {
		getChildren().remove(e);

		final var attachedArrow = e.getAttachedArrow();
		if (attachedArrow != null) {
			getChildren().remove(attachedArrow);
		}

		final Text attachedLabel = e.getAttachedLabel();
		if (attachedLabel != null) {
			getChildren().remove(attachedLabel);
		}
	}

	private void removeVertex(final SmartGraphVertexNode v) {
		getChildren().remove(v);

		final Text attachedLabel = v.getAttachedLabel();
		if (attachedLabel != null) {
			getChildren().remove(attachedLabel);
		}
	}

	/**
	 * Updates node's labels
	 */
	private void updateLabels() {
		theGraph.vertices().forEach(v -> {
			final var vertexNode = vertexNodes.get(v);
			if (vertexNode != null) {
				final var label = vertexNode.getAttachedLabel();
				if (label != null) {
					final var text = generateVertexLabel(v.element());
					label.setText(text);
				}

			}
		});

		theGraph.edges().forEach(e -> {
			final var edgeNode = edgeNodes.get(e);
			if (edgeNode != null) {
				final var label = edgeNode.getAttachedLabel();
				if (label != null) {
					final var text = generateEdgeLabel(e.element());
					label.setText(text);
				}
			}
		});
	}

	private String generateVertexLabel(final V vertex) {

		try {
			final Class<?> clazz = vertex.getClass();
			for (final Method method : clazz.getDeclaredMethods()) {
				if (method.isAnnotationPresent(SmartLabelSource.class)) {
					method.setAccessible(true);
					final var value = method.invoke(vertex);
					return value.toString();
				}
			}
		} catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
			Logger.getLogger(SmartGraphPanel.class.getName()).log(Level.SEVERE, null, ex);
		}

		return vertex != null ? vertex.toString() : "<NULL>";
	}

	private String generateEdgeLabel(final E edge) {

		try {
			final Class<?> clazz = edge.getClass();
			for (final Method method : clazz.getDeclaredMethods()) {
				if (method.isAnnotationPresent(SmartLabelSource.class)) {
					method.setAccessible(true);
					final var value = method.invoke(edge);
					return value.toString();
				}
			}
		} catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
			Logger.getLogger(SmartGraphPanel.class.getName()).log(Level.SEVERE, null, ex);
		}

		return edge != null ? edge.toString() : "<NULL>";
	}

	/**
	 * Computes the bounding box from all displayed vertices.
	 *
	 * @return bounding box
	 */
	private Bounds getPlotBounds() {
		double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;

		if (vertexNodes.size() == 0) {
			return new BoundingBox(0, 0, getWidth(), getHeight());
		}

		for (final SmartGraphVertexNode<V> v : vertexNodes.values()) {
			minX = Math.min(minX, v.getCenterX());
			minY = Math.min(minY, v.getCenterY());
			maxX = Math.max(maxX, v.getCenterX());
			maxY = Math.max(maxY, v.getCenterY());
		}

		return new BoundingBox(minX, minY, maxX - minX, maxY - minY);
	}

	/*
	 * AUTOMATIC LAYOUT
	 */
	private void computeForces() {
		for (final SmartGraphVertexNode<V> v : vertexNodes.values()) {
			for (final SmartGraphVertexNode<V> other : vertexNodes.values()) {
				if (v == other) {
					continue; // NOP
				}

				// double k = Math.sqrt(getWidth() * getHeight() / graphVertexMap.size());
				final var repellingForce = repellingForce(v.getUpdatedPosition(), other.getUpdatedPosition(), this.repulsionForce);

				double deltaForceX = 0, deltaForceY = 0;

				// compute attractive and reppeling forces
				// opt to use internal areAdjacent check, because a vertex can be removed from
				// the underlying graph before we have the chance to remove it from our
				// internal data structure
				if (areAdjacent(v, other)) {

					final var attractiveForce = attractiveForce(v.getUpdatedPosition(), other.getUpdatedPosition(), vertexNodes.size(),
					        this.attractionForce, this.attractionScale);

					deltaForceX = attractiveForce.getX() + repellingForce.getX();
					deltaForceY = attractiveForce.getY() + repellingForce.getY();
				} else {
					deltaForceX = repellingForce.getX();
					deltaForceY = repellingForce.getY();
				}

				v.addForceVector(deltaForceX, deltaForceY);
			}
		}
	}

	private boolean areAdjacent(final SmartGraphVertexNode<V> v, final SmartGraphVertexNode<V> u) {
		return v.isAdjacentTo(u);
	}

	private void updateForces() {
		vertexNodes.values().forEach(v -> {
			v.updateDelta();
		});
	}

	private void applyForces() {
		vertexNodes.values().forEach(v -> {
			v.moveFromForces();
		});
	}

	private void resetForces() {
		vertexNodes.values().forEach(v -> {
			v.resetForces();
		});
	}

	private int getTotalEdgesBetween(final Vertex<V> v, final Vertex<V> u) {
		// TODO: It may be necessary to adjust this method if you use another Graph
		// variant, e.g., Digraph (directed graph)
		var count = 0;
		for (final Edge<E, V> edge : theGraph.edges()) {
			if (edge.vertices()[0] == v && edge.vertices()[1] == u || edge.vertices()[0] == u && edge.vertices()[1] == v) {
				count++;
			}
		}
		return count;
	}

	private List<Edge<E, V>> listOfEdges() {
		return new LinkedList<>(theGraph.edges());
	}

	private List<Vertex<V>> listOfVertices() {
		return new LinkedList<>(theGraph.vertices());
	}

	/**
	 * Computes the vertex collection of the underlying graph that are not currently
	 * being displayed.
	 *
	 * @return collection of vertices
	 */
	private Collection<Vertex<V>> unplottedVertices() {
		final List<Vertex<V>> unplotted = new LinkedList<>();

		for (final Vertex<V> v : theGraph.vertices()) {
			if (!vertexNodes.containsKey(v)) {
				unplotted.add(v);
			}
		}

		return unplotted;
	}

	/**
	 * Computes the collection for vertices that are currently being displayed but
	 * do not longer exist in the underlying graph.
	 *
	 * @return collection of vertices
	 */
	private Collection<Vertex<V>> removedVertices() {
		final List<Vertex<V>> removed = new LinkedList<>();

		final var graphVertices = theGraph.vertices();
		final var plotted       = vertexNodes.values();

		for (final SmartGraphVertexNode<V> v : plotted) {
			if (!graphVertices.contains(v.getUnderlyingVertex())) {
				removed.add(v.getUnderlyingVertex());
			}
		}

		return removed;
	}

	/**
	 * Computes the collection for edges that are currently being displayed but do
	 * not longer exist in the underlying graph.
	 *
	 * @return collection of edges
	 */
	private Collection<Edge<E, V>> removedEdges() {
		final List<Edge<E, V>> removed = new LinkedList<>();

		final var graphEdges = theGraph.edges();
		final var plotted    = edgeNodes.values();

		for (final SmartGraphEdgeBase e : plotted) {
			if (!graphEdges.contains(e.getUnderlyingEdge())) {
				removed.add(e.getUnderlyingEdge());
			}
		}

		return removed;
	}

	/**
	 * Computes the edge collection of the underlying graph that are not currently
	 * being displayed.
	 *
	 * @return collection of edges
	 */
	private Collection<Edge<E, V>> unplottedEdges() {
		final List<Edge<E, V>> unplotted = new LinkedList<>();

		for (final Edge<E, V> e : theGraph.edges()) {
			if (!edgeNodes.containsKey(e)) {
				unplotted.add(e);
			}
		}

		return unplotted;
	}

	/**
	 * Sets a vertex position (its center) manually.
	 *
	 * The positioning should be inside the boundaries of the panel, but no
	 * restrictions are enforced by this method, so be aware.
	 *
	 * @param v underlying vertex
	 * @param x x-coordinate on panel
	 * @param y y-coordinate on panel
	 */
	public void setVertexPosition(final Vertex<V> v, final double x, final double y) {
		final var node = vertexNodes.get(v);
		if (node != null) {
			node.setPosition(x, y);
		}
	}

	/**
	 * Return the current x-coordinate (relative to the panel) of a vertex.
	 *
	 * @param v underlying vertex
	 * @return the x-coordinate or NaN if the vertex does not exist
	 */
	public double getVertexPositionX(final Vertex<V> v) {
		final var node = vertexNodes.get(v);
		if (node != null) {
			return node.getPositionCenterX();
		}
		return Double.NaN;
	}

	/**
	 * Return the current y-coordinate (relative to the panel) of a vertex.
	 *
	 * @param v underlying vertex
	 * @return the y-coordinate or NaN if the vertex does not exist
	 */
	public double getVertexPositionY(final Vertex<V> v) {
		final var node = vertexNodes.get(v);
		if (node != null) {
			return node.getPositionCenterY();
		}
		return Double.NaN;
	}

	/**
	 * Returns the associated stylable element with a graph vertex.
	 *
	 * @param v underlying vertex
	 * @return stylable element
	 */
	public SmartStylableNode getStylableVertex(final Vertex<V> v) {
		return vertexNodes.get(v);
	}

	/**
	 * Returns the associated stylable element with a graph vertex.
	 *
	 * @param vertexElement underlying vertex's element
	 * @return stylable element
	 */
	public SmartStylableNode getStylableVertex(final V vertexElement) {
		for (final Vertex<V> v : vertexNodes.keySet()) {
			if (v.element().equals(vertexElement)) {
				return vertexNodes.get(v);
			}
		}
		return null;
	}

	/**
	 * Returns the associated stylable element with a graph edge.
	 *
	 * @param edge underlying graph edge
	 * @return stylable element
	 */
	public SmartStylableNode getStylableEdge(final Edge<E, V> edge) {
		return edgeNodes.get(edge);
	}

	/**
	 * Returns the associated stylable element with a graph edge.
	 *
	 * @param edgeElement underlying graph edge's element
	 * @return stylable element
	 */
	public SmartStylableNode getStylableEdge(final E edgeElement) {
		for (final Edge<E, V> e : edgeNodes.keySet()) {
			if (e.element().equals(edgeElement)) {
				return edgeNodes.get(e);
			}
		}
		return null;
	}

	/**
	 * Returns the associated stylable element with a graph vertex.
	 *
	 * @param v underlying vertex
	 * @return stylable element (label)
	 */
	public SmartStylableNode getStylableLabel(final Vertex<V> v) {
		final var vertex = vertexNodes.get(v);

		return vertex != null ? vertex.getStylableLabel() : null;
	}

	/**
	 * Returns the associated stylable element with a graph edge.
	 *
	 * @param e underlying graph edge
	 * @return stylable element (label)
	 */
	public SmartStylableNode getStylableLabel(final Edge<E, V> e) {
		final var edge = edgeNodes.get(e);

		return edge != null ? edge.getStylableLabel() : null;
	}

	/**
	 * Loads the stylesheet and applies the .graph class to this panel.
	 */
	private void loadStylesheet(final URI cssFile) {
		try {
			String css;
			if (cssFile != null) {
				css = cssFile.toURL().toExternalForm();
			} else {
				final var bundle = FrameworkUtil.getBundle(getClass());
				css = bundle.getEntry("smartgraph.css").toExternalForm();
			}
			getStylesheets().add(css);
			getStyleClass().add("graph");
		} catch (final MalformedURLException ex) {
			Logger.getLogger(SmartGraphPanel.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * Enables the double click action on this pane.
	 *
	 * This method identifies the node that was clicked and, if any, calls the
	 * appropriate consumer, i.e., vertex or edge consumers.
	 */
	private void enableDoubleClickListener() {
		setOnMouseClicked((final MouseEvent mouseEvent) -> {
			if (MouseButton.PRIMARY.equals(mouseEvent.getButton()) && mouseEvent.getClickCount() == 2) {
				// no need to continue otherwise
				if (vertexClickConsumer == null && edgeClickConsumer == null) {
					return;
				}

				final var node = pick(SmartGraphPanel.this, mouseEvent.getSceneX(), mouseEvent.getSceneY());
				if (node == null) {
					return;
				}

				if (node instanceof final SmartGraphVertex v) {
					vertexClickConsumer.accept(v);
				} else if (node instanceof final SmartGraphEdge e) {
					edgeClickConsumer.accept(e);
				}

			}
		});
	}

	/**
	 * Represents a tuple in Java.
	 *
	 * @param <T> the type of the tuple
	 */
	private class Tuple<T> {

		private final T first;
		private final T second;

		public Tuple(final T first, final T second) {
			this.first  = first;
			this.second = second;
		}

		@Override
		public int hashCode() {
			var hash = 7;
			hash = 29 * hash + Objects.hashCode(this.first);
			return 29 * hash + Objects.hashCode(this.second);
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			final Tuple<?> other = (Tuple<?>) obj;
			if (!Objects.equals(this.first, other.first) || !Objects.equals(this.second, other.second)) {
				return false;
			}
			return true;
		}
	}

	private String getStyleClass(final Vertex<V> vertex, final Graph<V, E> graph) {
		if (graph.incidentEdges(vertex).isEmpty()) {
			return "root-vertex";
		}
		return "vertex";
	}

}
