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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.citec.sc.corpus.Annotation;
import de.citec.sc.corpus.Document;
import de.citec.sc.variables.State;
import factors.Factor;
import factors.patterns.VariablePairPattern;
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
public class TopicSpecificPageRankTemplate
		extends templates.AbstractTemplate<Document, State, VariablePairPattern<Annotation>> {

	/*
	 * Handmade
	 */
	final private static int NUMBER_OF_BINS = 1000;

	private static double[] bins = new double[NUMBER_OF_BINS + 1];

	static {

		for (int i = 0; i <= NUMBER_OF_BINS; i++) {
			bins[i] = (double) i / (double) NUMBER_OF_BINS;
		}

	}

	private static final int NUM_OF_GOLD_INDICIES = 1700000;

	private static Logger log = LogManager.getFormatterLogger();

	private static Map<Integer, Map<Integer, Double>> tspr = new HashMap<>();

	private static boolean isInitialized = false;

	private boolean useBins = false;

	public static boolean isInitialized() {
		return isInitialized;
	}

	public static void init(final String keyFiles, final String pageRankFile) throws IOException {

		if (!isInitialized) {
			final Set<Integer> indicies;
			log.info("Load topic specific page rank file...");
			indicies = loadTopicSpecificPageRanks(pageRankFile);
			log.info("Done, loading topic specific page rank index mapping file");

			log.info("Load topic specific page rank index mapping file...");
			// loadIndexMapping(keyFiles, indicies);
			log.info("Done, loading topic specific page rank index mapping file");
			isInitialized = true;
		}

	}

	public TopicSpecificPageRankTemplate(boolean b) throws InitializationException {
		if (!isInitialized) {
			log.warn("TopicSpecificPageRankTemplate is NOT initialized correctly!");
			log.warn("Call TopicSpecificPageRankTemplate.init() for proper initlialization.");
			throw new InitializationException(
					"TopicSpecificPageRankTemplate is NOT initialized correctly! Call TopicSpecificPageRankTemplate.init() for proper initlialization.");
		}

		this.useBins = b;
	}

	private static Set<Integer> loadTopicSpecificPageRanks(String pageRankFile)
			throws NumberFormatException, IOException {
		final Set<Integer> goldIndicies = new HashSet<>();

		BufferedReader topicSpecificPageRankReader = new BufferedReader(new FileReader(new File(pageRankFile)));
		String line = topicSpecificPageRankReader.readLine();
		log.debug("Topic specific pagerank file format = " + line);
		while ((line = topicSpecificPageRankReader.readLine()) != null) {
			String[] allDataPoints = line.split("\t");
			final int startNode = Integer.parseInt(allDataPoints[0]);
			tspr.put(startNode, new HashMap<>());
			goldIndicies.add(startNode);
			for (int dataPointIndex = 1; dataPointIndex < allDataPoints.length; dataPointIndex++) {

				final String[] data = allDataPoints[dataPointIndex].split(":");

				final int node = Integer.parseInt(data[0]);
				final double value = Double.parseDouble(data[1]);
				tspr.get(startNode).put(node, value);
				goldIndicies.add(node);
			}
		}
		topicSpecificPageRankReader.close();
		return goldIndicies;
	}

	// private static void loadIndexMapping(final String keyFiles, Set<Integer>
	// indicies)
	// throws FileNotFoundException, IOException {
	// BufferedReader indexMappingReader = new BufferedReader(new FileReader(new
	// File(keyFiles)));
	// String line = "";
	// while ((line = indexMappingReader.readLine()) != null) {
	// String[] data = line.split("\t");
	// final int nodeIndex = Integer.parseInt(data[0]);
	// if (indicies.contains(nodeIndex))
	// indexMappings.put(data[1], nodeIndex);
	// }
	// indexMappingReader.close();
	// }
	@Override
	public Set<VariablePairPattern<Annotation>> generateFactorPatterns(State state) {
		Set<VariablePairPattern<Annotation>> factors = new HashSet<>();
		for (Annotation firstAnnotation : state.getEntities()) {
			for (Annotation secondAnnotation : state.getEntities()) {
				if (!firstAnnotation.equals(secondAnnotation)) {
					factors.add(new VariablePairPattern<>(this, firstAnnotation, secondAnnotation));
				}
			}
		}

		log.info("Generate %s factor patterns for state %s.", factors.size(), state.getID());
		return factors;
	}

	@Override
	public void computeFactor(Document instance, Factor<VariablePairPattern<Annotation>> factor) {
		Annotation firstAnnotation = factor.getFactorPattern().getVariable1();
		Annotation secondAnnotation = factor.getFactorPattern().getVariable2();
		log.debug("Compute %s factor for variables %s and %s", TopicSpecificPageRankTemplate.class.getSimpleName(),
				firstAnnotation, secondAnnotation);

		Vector featureVector = factor.getFeatureVector();

		double score = 0;

		final String link = firstAnnotation.getLink().trim();
		final String link2 = secondAnnotation.getLink().trim();
		calcScore: {

			if (link.equals(Annotation.DEFAULT_ID)) {
				break calcScore;
			}

			if (!IndexMapping.indexMappings.containsKey(link)) {
				// log.warn("Unknown link node detected for link: " + link);
				break calcScore;
			}

			/*
			 * If node is known
			 */
			final int linkNodeIndex = IndexMapping.indexMappings.get(link);

			if (!tspr.containsKey(linkNodeIndex)) {
				break calcScore;
			}

			if (link2.equals(Annotation.DEFAULT_ID)) {
				break calcScore;
			}

			if (!IndexMapping.indexMappings.containsKey(link2)) {
				// log.warn("Unknown link node detected for link: " +
				// link2);
				break calcScore;
			}

			/*
			 * If other link is known
			 */
			final int linkNodeIndex2 = IndexMapping.indexMappings.get(link2);

			if (!tspr.get(linkNodeIndex).containsKey(linkNodeIndex2)) {
				break calcScore;
			}

			/*
			 * If tspr of startnode contains node
			 */
			score += tspr.get(linkNodeIndex).get(linkNodeIndex2);

			/*
			 * Normalize by number of additions.
			 */
			final int bin = getBin(score);

			featureVector.set("TopicSpecificPageRank", score);

			if (useBins) {
				for (int i = 0; i < bin; i++) {
					featureVector.set("TopicSpecificPageRank >= " + i, score);
				}

				featureVector.set("1TopicSpecificPageRankInBin_" + bin, 1d);
				featureVector.set("ScoreTopicSpecificPageRankInBin_" + bin, score);
			}

		}

	}

	private int getBin(final double score) {
		for (int i = 0; i < bins.length - 1; i++) {
			if (bins[i] <= score && score < bins[i + 1]) {
				return i;
			}
		}
		return -1;
	}

	// @Override
	// protected void computeFactor(State state, AbstractFactor absFactor) {
	// if (absFactor instanceof UnorderedVariablesFactor) {
	// UnorderedVariablesFactor factor = (UnorderedVariablesFactor) absFactor;
	// log.debug("Compute Topic Specific Page Rank factor for state %s",
	// state.getID());
	// Vector featureVector = new Vector();
	//
	// double score = 0;
	//
	// /*
	// * For all annotations
	// */
	// for (Annotation annotation : state.getEntities()) {
	//
	// final String link = annotation.getLink().trim();
	//
	// if (link.isEmpty())
	// continue;
	//
	// if (!indexMappings.containsKey(link)) {
	// // log.warn("Unknown link node detected for link: " + link);
	// continue;
	// }
	//
	// /*
	// * If node is known
	// */
	// final int linkNodeIndex = indexMappings.get(link);
	//
	// if (!tspr.containsKey(linkNodeIndex))
	// continue;
	//
	// /*
	// * For all other annotations
	// */
	// for (Annotation annotation2 : state.getEntities()) {
	//
	// if (annotation.equals(annotation2))
	// continue;
	//
	// final String link2 = annotation2.getLink().trim();
	//
	// if (link2.isEmpty())
	// continue;
	//
	// if (!indexMappings.containsKey(link2)) {
	// // log.warn("Unknown link node detected for link: " +
	// // link2);
	// continue;
	// }
	//
	// /*
	// * If other link is known
	// */
	// final int linkNodeIndex2 = indexMappings.get(link2);
	//
	// if (!tspr.get(linkNodeIndex).containsKey(linkNodeIndex2))
	// continue;
	//
	// /*
	// * If tspr of startnode contains node
	// */
	// score += tspr.get(linkNodeIndex).get(linkNodeIndex2);
	// // System.out.println("link = " + link);
	// // System.out.println("link2 = " + link2);
	// // System.out.println("tspr = " +
	// // tspr.get(linkNodeIndex).get(linkNodeIndex2));
	// }
	// }
	//
	// // state.getEntities().forEach(System.out::println);
	// // System.out.println("Score = " + score);
	// //
	// // System.out.println("===============");
	// // System.out.println();
	// // System.out.println();
	// /*
	// * Normalize by number of additions.
	// */
	// score /= (state.getEntities().size() * (state.getEntities().size() - 1));
	//
	// featureVector.set("TopicSpecificPageRank", score);
	//
	// factor.setFeatures(featureVector);
	// }
	// }
}

// 13:43:21.863 [main] INFO - Micro-average Precision=0.6225
// 13:43:21.863 [main] INFO - Micro-average Recall=0.6125
// 13:43:21.863 [main] INFO - F1 Micro-average=0.6174999999999999
// 13:43:21.863 [main] INFO - Macro-average Precision=0.6214999999999999
// 13:43:21.863 [main] INFO - Macro-average Recall=0.6145
// 13:43:21.863 [main] INFO - F1 Macro-average=0.618
