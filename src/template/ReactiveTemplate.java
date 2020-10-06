package template;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;


public class ReactiveTemplate implements ReactiveBehavior {

	private int numActions;
	private Agent myAgent;
	private double discount;
	private final String topology = "England";
	private static final double INIT_VALUE = -999999;

	// Store the required tables as hash maps for efficiency.
	HashMap<StatePair,Double> probTransitionTable = new HashMap<StatePair,Double>();
	HashMap<StateCityPair,Double> rewardTable = new HashMap<StateCityPair,Double>();
	HashMap<State,Double> stateValues = new HashMap<State,Double>();
	HashMap<State,City> bestAction = new HashMap<State,City>();
	HashMap<StateCityPair,Double> stateActionQvalues = new HashMap<StateCityPair,Double>();

	// List that will contain all the states.
	List<State> allStates = new ArrayList<State>();

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		this.numActions = 0;
		this.myAgent = agent;

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		this.discount = agent.readProperty("discount-factor", Double.class, 0.95);
		double reward = 0;

		//Read all cities from topology.
		List<City> cities = topology.cities();

		//Populate the tables for he reinforcement learning algorithm.
		for (City currentCity: cities) {

			// Case 1: no task at the current city.
			List<City> currentCityNeighbours = currentCity.neighbors();
			State noTaskCurrentState = new State(currentCity, null);
			allStates.add(noTaskCurrentState);
			stateValues.put(noTaskCurrentState, INIT_VALUE);

			for (City neighbour: currentCityNeighbours) {
				reward = 0 - currentCity.distanceTo(neighbour) * agent.vehicles().get(0).costPerKm();
				StateCityPair neighbourStateNoTaskStateCityPair = new StateCityPair(noTaskCurrentState, neighbour);
				rewardTable.put(neighbourStateNoTaskStateCityPair, reward);

				State neighbourStateNoTask = new State(neighbour, null);
				StatePair neighbourStateNoTaskStatePair = new StatePair(noTaskCurrentState, neighbourStateNoTask);

				// No task at neighbouring city.
				probTransitionTable.put(neighbourStateNoTaskStatePair, td.probability(neighbour, neighbour));
				stateValues.put(neighbourStateNoTask, INIT_VALUE);

				for (City taskAtDestinationCity: cities) {
					State nextState = new State(neighbour, taskAtDestinationCity);
					StatePair StatePair = new StatePair(noTaskCurrentState, nextState);
					probTransitionTable.put(StatePair, td.probability(neighbour, taskAtDestinationCity));
					stateValues.put(nextState, INIT_VALUE);

				}
			}

			// Case 2: There is a task at the current city.
			for (City taskCity: cities) {
				State currentState = new State(currentCity, taskCity);

				// Case 2a: refuse the task.
				allStates.add(currentState);
				for (City neighbour: currentCityNeighbours) {
					if (!(neighbour.equals(taskCity))){
						State neighbourStateNoTask = new State(neighbour, null);
						StatePair neighbourStateNoTaskStatePair = new StatePair(currentState, neighbourStateNoTask);
						// No task at neighbour city.
						probTransitionTable.put(neighbourStateNoTaskStatePair, td.probability(neighbour, null));
						stateValues.put(neighbourStateNoTask, INIT_VALUE);

						reward = 0 -currentCity.distanceTo(neighbour) * agent.vehicles().get(0).costPerKm();
						StateCityPair neighbourStateNoTaskStateCityPair = new StateCityPair(currentState, neighbour);
						rewardTable.put(neighbourStateNoTaskStateCityPair, reward);

						for (City taskAtDestinationCity: cities) {
							State nextState = new State(neighbour, taskAtDestinationCity);
							StatePair StatePair = new StatePair(currentState, nextState);
							probTransitionTable.put(StatePair, td.probability(neighbour, taskAtDestinationCity));
							stateValues.put(nextState, INIT_VALUE);
						}}
				}

				// Case 2b: accept the task.
				reward = td.reward(currentCity, taskCity) - currentCity.distanceTo(taskCity) * agent.vehicles().get(0).costPerKm();
				StateCityPair taskStateStateCityPair = new StateCityPair(currentState, taskCity);
				rewardTable.put(taskStateStateCityPair, reward);

				for (City taskAtDestinationCity: cities) {
					State nextState = new State(taskCity, taskAtDestinationCity);
					StatePair StatePair = new StatePair(currentState, nextState);
					probTransitionTable.put(StatePair, td.probability(taskCity, taskAtDestinationCity));
					allStates.add(nextState);
					stateValues.put(nextState, INIT_VALUE);
				}

			}

		}

		// Offline reinforcement learning algorithm that populates the required tables.
		boolean keepLooping;
		double epsilon;
		double maxEpsilon;

		do {
			maxEpsilon = 0;
			keepLooping = false;

			double currentQValue;

			for (State state: allStates) {
				City currentCity = state.getCurrentCity();
				City taskCity = state.getTaskCity();
				currentQValue = -9999999;
				double maxQValue = -999999;
				City bestActionCurrentValue = taskCity;

				// Reject task or null.
				List<City> currentCityNeighbours = currentCity.neighbors();
				for (City neighbour: currentCityNeighbours) {
					if (!(neighbour.equals(taskCity))) {
						StateCityPair currentStateNeighbourCityPair = new StateCityPair(state, neighbour);
						currentQValue = rewardTable.get(currentStateNeighbourCityPair);

						for (City destinationCityForNeighbourCity : cities) {
							State destinationState = new State(neighbour, destinationCityForNeighbourCity);
							StatePair currentStatePair = new StatePair(state, destinationState);
							currentQValue += discount * probTransitionTable.get(currentStatePair) * stateValues.get(destinationState);

							if (currentQValue > maxQValue) {
								maxQValue = currentQValue;
								bestActionCurrentValue = neighbour;
							}
						}
						stateActionQvalues.put(currentStateNeighbourCityPair, currentQValue);
					}
				}


				// Accept the task.
				if (taskCity != null) {
					StateCityPair currentStateTaskCityPair = new StateCityPair(state, taskCity);
					currentQValue = rewardTable.get(currentStateTaskCityPair);
					for (City destinationCityForTaskCity : cities) {
						State destinationState = new State(taskCity, destinationCityForTaskCity);
						StatePair currentStatePair = new StatePair(state, destinationState);
						currentQValue += discount * probTransitionTable.get(currentStatePair) * stateValues.get(destinationState);
					}
					stateActionQvalues.put(currentStateTaskCityPair, currentQValue);
				}
				if (currentQValue > maxQValue) {
					maxQValue = currentQValue;
					bestActionCurrentValue = taskCity;
				}

				if (stateValues.get(state) != 0 && (maxQValue > stateValues.get(state))) {
					epsilon = Math.abs((maxQValue - stateValues.get(state)) / stateValues.get(state));
				} else {
					epsilon = 0;
				}

				if (epsilon > maxEpsilon) {
					maxEpsilon = epsilon;
				}

				if (epsilon > 0.001) {
					keepLooping = true;
				}
				stateValues.put(state, maxQValue);

				if (bestActionCurrentValue != null) {
					bestAction.put(state, bestActionCurrentValue);
				}
			}
		} while (keepLooping);
	}

	public void writeDataToCSV(String csvFile, int dataItem1, long dataItem2, double dataItem3) {
		FileWriter writer;

		try {
			writer = new FileWriter(csvFile, true);

			CSVWriter.writeLine(writer, Arrays.asList(
					Integer.toString(dataItem1),
					Long.toString(dataItem2),
					Double.toString(dataItem3)
					)
			);

			writer.flush();
			writer.close();

		} catch (IOException e2) {
			e2.printStackTrace();
		}
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {

		Action action;
		City currentCity = vehicle.getCurrentCity();
		City bestCity;
		State currentState;

		if (availableTask != null) currentState = new State(currentCity, availableTask.deliveryCity);
		else currentState = new State(currentCity, null);
		bestCity = bestAction.get(currentState);

		if (availableTask == null) {
			action = new Move(bestCity);
		} else {
			if (bestCity.equals(availableTask.deliveryCity)) action = new Pickup(availableTask);
			else action = new Move(bestCity);

		}
		
		if (numActions >= 1) {

			System.out.println("The total profit after " + numActions + " actions is "
								+ myAgent.getTotalReward() + " (average profit: "
								+ (myAgent.getTotalReward()/myAgent.getTotalDistance()) + ")");

//			writeDataToCSV("/home/iuliana/Devel/IntelligentAgents/Reactive-agents-transportation/ReactivePlots/reactiveAg"
//							+ this.myAgent.id() + this.topology + this.discount + ".csv",
//							numActions, myAgent.getTotalReward(), (myAgent.getTotalReward()/myAgent.getTotalDistance()));
		}
		numActions++;
		
		return action;
	}
}
