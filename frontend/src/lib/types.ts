/** Shared API types mirroring the backend DTOs. */

export interface User {
  id: string;
  email: string;
  fullName: string;
  emailVerified: boolean;
  mustChangePassword: boolean;
  roles: string[];
  createdAt: string | null;
}

export interface Category {
  id: string;
  name: string;
  slug: string;
  description: string | null;
  sortOrder: number;
  active: boolean;
}

export interface ProductSummary {
  id: string;
  title: string;
  slug: string;
  shortDescription: string;
  categoryName: string;
  categorySlug: string;
  tags: string[];
  priceMinor: number;
  comparePriceMinor: number | null;
  currency: string;
  featured: boolean;
  bestseller: boolean;
  avgRating: number;
  reviewCount: number;
  coverImageKey: string | null;
  coverUrl: string | null;
}

export interface ProductDetail extends ProductSummary {
  description: string;
  contentVersion: number;
  updatedAt: string;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
  errors?: Record<string, string>;
}
