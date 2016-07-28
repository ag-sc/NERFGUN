package de.citec.sc.learning;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import corpus.Instance;
import learning.Trainer;
import learning.callbacks.InstanceCallback;
import sampling.DefaultSampler;
import sampling.samplingstrategies.AcceptStrategies;
import sampling.samplingstrategies.SamplingStrategies;
import variables.AbstractState;

public class ChangeSamplingStrategy implements InstanceCallback {

	private static Logger log = LogManager.getFormatterLogger();
	private DefaultSampler<?, ?, ?> sampler;

	public ChangeSamplingStrategy(DefaultSampler<?, ?, ?> sampler) {
		this.sampler = sampler;
	}

	@Override
	public <InstanceT extends Instance> void onStartInstance(Trainer caller, InstanceT document, int indexOfDocument,
			int numberOfDocuments, int epoch, int numberOfEpochs) {

		double ratio = ((double) (indexOfDocument + epoch * numberOfDocuments)) / (numberOfDocuments * numberOfEpochs);

		log.info("%.2f%% of total instances processed.", ratio * 100);
		if (Math.random() > ratio) {
			log.info("Set SamplingStrategy and AcceptStrategy to: GREEDY OBJECTIVE");
			sampler.setTrainingSamplingStrategy(SamplingStrategies.greedyObjectiveStrategy());
			sampler.setTrainingAcceptStrategy(AcceptStrategies.strictObjectiveAccept());
		} else {
			log.info("Set SamplingStrategy and AcceptStrategy to: SAMPLED MODEL");
			sampler.setTrainingSamplingStrategy(SamplingStrategies.linearModelSamplingStrategy());
			sampler.setTrainingAcceptStrategy(AcceptStrategies.strictModelAccept());

		}
	}

	@Override
	public <InstanceT extends Instance, StateT extends AbstractState<? super InstanceT>> void onEndInstance(
			Trainer caller, InstanceT document, int indexOfDocument, StateT finalState, int numberOfDocuments,
			int epoch, int numberOfEpochs) {

	}

}
