package de.citec.sc.settings;

import de.citec.sc.templates.CandidateSimilarityTemplate;
import de.citec.sc.templates.CategoryTemplate;
import de.citec.sc.templates.ClassContextTemplate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.citec.sc.templates.DocumentSimilarityTemplate;
import de.citec.sc.templates.EditDistanceTemplate;
import de.citec.sc.templates.LocalIDFDocumentSimilarityTemplate;
import de.citec.sc.templates.NameSurnameTemplate;
import de.citec.sc.templates.PageLinkEmbeddingTemplate;
import de.citec.sc.templates.PageRankTemplate;
import de.citec.sc.templates.PairwiseClassOccurenceTemplate;
import de.citec.sc.templates.TermFrequencyTemplate;
import de.citec.sc.templates.TopicSpecificPageRankTemplate;
import templates.AbstractTemplate;

public class BIRESettings {

	/**
	 * All possible templates in order of their importance.
	 */
	final private static List<Class<? extends AbstractTemplate>> templates = Arrays.asList(TopicSpecificPageRankTemplate.class, EditDistanceTemplate.class, 
			TermFrequencyTemplate.class,   NameSurnameTemplate.class, ClassContextTemplate.class, PageRankTemplate.class);

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
				Setting setting = new Setting(getTemplates(numOfFixedTempaltes));
				if (setting.orderedOption.contains(template)) {
					continue;
				}
				setting.addTemplate(template);
				settings.add(setting);
			}
		}

	}

	private static List<Class<? extends AbstractTemplate>> getTemplates(int numOfFixedTempaltes) {
		return templates.subList(0, numOfFixedTempaltes);
	}

	public static int size() {
		return settings.size();
	}

	public static Setting getSetting(final int index) {
		return settings.get(index);
	}

	public static void main(String[] args) {
            
            for(int i =0; i<settings.size(); i++){
                System.out.println(i + "\t" +settings.get(i));
            }
		
	}

}
