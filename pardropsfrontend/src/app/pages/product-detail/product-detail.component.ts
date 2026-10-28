/**
 * @file src/app/pages/product-detail/product-detail.component.ts
 * @description Página de detalle de producto.
 *
 * Muestra información completa del producto: imágenes, descripción larga,
 * precio, stock, categorías y botón para añadir al carrito con cantidad.
 *
 * Incluye además las reseñas de los clientes: la valoración media, la lista de
 * opiniones y, para quien compró y recibió el producto, el formulario para
 * publicar una reseña con estrellas y comentario.
 *
 * El producto se obtiene de la lista ya cargada por el servicio (que a su vez
 * la descarga de la API). Si el slug no corresponde a ningún producto, se
 * redirige al catálogo.
 *
 * @author ParDrops E-commerce
 * @version 1.1.0
 */

import { Component, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { formatCurrency } from '../../core/config/commerce.config';
import { ProductService } from '../../core/services/product.service';
import { CartService } from '../../core/services/cart.service';
import { ReviewService } from '../../core/services/review.service';
import { AuthService } from '../../core/services/auth.service';
import { Product } from '../../core/models/product.model';
import { ProductReview, ReviewEligibility } from '../../core/models/review.model';
import { StarRatingComponent } from '../../shared/components/star-rating/star-rating.component';
import { ImageUploaderComponent } from '../../shared/components/image-uploader/image-uploader.component';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    FormsModule,
    ReactiveFormsModule,
    StarRatingComponent,
    ImageUploaderComponent
  ],
  templateUrl: './product-detail.component.html',
  styleUrls: ['./product-detail.component.scss']
})
export class ProductDetailComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly productService = inject(ProductService);
  private readonly cartService = inject(CartService);
  private readonly reviewService = inject(ReviewService);
  private readonly authService = inject(AuthService);
  private readonly fb = inject(FormBuilder);

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

  /** Talla seleccionada. */
  public readonly selectedSize = signal<string | null>(null);

  /** Color seleccionado. */
  public readonly selectedColor = signal<string | null>(null);

  /** Tallas disponibles en las variantes. */
  public readonly availableSizes = computed(() => {
    const sizes = (this.product()?.variants ?? [])
      .map(variant => variant.size)
      .filter((size): size is string => !!size);
    return [...new Set(sizes)];
  });

  /** Colores disponibles en las variantes. */
  public readonly availableColors = computed(() => {
    const colors = (this.product()?.variants ?? [])
      .map(variant => variant.color)
      .filter((color): color is string => !!color);
    return [...new Set(colors)];
  });

  /** Variante que coincide con la talla y el color elegidos. */
  public readonly selectedVariant = computed(() => {
    const variants = this.product()?.variants ?? [];
    if (variants.length === 0) return undefined;
    const size = this.selectedSize();
    const color = this.selectedColor();
    return variants.find(variant =>
      (!size || variant.size === size) && (!color || variant.color === color)
    ) ?? variants.find(variant => !size || variant.size === size) ?? variants[0];
  });

  /** Precio de la variante elegida o del producto. */
  public readonly displayPrice = computed(
    () => this.selectedVariant()?.price ?? this.product()?.price ?? 0
  );

  /** Stock de la variante elegida o del producto. */
  public readonly displayStock = computed(
    () => this.selectedVariant()?.stock ?? this.product()?.stock ?? 0
  );

  /** Indica si se ha añadido al carrito (feedback). */
  public readonly addedToCart = signal<boolean>(false);

  /** Reseñas publicadas sobre el producto. */
  public readonly reviews = signal<ProductReview[]>([]);

  /** Indica si se están cargando las reseñas. */
  public readonly reviewsLoading = signal<boolean>(false);

  /** Estado de elegibilidad del usuario para reseñar. */
  public readonly eligibility = signal<ReviewEligibility | null>(null);

  /** Indica si hay un envío de reseña en curso. */
  public readonly submitting = signal<boolean>(false);

  /** Indica si el formulario se ha intentado enviar (para mostrar validaciones). */
  public readonly submitted = signal<boolean>(false);

  /** Mensaje de éxito tras publicar una reseña. */
  public readonly successMessage = signal<string | null>(null);

  /** Mensaje de error al publicar una reseña. */
  public readonly errorMessage = signal<string | null>(null);

  /** Calificación elegida en el formulario (0 = sin elegir). */
  public readonly formRating = signal<number>(0);

  /** Imágenes adjuntas a la reseña en edición. */
  public readonly reviewImages = signal<string[]>([]);

  /** Indica si el usuario tiene sesión iniciada. */
  public readonly isAuthenticated = this.authService.isAuthenticated;

  /** Tiempo de feedback para "añadido al carrito". */
  private feedbackTimeout: any;

  /** Último producto cuyas reseñas se han cargado (evita recargas repetidas). */
  private loadedReviewsProductId: string | null = null;

  /** Última combinación producto/sesión consultada para la elegibilidad. */
  private eligibilityKey: string | null = null;

  /** Último producto cuyas variantes se inicializaron. */
  private initializedVariantProductId: string | null = null;

  /** Formulario para publicar una reseña. */
  public readonly reviewForm = this.fb.nonNullable.group({
    rating: [0, [Validators.required, Validators.min(1), Validators.max(5)]],
    comment: ['', [Validators.required, Validators.maxLength(2000)]]
  });

  constructor() {
    this.route.paramMap.subscribe(params => {
      this.slug.set(params.get('slug'));
      this.selectedImageIndex.set(0);
      this.quantity.set(1);
      this.addedToCart.set(false);
      this.resetReviewState();
    });

    // Si el catálogo ya se cargó y el slug no existe, se vuelve al listado.
    effect(() => {
      const loaded = this.productService.loaded();
      if (loaded && this.slug() && !this.product()) {
        this.router.navigate(['/productos']);
      }
    });

    // Inicializa la talla y el color cuando cambia el producto.
    effect(() => {
      const current = this.product();
      if (current && current.id !== this.initializedVariantProductId) {
        this.initializedVariantProductId = current.id;
        const first = current.variants?.[0];
        this.selectedSize.set(first?.size ?? null);
        this.selectedColor.set(first?.color ?? null);
        this.quantity.set(1);
      }
    });

    // Carga las reseñas cuando cambia el producto mostrado.
    effect(() => {
      const productId = this.product()?.id;
      if (productId && productId !== this.loadedReviewsProductId) {
        this.loadedReviewsProductId = productId;
        this.loadReviews(productId);
      }
    });

    // Actualiza la elegibilidad al cambiar de producto o de sesión.
    effect(() => {
      const productId = this.product()?.id;
      const user = this.authService.user();
      if (!productId) return;

      const key = `${productId}|${user?.id ?? 'anonimo'}`;
      if (key !== this.eligibilityKey) {
        this.eligibilityKey = key;
        this.loadEligibility(productId);
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
    const maxStock = this.displayStock();
    if (this.quantity() < maxStock) {
      this.quantity.set(this.quantity() + 1);
    }
  }

  /**
   * Selecciona una talla.
   *
   * @param size talla elegida.
   * @returns {void}
   */
  public selectSize(size: string): void {
    this.selectedSize.set(size);
    this.quantity.set(1);
  }

  /**
   * Selecciona un color.
   *
   * @param color color elegido.
   * @returns {void}
   */
  public selectColor(color: string): void {
    this.selectedColor.set(color);
    this.quantity.set(1);
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
    if (!prod || this.displayStock() <= 0) return;

    this.cartService.addItem(prod, this.quantity(), this.selectedVariant());
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
    return formatCurrency(price);
  }

  /**
   * Valoración media del producto (0 si todavía no tiene reseñas).
   *
   * @returns la media de las calificaciones.
   */
  public averageRating(): number {
    return this.product()?.rating ?? 0;
  }

  /**
   * Número total de reseñas del producto.
   *
   * @returns la cantidad de reseñas.
   */
  public reviewCount(): number {
    return this.product()?.reviewCount ?? 0;
  }

  /**
   * Indica si el usuario actual puede publicar una reseña.
   *
   * @returns {@code true} si la API lo permite.
   */
  public canReview(): boolean {
    return this.eligibility()?.canReview === true;
  }

  /**
   * Indica si el usuario actual ya ha reseñado el producto.
   *
   * @returns {@code true} si ya existe su reseña.
   */
  public hasReviewed(): boolean {
    return this.eligibility()?.hasReviewed === true;
  }

  /**
   * Registra la calificación elegida con las estrellas.
   *
   * @param rating calificación de 1 a 5 estrellas.
   * @returns {void}
   */
  public setRating(rating: number): void {
    this.formRating.set(rating);
    this.reviewForm.controls.rating.setValue(rating);
    this.reviewForm.controls.rating.markAsTouched();
  }

  /**
   * Publica la reseña del usuario autenticado.
   *
   * @returns {void}
   */
  public addReviewImage(url: string): void {
    this.reviewImages.update(images => images.length < 3 ? [...images, url] : images);
  }

  /**
   * Quita una imagen de la reseña.
   *
   * @param index posición de la imagen.
   * @returns {void}
   */
  public removeReviewImage(index: number): void {
    this.reviewImages.update(images => images.filter((_, position) => position !== index));
  }

  public submitReview(): void {
    const prod = this.product();
    this.submitted.set(true);

    if (!prod || this.reviewForm.invalid) {
      this.reviewForm.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const { rating, comment } = this.reviewForm.getRawValue();

    this.reviewService.createReview(prod.id, { rating, comment, images: this.reviewImages() }).subscribe({
      next: () => {
        this.successMessage.set('¡Gracias! Tu reseña se ha publicado correctamente.');
        this.submitted.set(false);
        this.formRating.set(0);
        this.reviewImages.set([]);
        this.reviewForm.reset({ rating: 0, comment: '' });
        this.loadReviews(prod.id);
        this.loadEligibility(prod.id);
        // Refresca el catálogo para actualizar la valoración media del producto.
        this.productService.loadProducts();
      },
      error: (error: HttpErrorResponse) => {
        const apiMessage = error.error?.message;
        this.errorMessage.set(
          apiMessage || 'No se pudo publicar la reseña. Inténtalo de nuevo.'
        );
      },
      complete: () => this.submitting.set(false)
    });
  }

  /**
   * Carga las reseñas de un producto.
   *
   * @param productId identificador del producto.
   * @returns {void}
   */
  private loadReviews(productId: string): void {
    this.reviewsLoading.set(true);
    this.reviewService.getReviews(productId).subscribe({
      next: reviews => this.reviews.set(reviews),
      error: () => this.reviews.set([]),
      complete: () => this.reviewsLoading.set(false)
    });
  }

  /**
   * Consulta si el usuario actual puede reseñar el producto.
   *
   * @param productId identificador del producto.
   * @returns {void}
   */
  private loadEligibility(productId: string): void {
    this.reviewService.getEligibility(productId).subscribe({
      next: eligibility => this.eligibility.set(eligibility),
      error: () => this.eligibility.set(null)
    });
  }

  /**
   * Limpia el estado de reseñas al cambiar de producto.
   *
   * @returns {void}
   */
  private resetReviewState(): void {
    this.reviews.set([]);
    this.eligibility.set(null);
    this.submitting.set(false);
    this.submitted.set(false);
    this.successMessage.set(null);
    this.errorMessage.set(null);
    this.formRating.set(0);
    this.reviewForm.reset({ rating: 0, comment: '' });
    this.loadedReviewsProductId = null;
    this.eligibilityKey = null;
  }
}
