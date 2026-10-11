/**
 * @file src/app/pages/product-detail/product-detail.component.ts
 * @description Página de detalle de producto.
 *
 * Muestra información completa del producto: imágenes, descripción larga,
 * precio, stock, categorías y botón para añadir al carrito con cantidad.
 *
 * El producto se obtiene de la lista ya cargada por el servicio (que a su vez
 * la descarga de la API). Si el slug no corresponde a ningún producto, se
 * redirige al catálogo.
 *
 * @author ParDrops E-commerce
 * @version 1.0.0
 */

import { Component, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ProductService } from '../../core/services/product.service';
import { CartService } from '../../core/services/cart.service';
import { Product } from '../../core/models/product.model';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './product-detail.component.html',
  styleUrls: ['./product-detail.component.scss']
})
export class ProductDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly productService = inject(ProductService);
  private readonly cartService = inject(CartService);

  /** Slug de la ruta actual. */
  private readonly slug = signal<string | null>(null);

  /** Producto actual, derivado de la lista cargada y del slug de la ruta. */
  public readonly product = computed<Product | undefined>(() => {
    const currentSlug = this.slug();
    if (!currentSlug) return undefined;
    return this.productService.products().find(product => product.slug === currentSlug);
  });

  /** Imagen seleccionada. */
  public readonly selectedImageIndex = signal<number>(0);

  /** Cantidad seleccionada. */
  public readonly quantity = signal<number>(1);

  /** Indica si se ha añadido al carrito (feedback). */
  public readonly addedToCart = signal<boolean>(false);

  /** Tiempo de feedback para "añadido al carrito". */
  private feedbackTimeout: any;

  constructor() {
    this.route.paramMap.subscribe(params => {
      this.slug.set(params.get('slug'));
      this.selectedImageIndex.set(0);
      this.quantity.set(1);
      this.addedToCart.set(false);
    });

    // Si el catálogo ya se cargó y el slug no existe, se vuelve al listado.
    effect(() => {
      const loaded = this.productService.loaded();
      if (loaded && this.slug() && !this.product()) {
        this.router.navigate(['/productos']);
      }
    });
  }

  /**
   * Obtiene las imágenes del producto.
   *
   * @returns lista de imágenes del producto actual.
   */
  public getImages() {
    return this.product()?.images || [];
  }

  /**
   * Selecciona una imagen.
   * @param index - Índice de la imagen.
   */
  public selectImage(index: number): void {
    this.selectedImageIndex.set(index);
  }

  /**
   * Incrementa la cantidad.
   */
  public increaseQuantity(): void {
    const prod = this.product();
    if (!prod) return;
    const maxStock = prod.stock;
    if (this.quantity() < maxStock) {
      this.quantity.set(this.quantity() + 1);
    }
  }

  /**
   * Decrementa la cantidad.
   */
  public decreaseQuantity(): void {
    if (this.quantity() > 1) {
      this.quantity.set(this.quantity() - 1);
    }
  }

  /**
   * Añade el producto al carrito.
   */
  public addToCart(): void {
    const prod = this.product();
    if (!prod || prod.stock <= 0) return;

    this.cartService.addItem(prod, this.quantity());
    this.addedToCart.set(true);

    if (this.feedbackTimeout) {
      clearTimeout(this.feedbackTimeout);
    }
    this.feedbackTimeout = setTimeout(() => {
      this.addedToCart.set(false);
    }, 2000);
  }

  /**
   * Formatea el precio con moneda.
   * @param price - Precio a formatear.
   */
  public formatPrice(price: number): string {
    const prod = this.product();
    if (!prod) return price.toString();
    return new Intl.NumberFormat('es-ES', {
      style: 'currency',
      currency: prod.currency,
      minimumFractionDigits: 2
    }).format(price);
  }
}
