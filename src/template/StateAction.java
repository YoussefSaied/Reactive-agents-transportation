package template;

import logist.topology.Topology.City;


public class StateAction {

    private City currentCity;
    private City taskCity;

    public StateAction(City givenCurrentCity, City taskCity) {
        this.currentCity = givenCurrentCity;
        this.taskCity = taskCity;
    }

    public City getCurrentCity() {
        return currentCity;
    }

    public void setCurrentCity(City currentCity) {
        this.currentCity = currentCity;
    }

    public City getTaskCity() {
        return taskCity;
    }

    public void setTaskCity(City taskCity) {
        this.taskCity = taskCity;
    }

    @Override
    public String toString() {
        if (taskCity != null) return "Current city:    " + Integer.toString(currentCity.id) + "      task city: " + Integer.toString(taskCity.id);
        return "Current city:    " + Integer.toString(currentCity.id);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((currentCity == null) ? 0 : currentCity.hashCode());
        result = prime * result + ((taskCity == null) ? 0 : taskCity.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;

//        if (obj == null) return false;

        if (getClass() != obj.getClass()) return false;

        StateAction other = (StateAction) obj;
        if (currentCity == null) {
            if (other.currentCity != null) {
                return false;
            }
        } else if (currentCity.id != other.currentCity.id) // (!currentCity.equals(other.currentCity))
            return false;

        if (taskCity == null) {
            if (other.taskCity != null) {
                return false;
            }
        } else if (taskCity.id != other.taskCity.id) // (!taskCity.equals(other.taskCity))
            return false;
        return true;
    }

}
