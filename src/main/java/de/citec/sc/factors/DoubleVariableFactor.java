package de.citec.sc.factors;

import java.util.HashSet;
import java.util.Set;

import factors.AbstractFactor;
import templates.AbstractTemplate;
import utility.VariableID;
import variables.AbstractState;

public class DoubleVariableFactor extends AbstractFactor {

	public VariableID firstEntityID;
	public VariableID secondEntityID;

	/**
	 * This is a generic implementation for a Factor that only needs a single
	 * variable to compute its features. Therefore, this factor only stores a
	 * single VariableID, which can later be used to retrieve the actual
	 * variable to compute the features.
	 * 
	 * @param template
	 * @param entityID
	 */
	public DoubleVariableFactor(AbstractTemplate<? extends AbstractState> template, VariableID firstEntityID,
			VariableID secondEntityID) {
		super(template);
		this.firstEntityID = firstEntityID;
		this.secondEntityID = secondEntityID;
	}

	@Override
	public Set<VariableID> getVariableIDs() {
		Set<VariableID> entities = new HashSet<>();
		entities.add(firstEntityID);
		entities.add(secondEntityID);
		return entities;
	}

	@Override
	public String toString() {
		return "DoubleVariableFactor [firstEntityID=" + firstEntityID + ", secondEntityID=" + secondEntityID + "]";
	}

}
