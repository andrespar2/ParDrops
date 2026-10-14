/**
 * @file src/app/app.routes.ts
 * @description Configuración de rutas de la aplicación.
 *
 * Define todas las rutas de la plataforma de e-commerce:
 * - Inicio (home)
 * - Lista de productos con filtros
 * - Detalle de producto
 * - Carrito de compras
 * - Checkout (datos del cliente)
 * - Autenticación (login y registro)
 * - Cuenta del usuario (historial de pedidos, protegida)
 * - Panel de administración de productos (solo ADMIN)
 * - Página 404 para rutas no encontradas
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';

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
    path: 'checkout',
    loadComponent: () => import('./pages/checkout/checkout.component').then(m => m.CheckoutComponent),
    title: 'ParDrops - Finalizar compra'
  },
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent),
    title: 'ParDrops - Iniciar sesión'
  },
  {
    path: 'cuenta',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/account/account.component').then(m => m.AccountComponent),
    title: 'ParDrops - Mi cuenta'
  },
  {
    path: 'admin/productos',
    canActivate: [adminGuard],
    loadComponent: () => import('./pages/admin-products/admin-products.component').then(m => m.AdminProductsComponent),
    title: 'ParDrops - Administración de productos'
  },
  {
    path: 'admin/pedidos',
    canActivate: [adminGuard],
    loadComponent: () => import('./pages/admin-orders/admin-orders.component').then(m => m.AdminOrdersComponent),
    title: 'ParDrops - Administración de pedidos'
  },
  {
    path: '**',
    loadComponent: () => import('./pages/not-found/not-found.component').then(m => m.NotFoundComponent),
    title: 'ParDrops - Página no encontrada'
  }
];
