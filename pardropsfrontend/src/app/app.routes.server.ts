/**
 * @file src/app/app.routes.server.ts
 * @description Configuración de rutas para renderizado en servidor (SSR).
 * 
 * Configura el modo de renderizado para cada ruta.
 * Las rutas con parámetros dinámicos no pueden ser prerenderizadas
 * sin especificar getPrerenderParams, por lo que se usa renderizado
 * en servidor para estas rutas.
 * 
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  {
    path: 'productos/:slug',
    renderMode: RenderMode.Server
  },
  {
    path: '**',
    renderMode: RenderMode.Prerender
  }
];