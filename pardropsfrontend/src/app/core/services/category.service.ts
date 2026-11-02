import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, shareReplay } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { Category } from '../models/category.model';

/** Categorías públicas del catálogo. */
@Injectable({ providedIn: 'root' })
export class CategoryService {
  private readonly http = inject(HttpClient);

  private readonly url = `${API_BASE_URL}/categories`;

  /** Caché de la petición mientras dure la sesión. */
  private cache$: Observable<Category[]> | null = null;

  /** Lista las categorías activas. */
  public list(): Observable<Category[]> {
    if (!this.cache$) {
      this.cache$ = this.http.get<Category[]>(this.url).pipe(shareReplay(1));
    }
    return this.cache$;
  }
}
