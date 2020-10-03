package template;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Random;

import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;


public class ReactiveTemplate implements ReactiveBehavior {

	private Random random;
	private double discount;
	private int numActions;
	private Agent myAgent;
	// Chanage the value here.
	private static final double INIT_VALUE = -999999;

	HashMap<StatePair,Double> probTransitionTable = new HashMap<StatePair,Double>();
	HashMap<StateCityPair,Double> rewardTable = new HashMap<StateCityPair,Double>();
	HashMap<StateAction, Double>  stateValues = new HashMap<StateAction, Double>();
	HashMap<StateAction, City>  bestAction = new HashMap<StateAction, City>();
	HashMap<StateCityPair, Double>  stateActionQvalues = new HashMap<StateCityPair, Double>();
	List<StateAction> allStates = new ArrayList<StateAction>();

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);


		this.random = new Random();
		this.discount = discount;
		this.numActions = 0;
		this.myAgent = agent;

		//Populate the tables for he reinforcement learning algorithm
		//Reads all cities from topology.
		List<City> cities = topology.cities();
		double reward = 0;

		for(City currentCity: cities) {
			//Case 1: no task
			List<City> currentCityNeighbours = currentCity.neighbors();
			StateAction noTaskCurrentState = new StateAction(currentCity, null);
			allStates.add(noTaskCurrentState);
			System.out.println(noTaskCurrentState);
			stateValues.put(noTaskCurrentState, INIT_VALUE);

			for(City neighbour: currentCityNeighbours) {
				reward = 0 -currentCity.distanceTo(neighbour)*agent.vehicles().get(0).costPerKm() ;
				StateCityPair neighbourStateNoTaskStateCityPair = new StateCityPair(noTaskCurrentState, neighbour);
				rewardTable.put(neighbourStateNoTaskStateCityPair, reward);

				StateAction neighbourStateNoTask = new StateAction(neighbour, null);
				StatePair neighbourStateNoTaskStatePair = new StatePair(noTaskCurrentState, neighbourStateNoTask);
				// No task at neighbour city.
				probTransitionTable.put(neighbourStateNoTaskStatePair, td.probability(neighbour, neighbour));
				stateValues.put(neighbourStateNoTask, INIT_VALUE);

				for (City taskAtDestinationCity: cities) {
					StateAction nextState = new StateAction(neighbour, taskAtDestinationCity);
					StatePair StatePair = new StatePair(noTaskCurrentState, nextState);
					probTransitionTable.put(StatePair, td.probability(neighbour, taskAtDestinationCity));
					stateValues.put(nextState, INIT_VALUE);

				}
			}
			// Case 2: There is a task
			for(City taskCity: cities) {
				StateAction currentState = new StateAction(currentCity, taskCity);

				// Case 2a: refuse task.
				allStates.add(currentState);
				for(City neighbour: currentCityNeighbours) {
					if (!(neighbour.equals(taskCity))){
						StateAction neighbourStateNoTask = new StateAction(neighbour, null);
						StatePair neighbourStateNoTaskStatePair = new StatePair(currentState, neighbourStateNoTask);
						// No task at neighbour city.
						probTransitionTable.put(neighbourStateNoTaskStatePair, td.probability(neighbour, null));
						stateValues.put(neighbourStateNoTask, INIT_VALUE);

						reward = 0 -currentCity.distanceTo(neighbour)*agent.vehicles().get(0).costPerKm() ;
						StateCityPair neighbourStateNoTaskStateCityPair = new StateCityPair(currentState, neighbour);
						rewardTable.put(neighbourStateNoTaskStateCityPair, reward);

						for (City taskAtDestinationCity: cities) {
							StateAction nextState = new StateAction(neighbour, taskAtDestinationCity);
							StatePair StatePair = new StatePair(currentState, nextState);
							probTransitionTable.put(StatePair, td.probability(neighbour, taskAtDestinationCity));
							stateValues.put(nextState, INIT_VALUE);
						}}
				}

				// Case 2b: accept task
				reward = td.reward(currentCity, taskCity)  -currentCity.distanceTo(taskCity)*agent.vehicles().get(0).costPerKm() ;
				StateCityPair taskStateStateCityPair = new StateCityPair(currentState, taskCity);
				rewardTable.put(taskStateStateCityPair, reward);

				for (City taskAtDestinationCity: cities) {
					StateAction nextState = new StateAction(taskCity, taskAtDestinationCity);
					StatePair StatePair = new StatePair(currentState, nextState);
					probTransitionTable.put(StatePair, td.probability(taskCity, taskAtDestinationCity));
					allStates.add(nextState);
					stateValues.put(nextState, INIT_VALUE);
				}

			}

		}
		//		double cost = cityA.distanceTo(cityB)*agent.vehicles().get(0).costPerKm();
		boolean keepLooping;
		int steps = 0;
		double eps = 0.0;
		double maxE = 0;
		do {
			maxE = 0;
			steps++;
			System.out.println(steps);
			keepLooping = false;
			double currentQValue = 0;
			for (StateAction state : allStates) {
				City currentCity = state.getCurrentCity();
				City taskCity = state.getTaskCity();
				double maxQValue = -9999;
				City bestActionCurrentValue = taskCity;

				//Accept the task
				if (taskCity != null){
					StateCityPair currentStateTaskCityPair = new StateCityPair(state, taskCity);
					currentQValue = rewardTable.get(currentStateTaskCityPair);
					for (City destinationCityForTaskCity : cities) {
						StateAction destinationState = new StateAction(taskCity, destinationCityForTaskCity);
						StatePair currentStatePair = new StatePair(state, destinationState);
						currentQValue += discount * probTransitionTable.get(currentStatePair) * stateValues.get(destinationState);
					}
					stateActionQvalues.put(currentStateTaskCityPair, currentQValue);
				}
				if (currentQValue> maxQValue) {
					maxQValue= currentQValue;
					bestActionCurrentValue = taskCity;}


				// Reject task or null
				List<City> currentCityNeighbours = currentCity.neighbors();
				for (City neighbour : currentCityNeighbours) {
					if (!(neighbour.equals(taskCity))  ){
						StateCityPair currentStateNeighbourCityPair = new StateCityPair(state, neighbour);
						currentQValue = rewardTable.get(currentStateNeighbourCityPair);
						for (City destinationCityForNeighbourCity : cities) {
							StateAction destinationState = new StateAction(neighbour, destinationCityForNeighbourCity);
							StatePair currentStatePair = new StatePair(state, destinationState);
							currentQValue += discount * probTransitionTable.get(currentStatePair) * stateValues.get(destinationState);
							if (currentQValue> maxQValue) {
								maxQValue= currentQValue;
								bestActionCurrentValue = neighbour;}
						}
						stateActionQvalues.put(currentStateNeighbourCityPair, currentQValue);
				}}
				if (stateValues.get(state) != 0 && (maxQValue>stateValues.get(state)) ) eps = Math.abs((maxQValue-stateValues.get(state))/stateValues.get(state));
				else eps =0;

				double shit = stateValues.get(state);

				if (eps > maxE) maxE = eps;

				if (eps > 0.001) keepLooping = true;
				stateValues.put(state,maxQValue);
				bestAction.put(state,bestActionCurrentValue);
//				if (eps >0.1) System.out.println(state+ "  maxQValue " + maxQValue + "   shit:  " + shit );
				//System.out.println("Steps: "+ steps + "      eps:" + eps + "   " + keepLooping + "  maxQValue " + maxQValue + "   shit:  " + shit);
			}
//			System.out.println("Steps: "+ steps + "      maxeps:" + maxE);
		}while (keepLooping);
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;
		City currentCity = vehicle.getCurrentCity();
		City bestCity;
		if (availableTask != null) bestCity = bestAction.get(new StateAction(currentCity, availableTask.deliveryCity));
		else bestCity = bestAction.get(new StateAction(currentCity, null));

		if (availableTask == null) {
			action = new Move(bestCity);
		} else {
			if (bestCity.equals(availableTask.deliveryCity)) action = new Pickup(availableTask);
			else action = new Move(bestCity);

		}
		
		if (numActions >= 1) {
			System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;
		
		return action;
	}
}
