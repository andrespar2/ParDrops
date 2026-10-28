/**
 * @file src/app/pages/admin/admin.component.ts
 * @description Armazón del centro de administración.
 *
 * Envuelve las páginas del panel con una navegación lateral de once secciones
 * (resumen, usuarios, solicitudes, vendedores, productos, categorías, pedidos,
 * notificaciones, auditoría y configuración). El contenido de cada sección se
 * carga en el `router-outlet` anidado. Solo es accesible con rol de
 * administrador (protegida por `adminGuard`).
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

/**
 * Sección del panel de administración.
 *
 * @interface AdminSection
 */
interface AdminSection {
  /** Ruta relativa de la sección. */
  path: string;
  /** Etiqueta visible. */
  label: string;
  /** Descripción corta de la sección. */
  description: string;
  /** Icono (trazo SVG) asociado a la sección. */
  icon: string;
}

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.scss']
})
export class AdminComponent {
  private readonly authService = inject(AuthService);

  /** Nombre del administrador con la sesión iniciada. */
  public readonly userName = computed(
    () => this.authService.user()?.fullName ?? 'Administrador'
  );

  /** Indica si el panel lateral está abierto en pantallas pequeñas. */
  public readonly menuOpen = signal<boolean>(false);

  /** Las diez secciones del centro de administración. */
  public readonly sections: AdminSection[] = [
    {
      path: 'resumen',
      label: 'Resumen general',
      description: 'Estadísticas y actividad de la plataforma',
      icon: 'M3 13h8V3H3v10zm0 8h8v-6H3v6zm10 0h8V11h-8v10zm0-18v6h8V3h-8z'
    },
    {
      path: 'usuarios',
      label: 'Usuarios',
      description: 'Consulta y administración de cuentas',
      icon: 'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M9 7a4 4 0 1 0 0 8 4 4 0 0 0 0-8zm13 14v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75'
    },
    {
      path: 'solicitudes',
      label: 'Solicitudes',
      description: 'Aprobar o rechazar nuevos vendedores',
      icon: 'M9 11l3 3L22 4M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11'
    },
    {
      path: 'vendedores',
      label: 'Vendedores',
      description: 'Suspensión y reactivación de tiendas',
      icon: 'M3 9l1-5h16l1 5M4 9v11a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1V9M4 9h16M9 21v-6h6v6'
    },
    {
      path: 'productos',
      label: 'Productos',
      description: 'Administración global del catálogo',
      icon: 'M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16zM3.27 6.96L12 12l8.73-5.04M12 22V12'
    },
    {
      path: 'categorias',
      label: 'Categorías',
      description: 'Creación y gestión de categorías',
      icon: 'M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82zM7 7h.01'
    },
    {
      path: 'pedidos',
      label: 'Pedidos',
      description: 'Supervisión global y estados',
      icon: 'M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4zM3 6h18M16 10a4 4 0 0 1-8 0'
    },
    {
      path: 'plantillas-correo',
      label: 'Plantillas de correo',
      description: 'Edita los mensajes de pedidos y pagos',
      icon: 'M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2zM22 6l-10 7L2 6'
    },
    {
      path: 'notificaciones',
      label: 'Notificaciones',
      description: 'Actividad que requiere atención',
      icon: 'M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 0 1-3.46 0'
    },
    {
      path: 'auditoria',
      label: 'Auditoría',
      description: 'Registro de acciones administrativas',
      icon: 'M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8zM14 2v6h6M16 13H8M16 17H8M10 9H8'
    },
    {
      path: 'configuracion',
      label: 'Configuración',
      description: 'Opciones generales de la plataforma',
      icon: 'M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.6a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z'
    }
  ];

  /** Alterna la visibilidad del panel lateral en móvil. */
  public toggleMenu(): void {
    this.menuOpen.update(open => !open);
  }

  /** Cierra el panel lateral (al navegar en móvil). */
  public closeMenu(): void {
    this.menuOpen.set(false);
  }
}
