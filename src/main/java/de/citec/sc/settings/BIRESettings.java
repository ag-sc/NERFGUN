package de.citec.sc.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.citec.sc.templates.DocumentSimilarityTemplate;
import de.citec.sc.templates.EditDistanceTemplate;
import de.citec.sc.templates.PageRankTemplate;
import de.citec.sc.templates.TermFrequencyTemplate;
import de.citec.sc.templates.TopicSpecificPageRankTemplate;
import templates.AbstractTemplate;

public class BIRESettings {

	/**
	 * All possible templates in order of their importance.
	 */
	final private static List<Class<? extends AbstractTemplate>> templates = Arrays.asList(PageRankTemplate.class,
			TermFrequencyTemplate.class, EditDistanceTemplate.class, TopicSpecificPageRankTemplate.class,
			DocumentSimilarityTemplate.class);

	/**
	 * All possible options generated given the sorted templates.
	 */
	final private static List<Setting> settings = new ArrayList<>();

	static {

		/*
		 * Add single template setting
		 */

		for (int numOfFixedTempaltes = 0; numOfFixedTempaltes < templates.size(); numOfFixedTempaltes++) {

			for (Class<? extends AbstractTemplate> template : templates) {
				Setting setting = new Setting(getTempaltes(numOfFixedTempaltes));
				if (setting.orderedOption.contains(template)) {
					continue;
				}
				setting.addTemaplte(template);
				settings.add(setting);
			}
		}

	}

	private static List<Class<? extends AbstractTemplate>> getTempaltes(int numOfFixedTempaltes) {
		return templates.subList(0, numOfFixedTempaltes);
	}

	public static int size() {
		return settings.size();
	}

	public static Setting getSetting(final int index) {
		return settings.get(index);
	}

	public static void main(String[] args) {
		settings.forEach(System.out::println);
	}

}
