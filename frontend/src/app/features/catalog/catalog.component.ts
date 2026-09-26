import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CatalogService } from './catalog.service';
import { ProductType } from '../../shared/models/product-type.model';

@Component({
  selector: 'app-catalog',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './catalog.component.html',
  styleUrl: './catalog.component.scss',
})
export class CatalogComponent implements OnInit {
  readonly productTypes = signal<ProductType[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  newTypeName = '';

  constructor(private readonly catalogService: CatalogService) {}

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.catalogService.list().subscribe({
      next: (types) => {
        this.productTypes.set(types);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load product types.');
        this.loading.set(false);
      },
    });
  }

  addType(): void {
    const name = this.newTypeName.trim();
    if (!name) {
      return;
    }
    this.catalogService.create(name).subscribe({
      next: () => {
        this.newTypeName = '';
        this.reload();
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'Could not create product type.');
      },
    });
  }
}
