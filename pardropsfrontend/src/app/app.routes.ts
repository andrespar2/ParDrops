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
 * - Centro de administración con diez secciones (solo ADMIN)
 * - Página 404 para rutas no encontradas
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';
import { sellerGuard } from './core/guards/seller.guard';

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
    path: 'auth/callback',
    loadComponent: () => import('./pages/auth-callback/auth-callback.component').then(m => m.AuthCallbackComponent),
    title: 'ParDrops - Acceso con Google'
  },
  {
    path: 'cuenta',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/account/account.component').then(m => m.AccountComponent),
    title: 'ParDrops - Mi cuenta'
  },
  {
    path: 'vender',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/seller-application/seller-application.component').then(m => m.SellerApplicationComponent),
    title: 'ParDrops - Vende en ParDrops'
  },
  {
    path: 'tiendas/:slug',
    loadComponent: () => import('./pages/store-public/store-public.component').then(m => m.StorePublicComponent),
    title: 'ParDrops - Tienda'
  },
  {
    path: 'vendedor',
    canActivate: [sellerGuard],
    loadComponent: () => import('./pages/seller/seller.component').then(m => m.SellerComponent),
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'resumen'
      },
      {
        path: 'resumen',
        loadComponent: () => import('./pages/seller-dashboard/seller-dashboard.component').then(m => m.SellerDashboardComponent),
        title: 'ParDrops - Panel de vendedor'
      },
      {
        path: 'productos',
        loadComponent: () => import('./pages/seller-products/seller-products.component').then(m => m.SellerProductsComponent),
        title: 'ParDrops - Mis productos'
      },
      {
        path: 'pedidos',
        loadComponent: () => import('./pages/seller-orders/seller-orders.component').then(m => m.SellerOrdersComponent),
        title: 'ParDrops - Pedidos de mi tienda'
      },
      {
        path: 'perfil',
        loadComponent: () => import('./pages/seller-profile/seller-profile.component').then(m => m.SellerProfileComponent),
        title: 'ParDrops - Mi tienda'
      }
    ]
  },
  {
    path: 'notificaciones',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/notifications/notifications.component').then(m => m.NotificationsComponent),
    title: 'ParDrops - Notificaciones'
  },
  {
    path: 'admin',
    canActivate: [adminGuard],
    loadComponent: () => import('./pages/admin/admin.component').then(m => m.AdminComponent),
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'resumen'
      },
      {
        path: 'resumen',
        loadComponent: () => import('./pages/admin-dashboard/admin-dashboard.component').then(m => m.AdminDashboardComponent),
        title: 'ParDrops - Resumen general'
      },
      {
        path: 'usuarios',
        loadComponent: () => import('./pages/admin-users/admin-users.component').then(m => m.AdminUsersComponent),
        title: 'ParDrops - Usuarios'
      },
      {
        path: 'solicitudes',
        loadComponent: () => import('./pages/admin-applications/admin-applications.component').then(m => m.AdminApplicationsComponent),
        title: 'ParDrops - Solicitudes de vendedor'
      },
      {
        path: 'vendedores',
        loadComponent: () => import('./pages/admin-sellers/admin-sellers.component').then(m => m.AdminSellersComponent),
        title: 'ParDrops - Vendedores'
      },
      {
        path: 'productos',
        loadComponent: () => import('./pages/admin-products/admin-products.component').then(m => m.AdminProductsComponent),
        title: 'ParDrops - Administración de productos'
      },
      {
        path: 'categorias',
        loadComponent: () => import('./pages/admin-categories/admin-categories.component').then(m => m.AdminCategoriesComponent),
        title: 'ParDrops - Categorías'
      },
      {
        path: 'pedidos',
        loadComponent: () => import('./pages/admin-orders/admin-orders.component').then(m => m.AdminOrdersComponent),
        title: 'ParDrops - Administración de pedidos'
      },
      {
        path: 'notificaciones',
        loadComponent: () => import('./pages/notifications/notifications.component').then(m => m.NotificationsComponent),
        title: 'ParDrops - Notificaciones del panel'
      },
      {
        path: 'auditoria',
        loadComponent: () => import('./pages/admin-audit/admin-audit.component').then(m => m.AdminAuditComponent),
        title: 'ParDrops - Auditoría'
      },
      {
        path: 'configuracion',
        loadComponent: () => import('./pages/admin-settings/admin-settings.component').then(m => m.AdminSettingsComponent),
        title: 'ParDrops - Configuración'
      }
    ]
  },
  {
    path: '**',
    loadComponent: () => import('./pages/not-found/not-found.component').then(m => m.NotFoundComponent),
    title: 'ParDrops - Página no encontrada'
  }
];
