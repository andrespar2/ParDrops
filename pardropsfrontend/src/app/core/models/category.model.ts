/** Categoría del catálogo. */
export interface Category {
  id: number;
  name: string;
  slug: string;
  description?: string | null;
  active: boolean;
  featured: boolean;
  parentId?: number | null;
  parentName?: string | null;
  sortOrder: number;
  imageUrl?: string | null;
  collection: boolean;
  productCount: number;
}
