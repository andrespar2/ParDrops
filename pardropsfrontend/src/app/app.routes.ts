/**
 * @file src/app/app.routes.ts
 * @description Configuración de rutas de la aplicación.
 * 
 * Define todas las rutas disponibles en la plataforma de e-commerce:
 * - Inicio (home)
 * - Lista de productos con filtros
 * - Detalle de producto
 * - Carrito de compras
 * - Página 404 para rutas no encontradas
 * 
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/home/home.component').then(m => m.HomeComponent),
    title: 'ParDrops - Inicio'
  },
  {
    path: 'productos',
    loadComponent: () => import('./pages/product-list/product-list.component').then(m => m.ProductListComponent),
    title: 'ParDrops - Productos'
  },
  {
    path: 'productos/:slug',
    loadComponent: () => import('./pages/product-detail/product-detail.component').then(m => m.ProductDetailComponent),
    title: 'ParDrops - Detalle de producto'
  },
  {
    path: 'carrito',
    loadComponent: () => import('./pages/cart/cart.component').then(m => m.CartComponent),
    title: 'ParDrops - Carrito de compras'
  },
  {
    path: '**',
    loadComponent: () => import('./pages/not-found/not-found.component').then(m => m.NotFoundComponent),
    title: 'ParDrops - Página no encontrada'
  }
];