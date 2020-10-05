package template;


public class StatePair {

    private final State state1;
    private final State state2;

    public StatePair(State state1, State state2) {
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
    public String toString()  {

        return "First state:   " + state1.toString() + "   second state:  "+ state2.toString() ;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StatePair)) return false;
        StatePair key = (StatePair) o;
        return state1.equals(key.state1) && state2.equals(key.state2);

//        boolean cityBool = false;
//        if (key.city == null){
//            if (city != null) return false;
//            else {
//                cityBool = true;
//            }
//        }else {
//            cityBool = city.equals(key.city);
//        }
//        boolean stateBool = state.equals(key.state);
//
//        return stateBool && cityBool;
    }
}
