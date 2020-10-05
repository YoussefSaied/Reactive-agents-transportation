package template;

import logist.topology.Topology.City;


public class StateCityPair {

    private final State state;
    private final City city;

    public StateCityPair(State state, City city) {
        this.state = state;
        this.city = city;
    }
    @Override
    public String toString() {
        if (city == null) return state.toString();
        return state.toString() + "      pair city: " + city.id;
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((state == null) ? 0 : state.hashCode());
        result = prime * result + ((city == null) ? 0 : city.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StateCityPair)) return false;
        StateCityPair key = (StateCityPair) o;
        boolean cityBool = false;
        if (key.city == null){
            if (city != null) return false;
            else {
                cityBool = true;
            }
        }else {
            cityBool = city.equals(key.city);
        }
        boolean stateBool = state.equals(key.state);

        return stateBool && cityBool;
    }
}
