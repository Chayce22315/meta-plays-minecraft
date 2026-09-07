# meta plays minecraft

fabric client mod for minecraft java 26.2 that lets a local ai model control a cooperative minecraft player.

## current behavior layer

- movement: forward, backward, strafe, jump, sprint, crouch
- camera: free look and target aiming
- chat: send and remember incoming player chat
- world perception: position, rotation, health, food, xp, nearby mobs, players, item drops, weather, time and workstation/bed hints
- hostile-mob combat with a hard player-target exclusion
- block mining through normal client input
- block use and placement through normal client input
- inventory hotbar selection
- sleep by locating a nearby bed
- core crafting automation for planks, sticks, crafting tables, chests, furnaces and torches
- simple waypoint movement with obstacle-aware jumping
- action validation and distance limits
- local ollama model bridge
- real second-client metabot world connection

## ollama model bridge

by default the mod talks to ollama through its local openai-compatible api:

`http://127.0.0.1:11434/v1/chat/completions`

The default model is `llama3.1:8b`.

make sure ollama is running and the model you choose is available locally. the mod does not download models automatically.

to use a different model, set:

`META_MINECRAFT_OLLAMA_MODEL=<your-model-name>`

before starting minecraft.

to use a different ollama host, set:

`META_MINECRAFT_OLLAMA_HOST=http://127.0.0.1:11434`

## real metabot connection

open your singleplayer world to lan, then use the mod's metabot toggle to start the separate player connection.

the ai brain can then produce structured actions which are forwarded to the real metabot client.

## target experience

the model should behave like a cooperative human teammate: observe, choose a goal, perform small actions, react to the world, remember recent chat, and avoid attacking players.

## important implementation note

this is a client-side controller. minecraft's own networking and interaction rules remain authoritative. the ai produces intent; the mod translates that intent into normal client inputs and validated interactions.

## target stack

- minecraft java 26.2
- fabric loader 0.19.5
- fabric loom 1.17
- fabric api 0.159.0+26.2
- java 25
- gradle 9.5.1

## next layers

- richer long-term memory
- stronger navigation and recovery
- broader recipe automation
- chest and container management
- equipment and resource planning
- automatic session/world joining
- cooperative task planning

## source template

based on the official fabric example mod for minecraft 26.2:
https://github.com/FabricMC/fabric-example-mod/tree/26.2
