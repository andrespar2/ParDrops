/**
 * @file src/app/pages/home/home.component.ts
 * @description Página de inicio.
 * 
 * Muestra una hero section, productos destacados, nuevos y en oferta.
 * Diseño minimalista y elegante.
 * 
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Component, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ProductService } from '../../core/services/product.service';
import { ProductCardComponent } from '../../shared/components/product-card/product-card.component';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink, ProductCardComponent],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent {
  /** Servicio de productos */
  private readonly productService = inject(ProductService);

  /** Productos destacados */
  public readonly featuredProducts = computed(() => this.productService.featuredProducts());

  /** Productos nuevos */
  public readonly newProducts = computed(() => this.productService.newProducts());

  /** Productos en oferta */
  public readonly saleProducts = computed(() => this.productService.saleProducts());
}