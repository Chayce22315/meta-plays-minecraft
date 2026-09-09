# metabot

this is the real second-client connection used by meta plays minecraft.

## quick start on windows

from the repository root, double-click:

```text
setup-metabot.bat
```

that checks for Node.js and installs the bot dependency with `npm install`.

once setup is finished, start the bot with:

```text
start-metabot.bat
```

or run `bot/start-metabot.bat` directly.

the launcher asks for the minecraft server address, port, and bot username. for a singleplayer world opened to LAN on the same pc, the defaults are:

```text
server: 127.0.0.1
port: 25565
username: MetaBot
```

keep the launcher window open while the bot is connected.

## minecraft setup

1. launch minecraft java 26.2 with the fabric mod.
2. open your singleplayer world.
3. open the world to LAN.
4. note the LAN port shown by minecraft if it is not the default `25565`.
5. run `start-metabot.bat` from the repository root.
6. enter the LAN port when prompted.
7. wait for `[meta-bot] joined the world`.

when the bot joins, it is a separate player connection rather than a fake minecraft entity.

the fabric mod can also start the local metabot automatically through its in-game bridge. if you use that path, the mod reads the integrated server's actual LAN port instead of requiring you to type it.

## manual test

```text
cd bot
npm install
node index.js --host 127.0.0.1 --port 25565 --username MetaBot --control-port 8765
```

when successful, the terminal prints `[meta-bot] joined the world`.

## requirements

- minecraft java 26.2
- the fabric mod from this repository
- node.js installed and available as `node` and `npm`
- a singleplayer world opened to LAN, or another minecraft server you own/control that accepts the bot's offline connection

metabot uses the 26.2 protocol stack and offline authentication. use it only with worlds/servers you own or control.
