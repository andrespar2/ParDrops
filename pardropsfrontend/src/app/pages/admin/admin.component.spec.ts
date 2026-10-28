/**
 * @file src/app/pages/admin/admin.component.spec.ts
 * @description Prueba de humo del armazon del centro de administracion.
 *
 * Comprueba que el armazon se monta y expone las diez secciones de la
 * navegacion, que es el requisito funcional del panel. Se monta sin el
 * router-outlet real para no arrastrar las paginas hijas ni sus llamadas HTTP.
 */

import { NO_ERRORS_SCHEMA } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { RouterModule } from '@angular/router';
import { beforeEach, describe, expect, it } from 'vitest';
import { AdminComponent } from './admin.component';

describe('AdminComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminComponent, RouterModule.forRoot([])]
    })
      .overrideComponent(AdminComponent, {
        set: { imports: [], schemas: [NO_ERRORS_SCHEMA] }
      })
      .compileComponents();
  });

  it('se monta el armazon del panel', () => {
    const fixture = TestBed.createComponent(AdminComponent);
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('expone las once secciones del centro de administracion', () => {
    const fixture = TestBed.createComponent(AdminComponent);
    const sections = fixture.componentInstance.sections;

    expect(sections.length).toBe(11);
    expect(sections.map(section => section.path)).toEqual([
      'resumen',
      'usuarios',
      'solicitudes',
      'vendedores',
      'productos',
      'categorias',
      'pedidos',
      'plantillas-correo',
      'notificaciones',
      'auditoria',
      'configuracion'
    ]);
  });

  it('la navegacion lateral esta cerrada al abrir en movil', () => {
    const fixture = TestBed.createComponent(AdminComponent);
    expect(fixture.componentInstance.menuOpen()).toBe(false);
  });

  it('alterna y cierra la navegacion lateral', () => {
    const fixture = TestBed.createComponent(AdminComponent);
    const component = fixture.componentInstance;

    component.toggleMenu();
    expect(component.menuOpen()).toBe(true);

    component.closeMenu();
    expect(component.menuOpen()).toBe(false);
  });
});
