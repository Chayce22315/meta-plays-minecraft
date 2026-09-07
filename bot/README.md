# metabot

this is the real second-client connection used by meta plays minecraft.

## setup

from the repository root:

```text
cd bot
npm install
```

then, with your minecraft 26.2 singleplayer world open to LAN, the fabric mod can start it automatically with `f8`.

metabot uses the 26.2 protocol stack and connects to `127.0.0.1` using offline authentication because the target is your own local LAN world. do not use this mode for servers you do not own or control.

## manual test

```text
node index.js --host 127.0.0.1 --port 25565 --username MetaBot --control-port 8765
```

when successful, the terminal prints `[meta-bot] joined the world`.
