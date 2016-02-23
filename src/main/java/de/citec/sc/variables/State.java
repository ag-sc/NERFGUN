/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.variables;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.corpus.Document;
import factors.FactorGraph;
import utility.VariableID;
import variables.AbstractState;

public class State extends AbstractState implements Serializable {

	private static Logger log = LogManager.getFormatterLogger();

	private static final String GENERATED_ENTITY_ID_PREFIX = "G";
	private static final DecimalFormat scoreFormat = new DecimalFormat("0.00000");

	/**
	 * Since Entities only hold weak pointer via references to one another,
	 * using a Map is sensible to enable an efficient access to the entities.
	 */
	private Map<VariableID, Annotation> entities = new HashMap<>();

	private AtomicInteger entityIDIndex = new AtomicInteger();
	/**
	 * The state needs to keep track of the changes that were made to its
	 * entities in order to allow for efficient computation of factors and their
	 * features. Note: The changes are not stored in the Entity object since it
	 * is more efficient to just clear this map instead of iterating over all
	 * entities and reset a field in order to mark all entities as unchanged.
	 */
	private Document document;

	private State() {
		super();
	}

	/**
	 * This Copy Constructor creates an exact copy of itself including all
	 * internal annotations.
	 *
	 * @param state
	 */
	public State(State state) {
		this();
		this.entityIDIndex = new AtomicInteger(state.entityIDIndex.get());
		this.document = state.document;
		this.factorGraph = new FactorGraph(state.factorGraph);
		for (Annotation e : state.entities.values()) {
			this.entities.put(e.getID(), e.clone());
		}

		this.modelScore = state.modelScore;
		this.objectiveScore = state.objectiveScore;
	}

	public State(Document document) {
		this();
		this.document = document;
	}

	public Document getDocument() {
		return document;
	}

	public void addEntity(Annotation entity) {
		log.debug("State %s: ADD new annotation: %s", this.getID(), entity);
		entities.put(entity.getID(), entity);

		// changedEntities.put(entity.getID(), StateChange.ADD_ANNOTATION);
	}

	public void removeEntity(Annotation entity) {
		log.debug("State %s: REMOVE annotation: %s", this.getID(), entity);
		entities.remove(entity.getID());

	}

	public void removeEntity(VariableID entityID) {
		Annotation entity = getEntity(entityID);
		if (entity != null) {
			log.debug("State %s: REMOVE annotation: %s", this.getID(), entity);
			entities.remove(entityID);

		} else {
			log.warn("Cannot remove entity %s. Entity not found!", entityID);
		}
	}

	/**
	 * Returns ALL entity IDs in this state. This includes entities marked as
	 * fixed. Explorers should consider using the getNonFixedEntityIDs() method.
	 *
	 * @return
	 */
	public Set<VariableID> getEntityIDs() {
		return entities.keySet();
	}

	/**
	 * Returns ALL entities in this state. This includes entities marked as
	 * fixed. Explorers should consider using the getNonFixedEntities() method.
	 *
	 * @return
	 */
	public Collection<Annotation> getEntities() {
		return entities.values();
	}

	public Annotation getEntity(VariableID id) {
		return entities.get(id);
	}

	public VariableID generateEntityID() {
		int currentID = entityIDIndex.getAndIncrement();
		String id = GENERATED_ENTITY_ID_PREFIX + currentID;
		return new VariableID(id);
	}

	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ID:");
		builder.append(id);
		builder.append(" [");
		builder.append(scoreFormat.format(modelScore));
		builder.append("]: ");
		builder.append(" [");
		builder.append(scoreFormat.format(objectiveScore));
		builder.append("]: ");
		builder.append(getDocument().toString() + "\n\n");

		for (Annotation a : sortByValue(entities).values()) {
			builder.append(a.toString() + "\n");
		}

		return builder.toString();
	}

	private HashMap<VariableID, Annotation> sortByValue(Map<VariableID, Annotation> unsortMap) {

		if (unsortMap == null) {
			return new HashMap<>();
		}

		List<Map.Entry<VariableID, Annotation>> list = new LinkedList<Map.Entry<VariableID, Annotation>>(
				unsortMap.entrySet());

		// Sorting the list based on values
		Collections.sort(list, new Comparator<Map.Entry<VariableID, Annotation>>() {
			public int compare(Map.Entry<VariableID, Annotation> o1, Map.Entry<VariableID, Annotation> o2) {

				return Integer.compare(o1.getValue().getStartIndex(), o2.getValue().getStartIndex());
			}
		});

		// Maintaining insertion order with the help of LinkedList
		HashMap<VariableID, Annotation> sortedMap = new LinkedHashMap<VariableID, Annotation>();
		for (Map.Entry<VariableID, Annotation> entry : list) {
			sortedMap.put(entry.getKey(), entry.getValue());
		}

		return sortedMap;
	}

}
