public class CoffeeMaker {

    // [impl->dsn~brew-cycle~1]
    public void runBrewCycle() {
        grindBeans();
        heatWater();
    }

    // A tag may carry its own name and revision:
    // [impl~grinder-motor~1->dsn~grind-beans~1]
    private void grindBeans() {
    }

    // [impl->dsn~heat-water~2]
    private void heatWater() {
    }

    // Intentional defect: this tag references revision 1, the design item is at revision 2.
    // [impl->dsn~heat-water~1]
    private void keepWarm() {
    }

    // Intentional defect: this tag points at an item that does not exist.
    // [impl->dsn~milk-frother~1]
    private void frothMilk() {
    }

    // Completion exercise area, type new coverage tags below this line:

}
