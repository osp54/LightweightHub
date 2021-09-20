# Configuring

After loading the server with the plugin, the `config-hub.json` file should be created in directory `config`. <br>
The plugin configuration file uses the JSON format, see [json.org](https://www.json.org/json-en.html) site

### Available Fields

| Field           | Type                                      | Description                                                                                             |
| --------------- | ----------------------------------------- | ------------------------------------------------------------------------------------------------------- |
| log-connects    | boolean                                   | log player connections to hub's servers in `[player uuid] player name --> server ip:server port` format |
| servers         | array of [server](#server-object) objects | hub's servers                                                                                           |
| effects         | array of [effect](#effect-object) objects | static effects that appear with a specified period                                                      |
| event-effects   | [event effects](#event-effect-object)     | effects called on a specific event                                                                      |

### Server Object

###### Server Object structure

| Field      | Type    | Description                                                        |
| ---------- | ------- | ------------------------------------------------------------------ |
| ip         | string  | server ip                                                          |
| port       | integer | server port                                                        |
| size       | integer | the teleport block size                                            |
| teleport-x | integer | latter x coordinate of the teleport border. minimap coordinate     |
| teleport-y | integer | latter y coordinate of the teleport border. minimap coordinate     |
| title      | string  | server display name                                                |
| title-x    | float   | latter x coordinate of the title signboard. minimap coordinate * 8 |
| title-y    | float   | latter y coordinate of the title signboard. minimap coordinate * 8 |
| label-x    | float   | latter x coordinate of the status label. minimap coordinate * 8    |
| label-y    | float   | latter y coordinate of the status label. minimap coordinate * 8    |

###### Optimal Setup Formula

```
title-x = (teleport-x * 8) + 12
title-y = (teleport-y * 8) + 36

label-x = (teleport-x * 8) + 12
label-y = (teleport-y * 8) - 16
```

###### Example Server Object

```json
{
  "ip": "localhost",
  "port": 6567,
  "size": 7,
  "teleport-x": 22,
  "teleport-y": 37,
  "title": "title",
  "title-x": 200,
  "title-y": 320,
  "label-x": 200,
  "label-y": 288
}
```
