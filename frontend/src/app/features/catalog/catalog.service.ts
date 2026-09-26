import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ProductType } from '../../shared/models/product-type.model';

/**
 * Reference implementation for every other feature service (sales, production, centro, materials,
 * finance): a thin wrapper around HttpClient, one method per backend endpoint, typed with the
 * shared model. Add the equivalent service under each features/<name> folder as you build that
 * module's backend counterpart.
 */
@Injectable({ providedIn: 'root' })
export class CatalogService {
  private readonly baseUrl = '/api/product-types';

  constructor(private readonly http: HttpClient) {}

  list(): Observable<ProductType[]> {
    return this.http.get<ProductType[]>(this.baseUrl);
  }

  create(name: string): Observable<ProductType> {
    return this.http.post<ProductType>(this.baseUrl, { name });
  }
}
