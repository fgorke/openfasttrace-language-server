# Coffee Maker — System Requirements

Demo specification for the OpenFastTrace Language Server walkthrough.
Some defects are intentional.

## Features

### Brew Coffee
`feat~brew-coffee~1`

The machine brews a pot of filter coffee at the push of a button.

Needs: req

## Requirements

### Start Brewing
`req~start-brewing~1`

Pressing the start button runs a brew cycle: grind the beans, heat the water,
run the water through the filter.

Covers:
* `feat~brew-coffee~1`

Needs: dsn

### Water Temperature
`req~water-temperature~2`

The water temperature during brewing stays between 92 and 96 °C.

Covers:
* `feat~brew-coffee~1`

Needs: dsn

### Fill Level Warning
`req~fill-level-warning~1`

The machine warns when the water tank is nearly empty.

Covers:
* `feat~brew-coffee~1`

### Fill Level Warning
`req~fill-level-warning~1`

Comment: Intentional duplicate definition of the item above.

Covers:
* `feat~brew-coffee~1`

### Cleaning Reminder
`req~cleaning-reminder~1`

After 30 brew cycles the machine reminds the user to descale it.

Covers:
* `feat~brew-coffee~1`

Needs: uman
