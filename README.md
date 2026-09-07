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
- local openai-compatible model bridge
- in-game configuration through mod menu

## ai configuration

open **mod menu -> meta plays minecraft -> configure**.

### providers

- **ollama**: `http://127.0.0.1:11434/v1/chat/completions`
- **openai compatible**: any compatible chat-completions endpoint
- **local server**: useful for llama.cpp, vllm, lm studio, or another local compatible server

all three providers let you edit the endpoint, model, optional api key, generation settings, request timeout, and decision interval.

configuration is saved to `config/meta_plays_minecraft.json`.

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
- mod menu 20.0.0

## source template

based on the official fabric example mod for minecraft 26.2:
https://github.com/FabricMC/fabric-example-mod/tree/26.2
