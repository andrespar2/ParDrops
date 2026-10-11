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

import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HeaderComponent } from './shared/components/header/header.component';
import { FooterComponent } from './shared/components/footer/footer.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, HeaderComponent, FooterComponent],
  templateUrl: './app.html',
  styleUrls: ['./app.scss']
})
export class App {
  /** Título de la aplicación */
  public readonly title = 'ParDrops';
}