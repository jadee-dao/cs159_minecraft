const WebSocket = require('ws');
const wss = new WebSocket.Server({ port: 3001 });

wss.on('connection', (ws) => {
    console.log('Client connected');
    ws.on('message', (message) => {
        console.log('Received:', message);
        // Send message to the Mineflayer bot
        const parsedMessage = JSON.parse(message);
        bot.chat(parsedMessage.message);
    });

    ws.on('close', () => {
        console.log('Client disconnected');
    });
});
