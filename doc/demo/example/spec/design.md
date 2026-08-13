# Coffee Maker — Design

### Brew Cycle
`dsn~brew-cycle~1`

The controller runs the brew cycle as a fixed sequence: grinder, heater, pump.

Covers:
* `req~start-brewing~1`

Needs: impl, utest

### Heat Water
`dsn~heat-water~2`

A control loop keeps the heater within the target temperature range.

Comment: This item is at revision 2, one coverage tag still references revision 1.

Covers:
* `req~water-temperature~2`

Needs: impl

### Grind Beans
`dsn~grind-beans~1`

The grinder runs for a fixed time per cup.

Covers:
* `req~start-brewing~1`

Needs: impl, utest

### Cup Counter (completion exercise)
`dsn~cup-counter~1`

The machine counts finished brew cycles. 

Comment: During the walkthrough you add the  missing `Covers:` entry right below this line using code completion.
