package com.smart.evie;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

import android.util.Log;

import com.main.evie.DynamicEventList;
import com.main.evie.Event;


public class UserPreference {
	private static final int RECOMMENDATIONS = 10;
	private static final int RECENT_WEIGHT = 3;
	private static final double DECAY = 0.8;

	private double means[];
	private int meanNumber;
	private BagOfWords bag;
	private VectorUtil vectorUtil;
	private DynamicEventList events;

	public UserPreference() {
		this.vectorUtil = new VectorUtil();
		this.events = new DynamicEventList();

		this.means = null;
		this.meanNumber = 0;
	}

	public ArrayList<Event> addWords(String words) {
		this.bag = new BagOfWords();
		ArrayList<double[]> allPollResults = this.bag.pollWords(this.events.getAllEvents());
		double poll[] = this.bag.poll(words);

		Log.i("evie_debug", "size of poll results is " + allPollResults.size());
		updateNewMeans(poll);

		PriorityQueue<Tuple> queue = new PriorityQueue<Tuple>
						(poll.length == 0? 10: poll.length, new TupleComparator());
		int index = 0;
		for (double[] pastPoll: allPollResults) {
			double similarity = this.vectorUtil.euclideanDistance(this.means, pastPoll);
			queue.add(new Tuple(similarity, index));
			index++;
		}
		
		ArrayList<Event> topRecommendations = new ArrayList<Event>();

		for (int i = 0; i < RECOMMENDATIONS; i++ ) {
			if (queue.isEmpty()) {
				Log.i("evie_debug", "queue is empty");
				break;
			}
			
			Tuple current = queue.remove();
			Log.i("evie_debug", "found tuple! index " + current.index + " similarity " + current.similarity);
			topRecommendations.add(this.events.getAllEvents().get(current.index));
		}
		Log.i("evie_debug", "start printing events");
		for (Event event: topRecommendations) {
			Log.i("evie_debug", "event " + event.getName());
		}
		Log.i("evie_debug", "finish printing events");
		return topRecommendations;
	}
	
	private void updateNewMeans(double[] newPoll) {
		double[] resultingMean;
		if (this.means == null) {
			this.means = new double[newPoll.length];
		}
		if (meanNumber != 0) {
			resultingMean = this.vectorUtil.multiplyVector(this.means, meanNumber*DECAY);
		}
		
		resultingMean = this.vectorUtil.sumVectors(this.means, newPoll);
		this.meanNumber++;
		resultingMean = this.vectorUtil.divideVector(resultingMean, this.meanNumber);
		this.means = resultingMean;
	}
	
	private static class TupleComparator implements Comparator<Tuple> {

		@Override
		public int compare(Tuple a, Tuple b) {
			return Double.compare(a.similarity, b.similarity);
		}
		
	}

	private static class Tuple {
		private final double similarity;
		private final int index; 

		public Tuple(double similarity, int index) {
			this.similarity = similarity;
			this.index = index;
		}
	}
}