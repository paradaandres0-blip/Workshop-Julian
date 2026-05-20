require('dotenv').config();
const express = require('express');
const nunjucks = require('nunjucks');
const path = require('path');

const { saleOrchestrator, inventoryOrchestrator, reportOrchestrator } = require('./composition/container');
const saleRoutes = require('./infrastructure/routes/saleRoutes');
const inventoryRoutes = require('./infrastructure/routes/inventoryRoutes');
const reportRoutes = require('./infrastructure/routes/reportRoutes');

const app = express();

// Configure Nunjucks
nunjucks.configure(path.join(__dirname, 'views'), {
    autoescape: true,
    express: app,
    watch: false,
});
app.set('view engine', 'njk');

// Middleware
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Routes
app.use('/sale', saleRoutes(saleOrchestrator));
app.use('/inventory', inventoryRoutes(inventoryOrchestrator));
app.use('/reports', reportRoutes(reportOrchestrator));

// Root redirect
app.get('/', (req, res) => res.redirect('/sale'));

// Start server
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    const reset  = '\x1b[0m';
    const bold   = '\x1b[1m';
    const cyan   = '\x1b[36m';
    const green  = '\x1b[32m';
    const yellow = '\x1b[33m';
    const blue   = '\x1b[34m';
    const magenta= '\x1b[35m';
    const white  = '\x1b[97m';
    const bgBlue = '\x1b[44m';
    const dim    = '\x1b[2m';

    const line   = `${cyan}${'─'.repeat(52)}${reset}`;
    const now    = new Date().toLocaleTimeString('es-ES');

    console.log('');
    console.log(line);
    console.log(`${cyan}│${reset}  ${bgBlue}${bold}${white}  🍴  SUPERMERCADO EL TENEDOR  ${reset}                  ${cyan}│${reset}`);
    console.log(line);
    console.log(`${cyan}│${reset}  ${bold}${green}✔  Servidor iniciado correctamente${reset}              ${cyan}│${reset}`);
    console.log(`${cyan}│${reset}                                                    ${cyan}│${reset}`);
    console.log(`${cyan}│${reset}  ${yellow}🌐  URL:${reset}      ${bold}${white}http://localhost:${PORT}${reset}             ${cyan}│${reset}`);
    console.log(`${cyan}│${reset}  ${blue}📦  Backend:${reset}  ${dim}${white}${process.env.BACKEND_URL || 'http://localhost:8080'}${reset}       ${cyan}│${reset}`);
    console.log(`${cyan}│${reset}  ${magenta}🕐  Hora:${reset}     ${dim}${white}${now}${reset}                         ${cyan}│${reset}`);
    console.log(`${cyan}│${reset}                                                    ${cyan}│${reset}`);
    console.log(`${cyan}│${reset}  ${dim}Rutas disponibles:${reset}                              ${cyan}│${reset}`);
    console.log(`${cyan}│${reset}  ${green}→${reset}  /sale        Pantalla de Venta              ${cyan}│${reset}`);
    console.log(`${cyan}│${reset}  ${green}→${reset}  /inventory   Inventario de Productos        ${cyan}│${reset}`);
    console.log(`${cyan}│${reset}  ${green}→${reset}  /reports     Reportes y Estadísticas        ${cyan}│${reset}`);
    console.log(line);
    console.log(`${dim}  Presiona Ctrl+C para detener el servidor${reset}`);
    console.log('');
});

module.exports = app;
