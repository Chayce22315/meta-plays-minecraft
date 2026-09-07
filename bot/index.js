const mineflayer = require('mineflayer')
const http = require('http')
const { routeAction, stopMovement } = require('./action-router')

function arg(name, fallback) {
  const index = process.argv.indexOf(name)
  return index >= 0 && process.argv[index + 1] ? process.argv[index + 1] : fallback
}

const host = arg('--host', '127.0.0.1')
const port = Number(arg('--port', '25565'))
const username = arg('--username', 'MetaBot')
const controlPort = Number(arg('--control-port', '8765'))

let bot = null
let server = null

function startBot() {
  console.log(`[meta-bot] connecting to ${host}:${port} as ${username}`)
  bot = mineflayer.createBot({
    host,
    port,
    username,
    auth: 'offline',
    version: '26.2',
    hideErrors: false
  })

  bot.once('spawn', () => {
    console.log('[meta-bot] joined the world and is ready for actions')
  })
  bot.on('kicked', reason => console.log('[meta-bot] kicked:', reason))
  bot.on('error', error => console.error('[meta-bot] error:', error.message))
  bot.on('end', () => {
    console.log('[meta-bot] disconnected')
    bot = null
  })
  bot.on('chat', (sender, message) => {
    if (sender !== bot.username) console.log(`[chat] ${sender}: ${message}`)
  })
}

function json(res, status, value) {
  const body = JSON.stringify(value)
  res.writeHead(status, { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(body) })
  res.end(body)
}

function readJson(req) {
  return new Promise((resolve, reject) => {
    let body = ''
    req.on('data', chunk => {
      body += chunk
      if (body.length > 4096) reject(new Error('request too large'))
    })
    req.on('end', () => {
      try { resolve(JSON.parse(body || '{}')) } catch (error) { reject(error) }
    })
    req.on('error', reject)
  })
}

server = http.createServer(async (req, res) => {
  if (req.method === 'GET' && req.url === '/status') {
    return json(res, 200, {
      connected: Boolean(bot && bot.entity),
      username,
      host,
      port,
      position: bot?.entity?.position ?? null,
      health: bot?.health ?? null,
      food: bot?.food ?? null
    })
  }

  if (req.method !== 'POST' || req.url !== '/action') return json(res, 404, { error: 'not found' })
  if (!bot) return json(res, 409, { error: 'bot is not connected' })

  try {
    const action = await readJson(req)
    const result = await routeAction(bot, action)
    return json(res, 200, result)
  } catch (error) {
    return json(res, 400, { ok: false, error: error.message })
  }
})

server.listen(controlPort, '127.0.0.1', () => {
  console.log(`[meta-bot] control api listening on 127.0.0.1:${controlPort}`)
  startBot()
})

function shutdown() {
  if (bot) {
    stopMovement(bot)
    bot.quit('client shutting down')
  }
  if (server) server.close()
  process.exit(0)
}

process.on('SIGINT', shutdown)
process.on('SIGTERM', shutdown)
