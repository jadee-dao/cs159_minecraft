const mineflayer = require('mineflayer');
const { pathfinder, Movements, goals } = require('mineflayer-pathfinder');
const mcData = require('minecraft-data');
const inventoryViewer = require('mineflayer-web-inventory')

const bot = mineflayer.createBot({
  host: 'localhost',
  port: 10008,
  username: 'Bot',
  version: '1.19.4'
});

bot.loadPlugin(pathfinder);
inventoryViewer(bot)

bot.on('chat', (username, message) => {
  if (username === bot.username) return;

  const args = message.split(' ');
  const command = args[0];
  let target;

  switch (command) {
    case '?mineiron':
        bot.chat("#mine iron_ore");
        break;

    case '?come':
      target = bot.players[username]?.entity;
      if (!target) {
        bot.chat("I don't see you!");
        return;
      }
      const data = mcData(bot.version);
      const movements = new Movements(bot, data);
      bot.pathfinder.setMovements(movements);
      const goal = new goals.GoalFollow(target, 1);
      bot.pathfinder.setGoal(goal, true);
      break;

    case '?stop':
      bot.pathfinder.setGoal(null);
      break;

    case '?mine':
      const blockBelow = bot.blockAt(bot.entity.position.offset(0, -1, 0));
      if (blockBelow && bot.canDigBlock(blockBelow)) {
        bot.dig(blockBelow, err => {
          if (err) {
            bot.chat("Error mining block: " + err);
          } else {
            bot.chat("Block mined successfully!");
          }
        });
      } else {
        bot.chat("Can't mine this block.");
      }
      break;

    default:
      bot.chat("I don't understand the command.");
  }
});
