/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.sc.templates;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.variables.State;
import factors.AbstractFactor;
import factors.impl.UnorderedVariablesFactor;
import learning.Vector;

/**
 * 
 * Computes a score given all entities and their topic specific pageranks of
 * each other.
 * 
 * @author hterhors
 *
 *         Feb 18, 2016
 */
public class TopicSpecificPageRankTemplate extends templates.AbstractTemplate<State> {

	private static final int NUM_OF_GOLD_INDICIES = 1700000;

	private static Logger log = LogManager.getFormatterLogger();

	private static Map<String, Integer> indexMappings = new HashMap<>(NUM_OF_GOLD_INDICIES);
	private static Map<Integer, Map<Integer, Double>> tspr = new HashMap<>();

	private Set<Integer> indicies = new HashSet<>();

	public TopicSpecificPageRankTemplate(final String keyFiles, final String pageRankFile) throws IOException {

		log.info("Load topic specific page rank file...");
		loadTopicSpecificPageRanks(pageRankFile);
		log.info("Done, loading topic specific page rank index mapping file");

		log.info("Load topic specific page rank index mapping file...");
		loadIndexMapping(keyFiles);
		log.info("Done, loading topic specific page rank index mapping file");

	}

	private void loadTopicSpecificPageRanks(String pageRankFile) throws NumberFormatException, IOException {
		BufferedReader topicSpecificPageRankReader = new BufferedReader(new FileReader(new File(pageRankFile)));
		String line = topicSpecificPageRankReader.readLine();
		log.debug("Topic specific pagerank file format = " + line);
		while ((line = topicSpecificPageRankReader.readLine()) != null) {
			String[] allDataPoints = line.split("\t");
			final int startNode = Integer.parseInt(allDataPoints[0]);
			tspr.put(startNode, new HashMap<>());
			indicies.add(startNode);
			for (int dataPointIndex = 1; dataPointIndex < allDataPoints.length; dataPointIndex++) {

				final String[] data = allDataPoints[dataPointIndex].split(":");

				final int node = Integer.parseInt(data[0]);
				final double value = Double.parseDouble(data[1]);
				tspr.get(startNode).put(node, value);
				indicies.add(node);
			}
		}
		topicSpecificPageRankReader.close();
	}

	private void loadIndexMapping(final String keyFiles) throws FileNotFoundException, IOException {
		BufferedReader indexMappingReader = new BufferedReader(new FileReader(new File(keyFiles)));
		String line = "";
		while ((line = indexMappingReader.readLine()) != null) {
			String[] data = line.split("\t");
			final int nodeIndex = Integer.parseInt(data[0]);
			if (indicies.contains(nodeIndex))
				indexMappings.put(data[1], nodeIndex);
		}
		indexMappingReader.close();
	}

	@Override
	protected Collection<AbstractFactor> generateFactors(State state) {

		Set<AbstractFactor> factors = new HashSet<>();
		// for (VariableID entityID : state.getEntityIDs()) {
		// factors.add(new SingleVariableFactor(this, entityID));
		// }

		factors.add(new UnorderedVariablesFactor(this, state.getEntityIDs()));

		log.debug("Generate %s factors for state %s.", factors.size(), state.getID());
		return factors;
	}

	@Override
	protected void computeFactor(State state, AbstractFactor absFactor) {
		if (absFactor instanceof UnorderedVariablesFactor) {
			UnorderedVariablesFactor factor = (UnorderedVariablesFactor) absFactor;
			log.debug("Compute Topic Specific Page Rank factor for state %s", state.getID());
			Vector featureVector = new Vector();

			double score = 0;

			/*
			 * For all annotations
			 */
			for (Annotation annotation : state.getEntities()) {

				final String link = annotation.getLink().trim();

				if (link.isEmpty())
					continue;

				if (!indexMappings.containsKey(link)) {
					// log.warn("Unknown link node detected for link: " + link);
					continue;
				}

				/*
				 * If node is known
				 */
				final int linkNodeIndex = indexMappings.get(link);

				if (!tspr.containsKey(linkNodeIndex))
					continue;

				/*
				 * For all other annotations
				 */
				for (Annotation annotation2 : state.getEntities()) {

					if (annotation.equals(annotation2))
						continue;

					final String link2 = annotation.getLink().trim();

					if (link2.isEmpty())
						continue;

					if (!indexMappings.containsKey(link2)) {
						// log.warn("Unknown link node detected for link: " +
						// link2);
						continue;
					}

					/*
					 * If other link is known
					 */
					final int linkNodeIndex2 = indexMappings.get(link2);

					if (!tspr.get(linkNodeIndex).containsKey(linkNodeIndex2))
						continue;

					/*
					 * If tspr of startnode contains node
					 */
					score += tspr.get(linkNodeIndex).get(linkNodeIndex2);

				}
			}

			/*
			 * Normalize by number of additions.
			 */
			score /= (state.getEntities().size() * (state.getEntities().size() - 1));

			featureVector.set("TopicSpecificPageRank", score);

			factor.setFeatures(featureVector);
		}
	}

}
