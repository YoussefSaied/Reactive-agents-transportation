package template;


public class StatePair {

    private final StateAction state1;
    private final StateAction state2;

    public StatePair(StateAction state1, StateAction state2) {
        this.state1 = state1;
        this.state2 = state2;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((state1 == null) ? 0 : state1.hashCode());
        result = prime * result + ((state2 == null) ? 0 : state2.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StatePair)) return false;
        StatePair key = (StatePair) o;
        return state1 == key.state1 && state2 == key.state2;
    }
}
