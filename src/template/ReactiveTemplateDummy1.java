package template;

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

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

public class ReactiveTemplateDummy1 implements ReactiveBehavior {

	private Random random;
	private double pPickup;
	private int numActions;
	private Agent myAgent;
	private final String topology = "France";
	private double discount;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		this.discount = agent.readProperty("discount-factor", Double.class, 0.95);

		this.random = new Random();
		this.pPickup = discount;
		this.numActions = 0;
		this.myAgent = agent;
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

		if (availableTask == null || random.nextDouble() > pPickup) {
			City currentCity = vehicle.getCurrentCity();
			action = new Move(currentCity.randomNeighbor(random));
		} else {
			action = new Pickup(availableTask);
		}
		
		if (numActions >= 1) {
			System.out.println("The total profit after " + numActions + " actions is "
					+ myAgent.getTotalReward() + " (average profit: "
					+ (myAgent.getTotalReward()/myAgent.getTotalDistance()) + ")");

//			writeDataToCSV("/home/iuliana/Devel/IntelligentAgents/Reactive-agents-transportation/ReactivePlots/reactiveAg"
//					+ this.myAgent.id() + this.topology + "Dummy1" + this.discount + ".csv", numActions, myAgent.getTotalReward(),
//					(myAgent.getTotalReward()/myAgent.getTotalDistance()));
		}
		numActions++;
		
		return action;
	}
}
