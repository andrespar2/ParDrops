/**
 * @file src/app/app.routes.server.ts
 * @description Configuración de rutas para el renderizado en servidor (SSR).
 *
 * Todas las rutas se renderizan en el cliente.
 *
 * Motivo: la aplicación depende de una API REST externa (el backend Spring Boot)
 * y de la sesión del usuario, que se guarda en el navegador (localStorage). Con
 * renderizado en servidor habría que resolver las llamadas HTTP y el estado de
 * autenticación también en el servidor, lo que complica el arranque y puede
 * provocar inconsistencias de hidratación. El renderizado en cliente es el
 * enfoque natural para este tipo de aplicación.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  {
    path: '**',
    renderMode: RenderMode.Client
  }
];
