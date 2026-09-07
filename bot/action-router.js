const MAX_ACTION_DISTANCE = 6
const MOVEMENT_KEYS = ['forward', 'back', 'left', 'right', 'jump', 'sprint', 'sneak']

function stopMovement(bot) {
  for (const key of MOVEMENT_KEYS) bot.setControlState(key, false)
}

function number(value, name) {
  const parsed = Number(value)
  if (!Number.isFinite(parsed)) throw new Error(`invalid ${name}`)
  return parsed
}

function sanitizeMessage(message) {
  if (typeof message !== 'string') throw new Error('invalid chat message')
  const value = message.trim().slice(0, 256)
  if (!value) throw new Error('chat message is empty')
  return value
}

async function routeAction(bot, action) {
  if (!bot) throw new Error('bot is not connected')
  if (!action || typeof action !== 'object') throw new Error('invalid action')
  switch (action.type) {
    case 'noop': return { ok: true }
    case 'stop': stopMovement(bot); return { ok: true }
    case 'chat': bot.chat(sanitizeMessage(action.message)); return { ok: true }
    case 'look': {
      const yaw = number(action.yaw, 'yaw')
      const pitch = Math.max(-Math.PI / 2, Math.min(Math.PI / 2, number(action.pitch, 'pitch')))
      await bot.look(yaw, pitch, true)
      return { ok: true }
    }
    case 'move':
      for (const key of MOVEMENT_KEYS) if (Object.prototype.hasOwnProperty.call(action, key)) bot.setControlState(key, Boolean(action[key]))
      return { ok: true }
    case 'jump':
      bot.setControlState('jump', true)
      setTimeout(() => { if (bot && bot.entity) bot.setControlState('jump', false) }, 150)
      return { ok: true }
    case 'mine': {
      const target = targetBlock(bot, action)
      if (!target || target.name === 'air') throw new Error('target block not found')
      await bot.dig(target, true)
      return { ok: true, block: target.name }
    }
    case 'place': {
      const target = targetBlock(bot, action)
      if (!target || target.name === 'air') throw new Error('target block not found')
      await bot.placeBlock(target, parseFace(action.face))
      return { ok: true }
    }
    case 'use':
    case 'interact': {
      const target = targetBlock(bot, action)
      if (!target || target.name === 'air') throw new Error('target block not found')
      await bot.activateBlock(target)
      return { ok: true, block: target.name }
    }
    case 'attack': {
      const target = nearestHostile(bot)
      if (!target) throw new Error('no hostile mob nearby')
      await bot.lookAt(target.position.offset(0, Math.max(0.4, target.height * 0.55), 0), true)
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
    default: throw new Error(`unsupported action: ${action.type}`)
  }
}

function targetBlock(bot, action) {
  const x = Math.trunc(number(action.x, 'block x'))
  const y = Math.trunc(number(action.y, 'block y'))
  const z = Math.trunc(number(action.z, 'block z'))
  const target = bot.blockAt({ x, y, z })
  if (!target) return null
  if (bot.entity.position.distanceTo(target.position) > MAX_ACTION_DISTANCE) throw new Error('target is too far away')
  return target
}

function parseFace(face) {
  const values = Array.isArray(face) ? face.map(Number) : String(face ?? '0,1,0').split(',').map(Number)
  if (values.length !== 3 || values.some(value => !Number.isFinite(value))) throw new Error('invalid face vector')
  return { x: values[0], y: values[1], z: values[2] }
}

function nearestHostile(bot) {
  const hostileNames = new Set(['zombie','skeleton','creeper','spider','cave_spider','enderman','witch','slime','magma_cube','phantom','drowned','husk','stray','pillager','vindicator','evoker','ravager','vex','guardian','elder_guardian','blaze','ghast','hoglin','piglin_brute','shulker'])
  let best = null
  let bestDistance = Infinity
  for (const entity of Object.values(bot.entities)) {
    if (!entity || !hostileNames.has(entity.name)) continue
    const distance = bot.entity.position.distanceTo(entity.position)
    if (distance <= 6 && distance < bestDistance) { best = entity; bestDistance = distance }
  }
  return best
}

module.exports = { routeAction, stopMovement }
