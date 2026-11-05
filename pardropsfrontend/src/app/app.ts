/**
 * @file src/app/app.ts
 * @description Componente raíz de la aplicación.
 * 
 * Este componente actúa como contenedor principal y proporciona
 * la estructura base con cabecera, contenido y pie de página.
 * 
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { HeaderComponent } from './shared/components/header/header.component';
import { FooterComponent } from './shared/components/footer/footer.component';
import { ConnectionStatusService } from './core/services/connection-status.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, HeaderComponent, FooterComponent],
  templateUrl: './app.html',
  styleUrls: ['./app.scss']
})
export class App {
  /** Título de la aplicación */
  public readonly title = 'ParDrops';

  /** Aviso de reconexión cuando el backend está despertando. */
  public readonly connection = inject(ConnectionStatusService);
}