package de.citec.sc.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import templates.AbstractTemplate;

public class Setting {

	final Set<Class<? extends AbstractTemplate>> orderedOption;

	public Setting(Class<? extends AbstractTemplate>... orderedOption) {
		this.orderedOption = new LinkedHashSet<>(Arrays.asList(orderedOption));

	}

	public Setting(List<Class<? extends AbstractTemplate>> templates) {
		this.orderedOption = new LinkedHashSet<>(templates);
	}

	@Override
	public String toString() {
		return "Option [orderedOption=" + orderedOption + "]";
	}

	public void addTemaplte(Class<? extends AbstractTemplate> template) {
		this.orderedOption.add(template);
	}

	public ArrayList<Class<? extends AbstractTemplate>> getSetting() {
		return new ArrayList<>(this.orderedOption);
	}

}
