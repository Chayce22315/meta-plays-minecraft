# meta plays minecraft

fabric client mod for minecraft java 26.2 that gives a local ai model a bridge into your minecraft session.

## current milestone

this pull request adds the first ai-player foundation:

- compact player/world perception snapshots
- local openai-compatible http bridge at `127.0.0.1:8000/v1/chat/completions`
- structured json actions
- movement, look, jump, and chat execution
- hostile-mob combat execution
- a hard code-level rule that player entities are never valid combat targets

## target behavior

the end goal is for the ai player to behave like a normal cooperative minecraft player:

- join worlds / configured servers
- walk, sprint, jump, crouch, swim, climb
- look around naturally
- talk in chat
- build and place blocks
- break and collect blocks
- manage inventory and equipment
- craft items and use crafting stations
- sleep and wake
- fight hostile mobs
- interact with blocks, items, villagers, containers, and other world objects
- navigate, remember locations, and pursue longer-term goals

those systems are being built incrementally. this milestone intentionally keeps the action vocabulary small while the bridge and validation layers are established.

## ai contract

the model should return exactly one json action. example:

```json
{
  "type": "move",
  "forward": 1,
  "sideways": 0,
  "jump": false
}
```

supported foundation actions are `noop`, `look`, `move`, `jump`, `chat`, and `attack`.

combat is intentionally implemented so the action executor never attacks a player target. the combat selector only accepts non-player living entities.

## local model bridge

run an openai-compatible inference server locally and expose `/v1/chat/completions`. the mod currently defaults to:

`http://127.0.0.1:8000/v1/chat/completions`

## build

minecraft 26.2 requires java 25. the github actions workflow installs java 25 and gradle 9.5.1, then runs the project build.

## source template

based on the official fabric example mod for minecraft 26.2:
https://github.com/FabricMC/fabric-example-mod/tree/26.2
