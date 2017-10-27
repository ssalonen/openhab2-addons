# Modbus Binding

This is the binding to access Modbus TCP and serial slaves. RTU, ASCII and BIN variants of Serial Modbus are supported. Modbus TCP slaves are usually also called as Modbus TCP servers.

The binding can act as

* Modbus TCP Client (that is, as modbus master), querying data from Modbus TCP servers (that is, modbus slaves).
* Modbus serial master, querying data from modbus serial slaves

The Modbus binding polls the slave data with an configurable poll period. openHAB commands are translated to write requests.

## Main features

The binding polls (or *reads*) Modbus data using function codes (FC) FC01 (Read coils), FC02 (Read discrete inputs), FC03 (Read multiple holding registers) or FC04 (Read input registers). This polled data is converted to data suitable for use in openHAB. Functionality exists to interpret typical number formats (e.g. single precision float).

The binding can also *write* data to Modbus slaves using FC05 (Write single coil), FC06 (Write single holding register), FC15 (Write multiple coils) or FC16 (Write multiple holding registers).

## Caveats and limitations

Please note the following caveats or limitations

* the binding does *not* act as Modbus slave (e.g. as Modbus TCP server).
* the binding does *not* support Modbus RTU over Modbus TCP, also known as "Modbus over TCP/IP" or "Modbus over TCP" or "Modbus RTU/IP", although normal "Modbus TCP" is supported. However, there is a workaround: you can use a Virtual Serial Port Server, to emulate a COM Port and Bind it with OpenHab unsing Modbus Serial.


## Background material

Reader of the documentation should understand the basics of Modbus protocol. Good sources for further information:

* [Wikipedia article](https://en.wikipedia.org/wiki/Modbus): good read on modbus basics and addressing.
* [Simplymodbus.ca](http://www.simplymodbus.ca/): good reference as well as excellent tutorial like explanation of the protocol

Useful tools

* [binaryconvert.com](http://www.binaryconvert.com/): tool to convert numbers between different binary presentations
* [rapidscada.net Modbus parser](http://modbus.rapidscada.net/): tool to parse Modbus requests and responses. Useful for debugging purposes when you want to understand the message sent / received.

## Supported Things

This binding support 6 different things types

| Thing    | Type   | Description                                                                                                                                                                                                                               |
| -------- | ------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `tcp`    | Bridge | Modbus TCP server (Modbus TCP slave)                                                                                                                                                                                                      |
| `serial` | Bridge | Modbus serial slave                                                                                                                                                                                                                       |
| `poller` | Bridge | Thing taking care of polling the data from modbus slaves. One poller corresponds to single Modbus read request (FC01, FC02, FC03, or FC04). Is child of `tcp` or `serial`.                                                                |
| `data`   | Thing  | thing for converting polled data to meaningful numbers. Analogously, is responsible of converting openHAB commands to Modbus write requests. Is child of `poller` (read-only or read-write things) or `tcp`/`serial` (write-only things). |

Typically one defines either `tcp` or `serial` bridge, depending on the variant of Modbus slave.
For each Modbus read request, a `poller` is defined.
Finally, one ore more `data` things are introduced to extract relevant numbers from the raw Modbus data. For write-only communication, `data` things can be introduced directly as children of `tcp` or `serial` bridges.

## Binding configuration

Other than the things themselves, there is no binding configuration.

## Serial port configuration

If you can see issues related to opening the serial port, and you are using **non standard serial ports** (e.g. `/dev/ttyAMA0`) you might have to configure openHAB to detect and access the port correctly.

Refer to [Serial Port Configuration Notes](http://docs.openhab.org/addons/bindings/serial1/readme.html#port-configuration-notes) for more information.

Without correct configuration, the binding might not be able to open the serial port for communication, and you will see an error message in the logs.

## Thing Configuration

In the tables below the thing configuration parameters are grouped by thing type.

Things can be configured using Paper UI, or using a `.things` file. The configuration in this documentation explains the `.things` file, although you can find the same parameters from the Paper UI.

Note that parameter type is very critical when writing `.things` file yourself, since it affects how the parameter value is encoded in the text file.

Some examples:

* `parameter="value"` for `text` parameters
* `parameter=4` for `integer`
* `parameter=true` for `boolean`

Note the differences with quoting.

Required parameters *must* be specified in the `.things` file. When optional parameters are not specified, they default to the values shown in the table below.

### `tcp` thing

`tcp` is representing a particular Modbus TCP slave.

Basic parameters

| Parameter | Type    | Required | Default if omitted | Description                                                 |
| --------- | ------- | -------- | ------------------ | ----------------------------------------------------------- |
| `host`    | text    |          | `"localhost"`      | IP Address or hostname                                      |
| `port`    | integer |          | `502`              | Port number                                                 |
| `id`      | integer |          | `1`                | Slave id. Also known as station address or unit identifier. |

Advanced parameters

| Parameter                       | Required | Type    | Default if omitted | Description                                                                                                                                                        |
| ------------------------------- | -------- | ------- | ------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `timeBetweenTransactionsMillis` |          | integer | `60`               | How long to delay we must have at minimum between two consecutive MODBUS transactions. In milliseconds.                                                            |
| `timeBetweenReconnectMillis`    |          | integer | `0`                | How long to wait to before trying to establish a new connection after the previous one has been disconnected. In milliseconds.                                     |
| `connectMaxTries`               |          | integer | `1`                | How many times we try to establish the connection. Should be at least 1.                                                                                           |
| `reconnectAfterMillis`          |          | integer | `0`                | The connection is kept open at least the time specified here. Value of zero means that connection is disconnected after every MODBUS transaction. In milliseconds. |
| `connectTimeoutMillis`          |          | integer | `10000`            | The maximum time that is waited when establishing the connection. Value of zero means that system/OS default is respected. In milliseconds.                        |

**Note:** Advanced parameters must be equal to all `tcp` things sharing the same `host` and `port`.

The advanced parameters have conservative defaults, meaning that they should work for most users. In some cases when extreme performance is required (e.g. poll period below 10 ms), one might want to decrease the delay parameters, especially `timeBetweenTransactionsMillis`. Similarly, with some slower devices on might need to increase the values.

### `serial` thing

`serial` is representing a particular Modbus serial slave.

Basic parameters

| Parameter | Type    | Required | Default if omitted | Description                                                                                                                                                                                               |     |
| --------- | ------- | -------- | ------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --- |
| port      | text    | ✓        |                    | Serial port to use, for example `/dev/ttyS0` or `COM1`                                                                                                                                                    |     |
| id        | integer |          | `1`                | Slave id. Also known as station address or unit identifier. See [Wikipedia](https://en.wikipedia.org/wiki/Modbus) and [simplymodbus](http://www.simplymodbus.ca/index.html) articles for more information |     |
| baud      | integer | ✓        |                    | Baud of the connection. Valid values are: `75`, `110`, `300`, `1200`, `2400`, `4800`, `9600`, `19200`, `38400`, `57600`, `115200`.                                                                        |     |
| stopBits  | text    | ✓        |                    | Stop bits. Valid values are: `"1"`, `"1.5"`, `"2"`.                                                                                                                                                       |     |
| parity    | text    | ✓        |                    | Parity. Valid values are: `"none"`, `"even"`, `"odd"`.                                                                                                                                                    |     |
| dataBits  | integer | ✓        |                    | Data bits. Valid values are: `5`, `6`, `7` and `8`.                                                                                                                                                       |     |
| encoding  | text    | ✓        |                    | Encoding. Valid values are: `"ascii"`, `"rtu"`, `"bin"`.                                                                                                                                                  |     |
| echo      | boolean |          | `false`            | Flag for setting the RS485 echo mode. This controls whether we should try to read back whatever we send on the line, before reading the response. Valid values are: `true`, `false`.                      |     |

Advanced parameters

| Parameter                       | Required | Type    | Default if omitted | Description                                                                                                                                |
| ------------------------------- | -------- | ------- | ------------------ | ------------------------------------------------------------------------------------------------------------------------------------------ |
| `receiveTimeoutMillis`          |          | integer | `1500`             | Timeout for read operations. In milliseconds.                                                                                              |
| `flowControlIn`                 |          | text    | `"none"`           | Type of flow control for receiving. Valid values are: `"none"`, `"xon/xoff in"`, `"rts/cts in"`.                                           |
| `flowControlOut`                |          | text    | `"none"`           | Type of flow control for sending. Valid values are: `"none"`, `"xon/xoff out"`, `"rts/cts out"`.                                           |
| `timeBetweenTransactionsMillis` |          | integer | `60`               | How long to delay we must have at minimum between two consecutive MODBUS transactions. In milliseconds.                                    |
| `connectMaxTries`               |          | integer | `1`                | How many times we try to establish the connection. Should be at least 1.                                                                   |
| `connectTimeoutMillis`          |          | integer | `10000`            | The maximum time that is waited when establishing the connection. Value of zero means thatsystem/OS default is respected. In milliseconds. |

With the exception of `id` parameters should be equal to all `serial` things sharing the same `port`.

These parameters have conservative defaults, meaning that they should work for most users. In some cases when extreme performance is required (e.g. poll period below 10ms), one might want to decrease the delay parameters, especially `timeBetweenTransactionsMillis`. With some slower devices on might need to increase the values.

With low baud rates and/or long read requests (that is, many items polled), there might be need to increase the read timeout `receiveTimeoutMillis` to e.g. `5000` (=5 seconds).

### `poller` thing

`poller` thing takes care of polling the slave data regularly.

| Parameter  | Type    | Required | Default if omitted | Description                                                                                                                                                                            |
| ---------- | ------- | -------- | ------------------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `start`    | integer |          | `0`                | Address of the first register, coil, or discrete input to poll. Input as zero-based index number.                                                                                      |
| `length`   | integer | ✓        | (-)                | Number of registers, coils or discrete inputs to read.                                                                                                                                 |
| `type`     | text    | ✓        | (-)                | Type of modbus items to poll. This matches directly to Modbus request type or function code (FC). Valid values are: `coil` (FC01), `discrete` (FC02), `holding`(FC03), `input` (FC04). |
| `refresh`  | integer |          | `500`              | Poll interval in milliseconds. Use zero to disable automatic polling.                                                                                                                  |
| `maxTries` | integer |          | `3`                | Maximum tries when reading. <br /><br />Number of tries when reading data, if some of the reading fail. For single try, enter 1.                                                       |

Note: Polling can be manually triggered by sending `REFRESH` command to item bound to channel of `data` thing. When manually triggering polling, a new poll is executed as soon as possible, and sibling `data` things (i.e. things that share the same `poller` bridge) are updated.

### `data` thing

`data` is responsible of extracting relevant piece of data (e.g. a number `3.14`) from binary received from the slave. Similarly, `data` thing is responsible of converting openHAB commands to write requests to the modbus slave.

| Parameter                                   | Type    | Required | Default if omitted | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| ------------------------------------------- | ------- | -------- | ------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `readValueType`                             | text    |          | (empty)            | How data is read from modbus. Use empty for write-only things.<br /><br />Bit value type must be used with coils and discrete inputs. With registers all value types are applicable. Valid values are: `"float32"`, `"float32_swap"`, `"int32"`, `"int32_swap"`, `"uint32"`, `"uint32_swap"`, `"int16"`, `"uint16"`, `"int8"`, `"uint8"`, or `"bit"`.                                                                                                                                                                                                                                                                                                 |
| `readStart`                                 | text    |          | (empty)            | Start address to start reading the value. Use empty for write-only things. <br /><br />Input as zero-based index number, e.g. in place of `400001` (first holding register), use the address `"0"`.  Must be between (poller start) and (poller start + poller length - 1) (inclusive).<br /><br />With registers and value type less than 16 bits, you must use `"X.Y"` format where `Y` specifies the sub-element to read from the 16 bit register:<ul> <li>For example, `"3.1"` would mean pick second bit from register index `3` with bit value type. </li><li>With int8 valuetype, it would pick the high byte of register index `3`.</li></ul> |
| `readTransform`                             | text    |          | `"default"`        | Transformation to apply to polled data, after it has been converted to number using `readValueType`. <br /><br />Use "default" to communicate that no transformation is done and value should be passed as is.<br />Use `"SERVICENAME(ARG)"` to use transformation service `SERVICENAME` with argument `ARG`. <br />Any other value than the above types will be interpreted as static text, in which case the actual content of the polled value is ignored.                                                                                                                                                                                         |
| `writeValueType`                            | text    |          | (empty)            | How data is written to modbus. Only applicable to registers. Valid values are: `float32`, `float32_swap`, `int32`, `int32_swap`, `int16`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| `writeStart`                                | text    |          | (empty)            | Start address of the first holding register or coils in the write. Use empty for read-only things. <br />Use zero based address, e.g. in place of 400001 (first holding register), use the address 0. This address is passed to data frame as is.                                                                                                                                                                                                                                                                                                                                                                                                     |
| `writeType`                                 | text    |          | (empty)            | Type of data to write. Use empty for read-only things. Valid values: `"coil"` or `"holding"`.<br /><br /> Coil uses function code (FC) FC05 or FC15. Holding register uses FC06 or FC16. See `writeMultipleEvenWithSingleRegisterOrCoil` parameter.                                                                                                                                                                                                                                                                                                                                                                                                   |
| `writeTransform`                            | text    |          | `"default"`        | Transformation to apply to received commands.<br /><br />Use `"default"` to communicate that no transformation is done and value should be passed as is.    <br />Use `"SERVICENAME(ARG)"` to use transformation service `SERVICENAME` with argument `ARG`.    <br />Any other value than the above types will be interpreted as static text, in which case the actual content of the command value is ignored.                                                                                                                                                                                                                                       |
| `writeMultipleEvenWithSingleRegisterOrCoil` | boolean |          | `false`            | Whether single register / coil of data is written using FC16 ("Write Multiple Holding Registers") / FC15 ("Write Multiple Coils"), respectively. <br /><br />If false, FC06 ("Write single holding register") / FC05 ("Write single coil") are used with single register and single coil, respectively.                                                                                                                                                                                                                                                                                                                                               |
| `writeMaxTries`                             | integer |          | `3`                | Maximum tries when writing <br /><br />Number of tries when writing data, if some of the writes fail. For single try, enter `1`.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |


## Channels

Only the `data` thing has channels. It has several "data channels", serving the polled data in different formats, and for accepting openHAB commands from different item types.

Please note that transformations might be *necessary* in order to update some data channels, or to convert some openHAB commands to suitable Modbus data. See [Transformations](#transformations) for more details.

| Channel Type ID | Item Type       | Description                         |
| --------------- | --------------- | ----------------------------------- |
| `number`        | `Number`        | Data as number                      |
| `switch`        | `Switch`        | Data as switch (`ON` / `OFF`)       |
| `contact`       | `Contact `      | Data as contact (`OPEN` / `CLOSED`) |
| `dimmer`        | `Dimmer`        | Data as dimmer                      |
| `datetime`      | `DateTime`      | Data as a date time                 |
| `string`        | `String`        | Data as string                      |
| `rollershutter` | `Rollershutter` | Data as roller shutter              |

You can send a `REFRESH` command to items linked to any of the above channels to ask binding to explicitly poll new data from the Modbus slave.

Furthermore, there are additional channels that are useful for diagnostics:

| Channel Type ID    | Item Type  | Description           |
| ------------------ | ---------- | --------------------- |
| `lastReadSuccess`  | `DateTime` | Last successful read  |
| `lastReadError`    | `DateTime` | Last erroring read    |
| `lastWriteSuccess` | `DateTime` | Last successful write |
| `lastWriteError`   | `DateTime` | Last erroring write   |

## Details

### Read steps

Everytime data is read by the binding, these steps are taken to convert the raw binary data to actual item `State` in openHAB:

1. Poll the data from Modbus slave. Data received is stored in list of bits (discrete inputs and coils), or in list of registers (input registers and holding registers)
1. Extract a single number from the polled data, using specified location `readStart` and number "value type" `readValueType`. As an example, we can tell the binding to extract 32-bit float (`readValueType="float32"`) from register index `readStart="105"`.
1. Number is converted to string (e.g. `"3.14"`) and passed as input to the transformation. Note that in case `readTransform="default"`, a default transformation provided by the binding is used. See [Transformations](#transformations) section for more details.
1. For each [data channel](#channels), we try to convert the transformation output of previous step to a State type (e.g. `ON`/`OFF`, or `DecimalType`) accepted by the channel. If all the conversions fail (e.g. trying to convert `ON` to a number), the data channel is not updated.

In case of read errors, all data channels are left unchanged, and `lastReadError` channel is updated with current time. Examples of errors include connection errors, IO errors on read, and explicit exception responses from the slave.

### Write steps

#### Basic case

Commands passed to openHAB items that are bound to a [data channel](#channels) are most often processed according to following steps:

1. Command is sent to openHAB item, that is bound to a [data channel](#channels). Command must be such that it is accepted by the item in the first place
1. Command is converted to string (e.g. `"3.14"`) and passed to the transformation. Note that in case `readTransform="default"`, a default transformation provided by the binding is used. See [Transformations](#transformations) section for more details.
1. We try to convert transformation output to number (`DecimalType`), `OPEN`/`CLOSED` (`OpenClosedType`), and `ON`/`OFF` (`OnOffType`); in this order. First successful conversion is stored. For example, `"3.14"` would convert to number (`DecimalType`), while `"CLOSED"` would convert to `CLOSED` (of `OpenClosedType`). In case all conversions fail, the command is discarded and nothing is written to the Modbus slave.
1. Next step depends on the `writeType`:
   * `writeType="coil"`: the command from the transformation is converted to boolean. Non-zero numbers, `ON`, and `OPEN` are considered `true`; and rest as `false`
   * `writeType="holding"`: First, the command from the transformation is converted `1`/`0` number in case of `OPEN`/`ON` or `CLOSED`/`OFF`. The number is converted to one or more registers using `writeValueType`. For example, number `3.14` would be converted to two registers when `writeValueType="float32"`: [0x4048, 0xF5C3].
1. Boolean (`writeType="coil"`) or registers (`writeType="holding"`) are written to the Modbus slave using `FC05`, `FC06`, `FC15`, or `FC16`, depending on the value of `writeMultipleEvenWithSingleRegisterOrCoil`. Write address specified by `writeStart`.

#### Advanced write using JSON

There are some more advanced use cases which need more control how the command is converted to set of bits or requests. Due to this reason, one can return a special [JSON](https://en.wikipedia.org/wiki/JSON) output from the transformation (step 3). The JSON directly specifies the write requests to send to Modbus slave. In this case, steps 4. and 5. are skipped.

For example, if the transformation returns the following JSON

```json
[
    {
        "functionCode": 16,
        "address": 5412,
        "value": [1, 0, 5]
    },
    {
        "functionCode": 6,
        "address": 555,
        "value": [3],
        "maxTries": 10
    }
]
```

Two write requests would be sent to the Modbus slave

1. FC16 (write multiple holding register), with start address 5412, having three registers of data (1, 0, and 5).
1. FC06 (write single holding register), with start address 555, and single register of data (3). Write is tried maximum of 10 times in case some of the writes fail. 

The JSON transformation output can be useful when you need full control how the write goes, for example in case where the write address depends on the incoming command.

Empty JSON array (`[]`) can be used to suppress all writes. 

Explanation for the different properties of the JSON object in the array.

| Key name       | Value type            | Required | Default if omitted | Description                                                                               |
| -------------- | --------------------- | -------- | ------------------ | ----------------------------------------------------------------------------------------- |
| `functionCode` | number                | ✓        | (-)                | Modbus function code to use with write. Use one of `5`, `6`, `15` or `16`.                |
| `address`      | number                | ✓        | (-)                | Last erroring read                                                                        |
| `value`        | JSON array of numbers | ✓        | (-)                | Array of coil or register values. Encode coil values as `0` or `1`.                       |
| `maxTries`     | number                |          | 3                  | Number of tries when writing data, in case some of the writes fail. Should be at least 1. |

#### REFRESH command

`REFRESH` command to item bound to any [data channel](#channels) makes `poller` thing to poll new from the Modbus slave. All data channels of children `data` things are refreshed per the normal logic.

`REFRESH` can be useful tool if you like to refresh only on demand (`poller` has refresh disabled, i.e. `refresh=0`), or have custom logic of refreshing only in some special cases.


#### Comment on addressing

[Modbus Wikipedia article](https://en.wikipedia.org/wiki/Modbus#Coil.2C_discrete_input.2C_input_register.2C_holding_register_numbers_and_addresses) summarizes this excellently:

> In the traditional standard, [entity] numbers for those entities start with a digit, followed by a number of four digits in range 1–9,999:
> * coils numbers start with a zero and then span from 00001 to 09999
> * discrete input numbers start with a one and then span from 10001 to 19999
> * input register numbers start with a three and then span from 30001 to 39999
> * holding register numbers start with a four and then span from 40001 to 49999
>
> This translates into [entity] addresses between 0 and 9,998 in data frames.

The openHAB modbus binding uses data frame entity addresses when referring to modbus entities. That is, the entity address configured in modbus binding is passed to modbus protocol frame as-is. For example, Modbus `poller` thing with `start=3`, `length=2` and `type=holding` will read modbus entities with the following numbers 40004 and 40005.

### Transformations

Transformations serve two purpose

* `readTransform`: doing preprocessing transformations to read binary data and to make it more usable in openHAB
* `writeTransform`: doing preprocessing to openHAB commands before writing them to Modbus slave

Note that transformation is only one part of the overall process how polled data is converted to openHAB state, or how commands are converted to Modbus writes. Consult [Read steps](#read-steps) and [Write steps](#write-steps) for more details. Specifically, note that you might not need transformations at all in some uses cases.

Please also note that you should install relevant transformations, as necessary. For example, `openhab-transformation-javascript` feature provides the javascript (`JS`) transformation.

**`readTransform`** can be used to transform the polled data, after a number is extracted from the polled data using `readValueType` and `readStart` (consult [Read steps](#read-steps)).

There are three different format to specify the configuration:

1. String `"default"`, in which case the default transformation is used. The default is to convert non-zero numbers to `ON`/`OPEN`, and zero numbers to `OFF`/`CLOSED`, respectively. If the item linked to the data channel does not accept these states, the number is converted to best-effort-basis to the states accepted by the item. For example, the extracted number is passed as-is for `Number` items, while `ON`/`OFF` would be used with `DimmerItem`.
1. `"SERVICENAME(ARG)"` for calling a transformation service. The transformation receives the extracted number as input. This is useful for example scaling (divide by x) the polled data before it is used in openHAB. See examples for more details.
1. Any other value is interpreted as static text, in which case the actual content of the polled value is ignored. Transformation result is always the same. The transformation output is converted to best-effort-basis to the states accepted by the item.

**`writeTransform`** can be used to transform the openHAB command before it is converted to actual binary data (see [Write steps](#write-steps)).

There are three different format to specify the configuration:

1. String `"default"`, in which case the default transformation is used. The default is to do no conversion to the command.
1. `"SERVICENAME(ARG)"` for calling a transformation service. The transformation receives the command as input. This is useful for example scaling ("multiply by x") commands before the data is written to Modbus. See examples for more details.
1. Any other value is interpreted as static text, in which case the actual command is ignored. Transformation result is always the same.

#### Transformation example: scaling

Typical use case for transformations is scaling of numbers. The data in Modbus slaves is quite commonly encoded as integers, and thus scaling is necessary to convert them to useful float numbers.

`transform/multiply10.js`:

```javascript
// Wrap everything in a function (no global variable pollution)
// variable "input" contains data passed by openhab
(function(inputData) {
    // on read: the polled number as string
    // on write: i openHAB command as string
    var MULTIPLY_BY = 10;
    return Math.round(parseFloat(inputData, 10) * MULTIPLY_BY);
})(input)
```

`transform/divide10.js`:

```javascript
// Wrap everything in a function (no global variable pollution)
// variable "input" contains data passed by openhab
(function(inputData) {
    // on read: the polled number as string
    // on write: i openHAB command as string
    var DIVIDE_BY = 10;
    return parseFloat(inputData) / DIVIDE_BY;
})(input)
```

See [Scaling example](#scaling-example) for full example with things, items and a sitemap.

#### Example: inverting binary data on read and write

This example transformation is able to invert "boolean" input. In this case, boolean input is considered to be either number `0`/`1`, `ON`/`OFF`, or `OPEN`/`CLOSED`.

```javascript
// function to invert Modbus binary states
// variable "input" contains data passed by OpenHAB binding
(function(inputData) {
    var out = inputData ;      // allow Undefined to pass through
    if (inputData == '1' || inputData == 'ON' || inputData == 'OPEN') {
        out = '0' ;
    } else if (inputData == '0' || inputData == 'OFF' || inputData == 'CLOSED') {
        out = '1' ;
    }
    return out ;      // return a string
})(input)
```

## Full Examples

Things can be configured via the Paper UI, or using a `things` file like here.

### Basic example

This example reads different kind of Modbus items from the slave.

In addition, there is write-only entry for holding register index 5 (register number **4**0006). Note that `Holding6writeonly` item state might differ from the physical slave since it is not refreshed.

Please refer to the comments for more explanations.

`things/modbus_ex1.things`:

```xtend
Bridge modbus:tcp:localhostTCP [ host="127.0.0.1", port=502, id=2 ] {

    // read-write for coils. Reading 4 coils, with index 4, and 5.
    // These correspond to input register numbers 000005, and 000005
    Bridge poller coils [ start=4, length=2, refresh=1000, type="coil" ] {
        // Note the zero based indexing: first coil is index 0.
        Thing data do4 [ readStart="4", readValueType="bit", writeStart="4", writeValueType="bit", writeType="coil" ]
        Thing data do5 [ readStart="5", readValueType="bit", writeStart="5", writeValueType="bit", writeType="coil" ]
    }
    // read-write for holding registers. Reading 4 registers, with index 1500, 1501, 1502, 1503.
    // These correspond to holding register numbers 401501, 401502, 401503, 401504.
    Bridge poller holding [ start=1500, length=4, refresh=1000, type="holding" ] {
        Thing data holding1500 [ readStart="1500", readValueType="float32", writeStart="1500", writeValueType="float32", writeType="holding" ]
        Thing data holding1502 [ readStart="1502", readValueType="float32", writeStart="1502", writeValueType="float32", writeType="holding" ]
    }
    // read-only for input registers. Reading 4 registers, with index 1500, 1501, 1502, 1503.
    // These correspond to input register numbers 301501, 301502, 301503, 301504.
    Bridge poller inputRegisters [ start=1500, length=4, refresh=1000, type="input" ] {
        Thing data input1500 [ readStart="1500", readValueType="float32" ]
        Thing data input1502 [ readStart="1502", readValueType="float32" ]

        // Extract high or low byte of the 16-bit register as unsigned 8-bit integer (uint8)
        Thing data input1502lo [ readStart="1502.0", readValueType="uint8" ]
        Thing data input1502hi [ readStart="1502.1", readValueType="uint8" ]

        // Extract individual bits of the 16-bit register
        // bit 0 is the least significant bit, and bit 15 is the most significant bit of the register
        Thing data input1502bit0 [ readStart="1502.0", readValueType="uint8" ]
        Thing data input1502bit1 [ readStart="1502.1", readValueType="uint8" ]
        Thing data input1502bit2 [ readStart="1502.2", readValueType="uint8" ]
    }

    // read-only for discrete inputs. Reading 4 discrete inputs, with index 1200, 1201, 1202, 1203.
    // These correspond to input register numbers 101201, 101202, 101203, 101204.
    Bridge poller discreteInputs [ start=1200, length=4, refresh=1000, type="discrete" ] {
        Thing data di1200 [ readStart="1200", readValueType="bit" ]
        Thing data di1201 [ readStart="1201", readValueType="bit" ]
    }

    // Write-only entry: thing is child of tcp directly. No readStart etc. need to be defined.
    Thing data holding6write [ writeStart="5", writeValueType="int16", writeType="holding" ]
}
```

`items/modbus_ex1.items`:

```xtend
Switch DO4            "Digital Input index 4 [%d]"    { channel="modbus:data:localhostTCP:coils:do4:switch" }
Switch DO5            "Digital Input index 5 [%d]"    { channel="modbus:data:localhostTCP:coils:do5:switch" }

Contact DI1200            "Digital Input index 1200 [%d]"    { channel="modbus:data:localhostTCP:discreteInputs:di1200:contact" }
Contact DI1201            "Digital Input index 1201 [%d]"    { channel="modbus:data:localhostTCP:discreteInputs:di1201:contact" }

Number Input1500Float32            "Input registers 1500-1501 as float32 [%.1f]"    { channel="modbus:data:localhostTCP:inputRegisters:input1500:number" }
Number Input1500Float32            "Input registers 1502-1503 as float32 [%.1f]"    { channel="modbus:data:localhostTCP:inputRegisters:input1502:number" }

DateTime Input1500Float32LastOKRead            "Input registers 1502-1503 last read [%1$tA, %1$td.%1$tm.%1$tY %1$tH:%1$tM:%1$tS]"    { channel="modbus:data:localhostTCP:inputRegisters:input1502:lastReadSuccess" }
DateTime Input1500Float32LastBadRead            "Input registers 1502-1503 last read [%1$tA, %1$td.%1$tm.%1$tY %1$tH:%1$tM:%1$tS]"    { channel="modbus:data:localhostTCP:inputRegisters:input1502:lastReadError" }

Number Holding6writeonly            "Holding index 5 [%.1f]"    { channel="modbus:data:localhostTCP:holding6write:number" }
```

`sitemaps/modbus_ex1.sitemap`:

```xtend
sitemap modbus_ex1 label="modbus_ex1"
{
    Frame {
        Switch item=DO4
        Switch item=DO5
        Setpoint item=Holding6writeonly minValue=0 maxValue=100 step=20

        Default item=DI1200
        Default item=DI1201

        Default item=Input1500Float32
        Default item=Input1502Float32

        Default item=Input1500Float32LastOKRead
        Default item=Input1500Float32LastBadRead

    }
}
```

### Writing to different address and type than read

This updates the item from discrete input index 4, and writes commands to coil 5. This can be useful when the discrete input is the measurement (e.g. "is valve open?"), and the command is the control (e.g. "open/close valve").

The sitemap shows the current coil status. It also has switches to set/reset coil status, for debugging purposes. Toggling these switches always have the same effect: either setting or resetting the bit.

`things/modbus_ex2.things`:

```xtend
Bridge modbus:tcp:localhostTCPex2 [ host="127.0.0.1", port=502 ] {

    Bridge poller items [ start=4, length=2, refresh=1000, type="discrete" ] {
        // read from index 4, write to coil 5
        Thing data readDiscrete4WriteCoil5 [ readStart="4", readValueType="bit", writeStart="5", writeValueType="bit", writeType="coil" ]
        Thing data resetCoil5 [ writeTransform="0", writeStart="5", writeValueType="bit", writeType="coil" ]
        Thing data setCoil5 [ writeTransform="1", writeStart="5", writeValueType="bit", writeType="coil" ]
    }

    Bridge poller coils [ start=5, length=1, refresh=500, type="coil" ] {
        Thing data index5 [ readStart="5", readValueType="bit" ]
    }
}
```

`items/modbus_ex2.items`:

```xtend
Switch ReadDI4WriteDO5            "Coil 4/5 mix [%d]"    { channel="modbus:data:localhostTCPex2:items:readDiscrete4WriteCoil5:switch" }
Switch ResetDO5            "Flip to turn Coil 5 OFF [%d]"    { channel="modbus:data:localhostTCPex2:items:resetCoil5:switch" }
Switch SetDO5            "Flip to turn Coil 5 ON [%d]"    { channel="modbus:data:localhostTCPex2:items:setCoil5:switch" }
Contact Coil5            "Coil 5 [%d]"    { channel="modbus:data:localhostTCPex2:coils:index5:contact" }

```

`sitemaps/modbus_ex2.sitemap`:

```xtend
sitemap modbus_ex2 label="modbus_ex2"
{
    Frame {
        Switch item=ReadDI4WriteDO5
        Switch item=ResetDO5
        Switch item=SetDO5
        Text item=Coil5
    }
}
```

### Scaling example

This example divides value on read, and multiplies them on write.

`things/modbus_ex_scaling.things`:

```xtend
Bridge modbus:tcp:localhostTCP3 [ host="127.0.0.1", port=502 ] {
    Bridge poller holdingPoller [ start=5, length=1, refresh=5000, type="holding" ] {
        Thing data holding5Scaled [ readStart="5", readValueType="int16", readTransform="JS(divide10.js)", writeStart="5", writeValueType="int16", writeType="holding", writeTransform="JS(multiply10.js)" ]
    }
}
```

`items/modbus_ex_scaling.items`:

```xtend
Number Holding5Scaled            "Holding index 5 scaled [%.1f]"   { channel="modbus:data:localhostTCP3:holdingPoller:holding5Scaled:number" }
```

`sitemaps/modbus_ex_scaling.sitemap`:

```xtend
sitemap modbus_ex_scaling label="modbus_ex_scaling"
{
    Frame {
        Text item=Holding5Scaled
        Setpoint item=Holding5Scaled minValue=0 maxValue=100 step=20
    }
}
```

See [transformation example](#transformation-example-scaling) for the `divide10.js` and `multiply10.js`.

### Rollershutter example

#### Rollershutter

This is an example how different Rollershutter commands can be written to Modbus.

Roller shutter position is read from register 0, `UP`/`DOWN` commands are written to register 1, and `MOVE`/`STOP` commands are written to register 2.

The logic of processing commands are summarized in the table

| Command | Number written to Modbus slave | Register index |
| ------- | ------------------------------ | -------------- |
| `UP`    | `1`                            | 1              |
| `DOWN`  | `-1`                           | 1              |
| `MOVE`  | `1`                            | 2              |
| `STOP`  | `0`                            | 2              |


`things/modbus_ex_rollershutter.things`:

```xtend
Bridge modbus:tcp:localhostTCPRollerShutter [ host="127.0.0.1", port=502 ] {
    Bridge poller holding [ start=0, length=3, refresh=1000, type="holding" ] {
        Thing data rollershutterData [ readStart="0", readValueType="int16",  writeTransform="JS(rollershutter.js)", writeType="holding" ]

        // For diagnostics
        Thing data rollershutterDebug0 [ readStart="0", readValueType="int16", writeStart="0", writeValueType="int16", writeType="holding" ]
        Thing data rollershutterDebug1 [ readStart="1", readValueType="int16" ]
        Thing data rollershutterDebug2 [ readStart="2", readValueType="int16" ]
    }
}
```

`items/modbus_ex_rollershutter.items`:

```xtend
// We disable auto-update to make sure that rollershutter position is updated from the slave, not "automatically" via commands
Rollershutter RollershutterItem "Roller shutter position [%.1f]" <temperature> { autoupdate="false", channel="modbus:data:localhostTCPRollerShutter:holding:rollershutterData:rollershutter" }

// For diagnostics
Number RollershutterItemDebug0 "Roller shutter Debug 0 [%d]" <temperature> { channel="modbus:data:localhostTCPRollerShutter:holding:rollershutterDebug0:number" }
Number RollershutterItemDebug1 "Roller shutter Debug 1 [%d]" <temperature> { channel="modbus:data:localhostTCPRollerShutter:holding:rollershutterDebug1:number" }
Number RollershutterItemDebug2 "Roller shutter Debug 2 [%d]" <temperature> { channel="modbus:data:localhostTCPRollerShutter:holding:rollershutterDebug2:number" }
```

`sitemaps/modbus_ex_rollershutter.sitemap`:

```xtend
sitemap modbus_ex_rollershutter label="modbus_ex_rollershutter" {
    Switch item=RollershutterItem label="Roller shutter [(%d)]" mappings=[UP="up", STOP="X", DOWN="down", MOVE="move"]

    // For diagnostics
    Setpoint item=RollershutterItemDebug0 minValue=0 maxValue=100 step=20
    Text item=RollershutterItemDebug0
    Text item=RollershutterItemDebug1
    Text item=RollershutterItemDebug2
}
```

`transform/rollershutter.js`:

```javascript
// Wrap everything in a function
// variable "input" contains data passed by openhab
(function(cmd) {
    var cmdToValue = {"UP": 1,  "DOWN": -1, "MOVE": 1, "STOP": 0};
    var cmdToAddress = {"UP": 1, "DOWN": 1, "MOVE": 2, "STOP": 2};

    var value = cmdToValue[cmd];
    var address = cmdToAddress[cmd];
    if(value === undefined || address === undefined) {
        // unknown command, do not write anything
        return "[]";
    } else {
        return [
            "[",
                   "{\"functionCode\": 6, \"index\":" + address.toString() + ", \"value\": [" + value +  "] }",
            "]",
        ].join("\n")
    }
})(input)
```




# OLD

### Item configuration examples

## Details


### Register interpretation (valuetype) on read & write

Note that this section applies to register elements only (`holding` or `input` type)

#### Read

When the binding interprets and converts polled input registers (`input`) or holding registers (`holding`) to openHAB items, the process goes like this:

- 1. register(s) are first parsed to a number (see below for the details, exact logic depends on `valuetype`)
- 2a. if the item is Switch or Contact: zero is converted CLOSED / OFF. Other numbers are converted to OPEN / ON.
- 2b. if the item is Number: the value is used as is
- 3. transformation is done to the value, if configured. The transformation output (string) is parsed to state using item's accepted state types (e.g. number, or CLOSED/OPEN).

Polled registers from the Modbus slave are converted to openHAB state. The exact conversion logic depends on `valuetype` as described below.

Note that _first register_ refers to register with address `start` (as defined in the slave definition), _second register_ refers to register with address `start + 1` etc. The _index_ refers to item read index, e.g. item `Switch MySwitch "My Modbus Switch" (ALL) {modbus="slave1:5"}` has 5 as read index.

`valuetype=bit`:

- a single bit is read from the registers
- indices between 0...15 (inclusive) represent bits of the first register
- indices between 16...31 (inclusive) represent bits of the second register, etc.
- index 0 refers to the least significant bit of the first register
- index 1 refers to the second least significant bit of the first register, etc.

(Note that updating a bit in a holding type register will NOT work as expected across Modbus, the whole register gets rewritten. Best to use a read-only mode, such as Contact item.  Input type registers are by definition read-only.)

`valuetype=int8`:

- a byte (8 bits) from the registers is interpreted as signed integer
- index 0 refers to low byte of the first register, 1 high byte of first register
- index 2 refers to low byte of the second register, 3 high byte of second register, etc.
- it is assumed that each high and low byte is encoded in most significant bit first order

`valuetype=uint8`:

- same as `int8` except values are interpreted as unsigned integers

`valuetype=int16`:

- register with index (counting from zero) is interpreted as 16 bit signed integer.
- it is assumed that each register is encoded in most significant bit first order

`valuetype=uint16`:

- same as `int16` except values are interpreted as unsigned integers

`valuetype=int32`:

- registers (2 index) and ( 2 *index + 1) are interpreted as signed 32bit integer.
- it assumed that the first register contains the most significant 16 bits
- it is assumed that each register is encoded in most significant bit first order

`valuetype=uint32`:

- same as `int32` except values are interpreted as unsigned integers

`valuetype=float32`:

- registers (2 index) and ( 2 *index + 1) are interpreted as signed 32bit floating point number.
- it assumed that the first register contains the most significant 16 bits
- it is assumed that each register is encoded in most significant bit first order

##### Word Swapped valuetypes (New since 1.9.0)

The MODBUS specification defines each 16bit word to be encoded as Big Endian,
but there is no specification on the order of those words within 32bit or larger data types.
The net result is that when you have a master and slave that operate with the same
Endian mode things work fine, but add a device with a different Endian mode and it is
very hard to correct. To resolve this the binding supports a second set of valuetypes
that have the words swapped.

If you get strange values using the `int32`, `uint32` or `float32` valuetypes then just try the `int32_swap`, `uint32_swap` or `float32_swap` valuetype, depending upon what your data type is.

`valuetype=int32_swap`:

- registers (2 index) and ( 2 *index + 1) are interpreted as signed 32bit integer.
- it assumed that the first register contains the least significant 16 bits
- it is assumed that each register is encoded in most significant bit first order (Big Endian)

`valuetype=uint32_swap`:

- same as `int32_swap` except values are interpreted as unsigned integers

`valuetype=float32_swap`:

- registers (2 index) and ( 2 *index + 1) are interpreted as signed 32bit floating point number.
- it assumed that the first register contains the least significant 16 bits
- it is assumed that each register is encoded in most significant bit first order (Big Endian)


##### Extra notes

- `valuetypes` smaller than one register (less than 16 bits) actually read the whole register, and finally extract single bit from the result.

#### Write

When the binding processes openHAB command (e.g. sent by `sendCommand` as explained [here](https://github.com/openhab/openhab1-addons/wiki/Actions)), the process goes as follows

1. it is checked whether the associated item is bound to holding register. If not, command is ignored.
2. command goes through transformation, if configured. No matter what commands the associated item accepts, the transformation can always output number (DecimalType), OPEN/CLOSED (OpenClosedType) and ON/OFF (OnOffType).
3. command is converted to 16bit integer (in [two's complement format](https://www.cs.cornell.edu/~tomf/notes/cps104/twoscomp.html)). See below for details.
4. the 16bits are written to the register with address `start` (as defined in the slave definition)

Conversion rules for converting command to 16bit integer

- UP, ON, OPEN commands that are converter to number 1
- DOWN, OFF, CLOSED commands are converted to number 0
- Decimal commands are truncated as 32 bit integer (in 2's complement representation), and then the least significant 16 bits of this integer are extracted.
- INCREASE, DECREASE: see below

Other commands are not supported.

**Note: The way Decimal commands are handled currently means that it is probably not useful to try to use Decimal commands with non-16bit `valuetype`s.**

Converting INCREASE and DECREASE commands to numbers is more complicated

(After 1.10.0)

1. Most recently polled state (as it has gone through the read transformations etc.) of this item is acquired
2. add/subtract `1` from the state. If the state is not a number, the whole command is ignored.

(Before 1.10.0)

1. Register matching (`start` + read index) is interpreted as unsigned 16bit integer. Previous polled register value is used
2. add/subtract `1` from the integer

**Note (before 1.10.0): note that INCREASE and DECREASE ignore valuetype when using the previously polled value. Thus, it is not recommended to use INCREASE and DECREASE commands with other than `valuetype=uint16`**

#### Modbus RTU over TCP

Some devices uses modbus RTU over TCP. This is usually Modbus RTU encapsulation in an ethernet packet. So, those devices does not work with Modbus TCP binding since it is Modbus with a special header. Also Modbus RTU over TCP is not supported by Openhab Modbus Binding. But there is a workaround: you can use a Virtual Serial Port Server, to emulate a COM Port and Bind it with OpenHab unsing Modbus Serial.



## Config Examples

Please take a look at [Samples-Binding-Config page](https://github.com/openhab/openhab1-addons/wiki/Samples-Binding-Config) or examine to the following examples.

- Minimal construction in modbus.cfg for TCP connections will look like:

```
# read 10 coils starting from address 0
tcp.slave1.connection=192.168.1.50
tcp.slave1.length=10
tcp.slave1.type=coil
```

- Minimal construction in modbus.cfg for serial connections will look like:

```
# read 10 coils starting from address 0
serial.slave1.connection=/dev/ttyUSB0
tcp.slave1.length=10
tcp.slave1.type=coil
```

- More complex setup could look like

```
# Poll values very 300ms = 0.3 seconds
poll=300

# Connect to modbus slave at 192.168.1.50, port 502
tcp.slave1.connection=192.168.1.50:502
# use slave id 41 in requests
tcp.slave1.id=41
# read 32 coils (digital outputs) starting from address 0
tcp.slave1.start=0
tcp.slave1.length=32
tcp.slave1.type=coil
```

- Another example where coils, discrete inputs (`discrete`) and input registers (`input`) are polled from modbus tcp slave at `192.168.6.180`.

> (original example description:)
> example for an moxa e1214 module in simple io mode
> 6 output switches starting from modbus address 0 and
> 6 inputs from modbus address 10000 (the function 2 implizits the modbus 10000 address range)
> you only read 6 input bits and say start from 0
> the moxa manual ist not right clear in this case

```ini
poll=300

# Query coils from 192.168.6.180
tcp.slave1.connection=192.168.6.180:502
tcp.slave1.id=1
tcp.slave1.start=0
tcp.slave1.length=6
tcp.slave1.type=coil

# Query discrete inputs from 192.168.6.180
tcp.slave2.connection=192.168.6.180:502
tcp.slave2.id=1
tcp.slave2.start=0
tcp.slave2.length=6
tcp.slave2.type=discrete

# Query input registers from 192.168.6.180
tcp.slave3.connection=192.168.6.180:502
tcp.slave3.id=1
tcp.slave3.start=17
tcp.slave3.length=2
tcp.slave3.type=input

# Query holding registers from 192.168.6.181
# Holding registers matching addresses 33 and 34 are read
tcp.slave4.connection=192.168.6.181:502
tcp.slave4.id=1
tcp.slave4.start=33
tcp.slave4.length=2
tcp.slave4.type=holding

# Query 2 input registers from 192.168.6.181.
# Interpret the two registers as single 32bit floating point number
tcp.slave5.connection=192.168.6.181:502
tcp.slave5.id=1
tcp.slave5.start=10
tcp.slave5.length=2
tcp.slave5.type=input
tcp.slave5.valuetype=float32
```

Above we used the same modbus gateway with ip 192.168.6.180 multiple times
on different modbus address ranges and modbus functions.

## Troubleshooting

### Enable verbose logging

Enable `DEBUG` or `TRACE` (even more verbose) logging for the loggers named:

* `net.wimpi.modbus`
* `org.openhab.binding.modbus`

## For developers

### Testing serial implementation

You can use test serial slaves without any hardware on linux using these steps:

1. Set-up virtual null modem emulator using [tty0tty](https://github.com/freemed/tty0tty)
2. Download [diagslave](http://www.modbusdriver.com/diagslave.html) and start modbus serial slave up using this command:

```
./diagslave -m rtu -a 1 -b 38400 -d 8 -s 1 -p none -4 10 /dev/pts/7
```

3. Configure openHAB's modbus slave to connect to `/dev/pts/8`:

```
xxx.connection=/dev/pts/8:38400:8:none:1:rtu
```

4. Modify `start.sh` or `start_debug.sh` to include the unconventional port name by adding the following argument to `java`:

```
-Dgnu.io.rxtx.SerialPorts=/dev/pts/8
```

Naturally this is not the same thing as the real thing but helps to identify simple issues.

### Testing TCP implementation

1. Download [diagslave](http://www.modbusdriver.com/diagslave.html) and start modbus tcp server (slave) using this command:

```
./diagslave -m tcp -a 1 -p 55502
```

2. Configure openHAB's modbus slave to connect to `127.0.0.1:55502`:

```
tcp.slave1.connection=127.0.0.1:55502
```

### Writing data

See this [community post](https://community.openhab.org/t/something-is-rounding-my-float-values-in-sitemap/13704/32?u=ssalonen) explaining how `pollmb` and `diagslave` can be used to debug modbus communication.

### Troubleshooting

To troubleshoot, you might be asked to update to latest development version. You can find the "snapshot" or development version from [Cloudbees CI](https://openhab.ci.cloudbees.com/job/openHAB1-Addons/lastSuccessfulBuild/artifact/bundles/binding/org.openhab.binding.modbus/target/).

With modbus binding before 1.9.0, it strongly recommended to try out with the latest development version since many bugs were fixed in 1.9.0. Furthermore, error logging is enhanced in this new version.

If the problem persists in the new version, it is recommended to try to isolate to issue using minimal configuration. Easiest would be to have a fresh openHAB installation, and configure it minimally (if possible, single modbus slave in `modbus.cfg`, single item, no rules etc.). This helps the developers and community to debug the issue.

Problems can be communicated via [community.openhab.org](https://community.openhab.org). Please use the search function to find out existing reports of the same issue.

It helps greatly to document the issue in detail (especially how to reproduce the issue), and attach the related [verbose logs](#enable-verbose-debug-logging). Try to keep interaction minimal during this test; for example, if the problem occurs with modbus read alone, do not touch the the switch items in openHAB sitemap (would trigger write).

For attaching the logs to a community post, the [pastebin.com](http://pastebin.com/) service is strongly recommended to keep the thread readable. It is useful to store the logs from openHAB startup, and let it run for a while.

Remember to attach configuration lines from modbus.cfg, item definitions related to modbus binding, and any relevant rules (if any). You can use [pastebin.com](http://pastebin.com/) for this purpose as well.

To summarize, here are the recommended steps in case of errors

1. Update to latest development version; especially if you are using modbus binding version before 1.9.0
2. isolate the issue using minimal configuration, and enable verbose logging (see above)
3. record logs and configuration to [pastebin.com](http://pastebin.com/).