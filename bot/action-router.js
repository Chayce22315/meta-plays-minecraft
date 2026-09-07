const MAX_ACTION_DISTANCE = 6

const heldKeys = ['forward', 'back', 'left', 'right', 'jump', 'sprint', 'sneak']

function stopMovement(bot) {
  for (const key of heldKeys) bot.setControlState(key, false)
}

function clamp(value, min, max) {
  return Math.max(min, Math.min(max, value))
}

function sanitizeMessage(message) {
  return typeof message === 'string' ? message.trim().slice(0, 256) : ''
}

async function routeAction(bot, action) {
  if (!bot) throw new Error('bot is not connected')
  if (!action || typeof action !== 'object') throw new Error('invalid action')

  switch (action.type) {
    case 'noop':
      return { ok: true }

    case 'stop':
      stopMovement(bot)
      return { ok: true }

    case 'chat': {
      const message = sanitizeMessage(action.message)
      if (!message) throw new Error('chat message is empty')
      bot.chat(message)
      return { ok: true }
    }

    case 'look': {
      const yaw = Number(action.yaw)
      const pitch = clamp(Number(action.pitch), -Math.PI / 2, Math.PI / 2)
      if (!Number.isFinite(yaw) || !Number.isFinite(pitch)) throw new Error('invalid look')
      await bot.look(yaw, pitch, true)
      return { ok: true }
    }

    case 'move': {
      for (const key of heldKeys) {
        if (Object.prototype.hasOwnProperty.call(action, key)) {
          bot.setControlState(key, Boolean(action[key]))
        }
      }
      return { ok: true }
    }

    case 'jump':
      bot.setControlState('jump', true)
      setTimeout(() => bot?.setControlState('jump', false), 150)
      return { ok: true }

    case 'mine': {
      const target = findBlock(bot, action)
      if (!target) throw new Error('target block not found')
      await bot.dig(target, true)
      return { ok: true, block: target.name }
    }

    case 'place': {
      const target = findBlock(bot, action)
      if (!target) throw new Error('target block not found')
      const face = parseFace(action.face)
      await bot.placeBlock(target, face)
      return { ok: true }
    }

    case 'use':
    case 'interact': {
      const target = findBlock(bot, action)
      if (!target) throw new Error('target block not found')
      await bot.activateBlock(target)
      return { ok: true, block: target.name }
    }

    case 'attack': {
      const target = nearestHostile(bot)
      if (!target) throw new Error('no hostile mob nearby')
      await bot.lookAt(target.position.offset(0, target.height * 0.55, 0), true)
      bot.attack(target)
      return { ok: true, target: target.name }
    }

    case 'sleep': {
      const bed = bot.findBlock({ matching: block => block.name.endsWith('_bed'), maxDistance: 8 })
      if (!bed) throw new Error('no bed nearby')
      await bot.sleep(bed)
      return { ok: true }
    }

    case 'select': {
      const slot = Number(action.slot)
      if (!Number.isInteger(slot) || slot < 0 || slot > 8) throw new Error('slot must be 0-8')
      bot.setQuickBarSlot(slot)
      return { ok: true }
    }

    default:
      throw new Error(`unsupported action: ${action.type}`)
  }
}

function findBlock(bot, action) {
  const x = Number(action.x)
  const y = Number(action.y)
  const z = Number(action.z)
  if (![x, y, z].every(Number.isFinite)) throw new Error('invalid block coordinates')
  const block = bot.blockAt({ x, y, z })
  if (!block) return null
  if (bot.entity.position.distanceTo(block.position) > MAX_ACTION_DISTANCE) {
    throw new Error('target is too far away')
  }
  return block
}

function parseFace(face) {
  const value = Array.isArray(face) ? face.map(Number) : String(face ?? '0,1,0').split(',').map(Number)
  if (value.length !== 3 || value.some(n => !Number.isFinite(n))) throw new Error('invalid face vector')
  return { x: value[0], y: value[1], z: value[2] }
}

function nearestHostile(bot) {
  const hostileNames = new Set([
    'zombie', 'skeleton', 'creeper', 'spider', 'cave_spider', 'enderman',
    'witch', 'slime', 'magma_cube', 'phantom', 'drowned', 'husk', 'stray',
    'pillager', 'vindicator', 'evoker', 'ravager', 'vex', 'guardian',
    'elder_guardian', 'blaze', 'ghast', 'hoglin', 'piglin_brute', 'shulker'
  ])

  let best = null
  let bestDistance = Infinity
  for (const entity of Object.values(bot.entities)) {
    if (!entity || !hostileNames.has(entity.name)) continue
    const distance = bot.entity.position.distanceTo(entity.position)
    if (distance < bestDistance && distance <= 6) {
      best = entity
      bestDistance = distance
    }
  }
  return best
}

module.exports = { routeAction, stopMovement }
