/**
 * @file src/app/shared/components/footer/footer.component.ts
 * @description Componente de pie de página.
 * 
 * Muestra información de la marca, enlaces útiles y copyright.
 * 
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './footer.component.html',
  styleUrls: ['./footer.component.scss']
})
export class FooterComponent {
  /** Año actual para el copyright */
  public readonly currentYear = new Date().getFullYear();
}