import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import {
  Scan,
  SCAN_ORDER_BY_COLUMNS,
  ScanOrderByColumn,
} from './models/scan.model';
import { ScanService } from './services/scan.service';

interface Toast {
  type: 'success' | 'danger' | 'info';
  message: string;
}

@Component({
  selector: 'app-root',
  imports: [CommonModule, FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnInit {
  private readonly scanService = inject(ScanService);

  // Columns offered in the sort dropdown. `deleted` is a valid backend
  // column but hidden here as it isn't a useful sort for users.
  readonly orderByColumns = SCAN_ORDER_BY_COLUMNS.filter(
    (c) => c !== 'deleted'
  );

  // --- Scan list --------------------------------------------------------
  readonly scans = signal<Scan[]>([]);
  readonly loadingList = signal(false);

  // --- Summary KPIs -----------------------------------------------------
  readonly totalScans = computed(() => this.scans().length);
  readonly totalPages = computed(() =>
    this.scans().reduce((sum, s) => sum + (s.numPages ?? 0), 0)
  );
  readonly totalBrokenLinks = computed(() =>
    this.scans().reduce((sum, s) => sum + (s.numBrokenLinks ?? 0), 0)
  );
  readonly totalMissingImages = computed(() =>
    this.scans().reduce((sum, s) => sum + (s.numMissingImages ?? 0), 0)
  );

  // --- Toast / feedback -------------------------------------------------
  readonly toast = signal<Toast | null>(null);

  // --- Create form ------------------------------------------------------
  newScan: Scan = this.emptyScan();
  readonly creating = signal(false);

  // --- Get by id --------------------------------------------------------
  lookupId: number | null = null;
  readonly lookupResult = signal<Scan | null>(null);
  readonly lookupLoading = signal(false);

  // --- Search -----------------------------------------------------------
  searchDomain = '';
  searchOrderBy: ScanOrderByColumn = 'numPages';
  readonly searchResults = signal<Scan[] | null>(null);
  readonly searchLoading = signal(false);

  ngOnInit(): void {
    this.loadScans();
  }

  // ---------------------------------------------------------------------
  // GET /scan
  // ---------------------------------------------------------------------
  loadScans(): void {
    this.loadingList.set(true);
    this.scanService
      .getAllScans()
      .pipe(finalize(() => this.loadingList.set(false)))
      .subscribe({
        next: (data) => this.scans.set(data),
        error: (err) => this.handleError('Failed to load scans', err),
      });
  }

  // ---------------------------------------------------------------------
  // POST /scan
  // ---------------------------------------------------------------------
  createScan(): void {
    const payload: Scan = {
      domainName: this.newScan.domainName.trim(),
      numPages: Number(this.newScan.numPages) || 0,
      numBrokenLinks: Number(this.newScan.numBrokenLinks) || 0,
      numMissingImages: Number(this.newScan.numMissingImages) || 0,
    };

    if (!payload.domainName) {
      this.showToast('info', 'Domain name is required.');
      return;
    }

    this.creating.set(true);
    this.scanService
      .createScan(payload)
      .pipe(finalize(() => this.creating.set(false)))
      .subscribe({
        next: (created) => {
          this.showToast(
            'success',
            `Scan created for "${created.domainName}" (id ${created.id}).`
          );
          this.newScan = this.emptyScan();
          this.loadScans();
        },
        error: (err) => this.handleError('Failed to create scan', err),
      });
  }

  // ---------------------------------------------------------------------
  // GET /scan/{id}
  // ---------------------------------------------------------------------
  findById(): void {
    if (this.lookupId == null) {
      this.showToast('info', 'Please enter an id.');
      return;
    }

    this.lookupLoading.set(true);
    this.lookupResult.set(null);
    this.scanService
      .getScanById(this.lookupId)
      .pipe(finalize(() => this.lookupLoading.set(false)))
      .subscribe({
        next: (scan) => this.lookupResult.set(scan),
        error: (err: HttpErrorResponse) => {
          if (err.status === 404) {
            this.showToast('danger', `No scan found with id ${this.lookupId}.`);
          } else {
            this.handleError('Lookup failed', err);
          }
        },
      });
  }

  // ---------------------------------------------------------------------
  // DELETE /scan/{id}
  // ---------------------------------------------------------------------
  deleteScan(id: number | null | undefined): void {
    if (id == null) {
      return;
    }
    if (!confirm(`Delete scan #${id}? This performs a logical delete.`)) {
      return;
    }

    this.scanService.deleteScan(id).subscribe({
      next: () => {
        this.showToast('success', `Scan #${id} deleted.`);
        this.loadScans();
        if (this.lookupResult()?.id === id) {
          this.lookupResult.set(null);
        }
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 404) {
          this.showToast('danger', `No scan found with id ${id}.`);
        } else {
          this.handleError('Delete failed', err);
        }
      },
    });
  }

  // ---------------------------------------------------------------------
  // GET /scan/search/{domainName}?orderBy=...
  // ---------------------------------------------------------------------
  search(): void {
    const domain = this.searchDomain.trim();
    if (!domain) {
      this.showToast('info', 'Please enter a domain name to search.');
      return;
    }

    this.searchLoading.set(true);
    this.searchResults.set(null);
    this.scanService
      .searchScans(domain, this.searchOrderBy)
      .pipe(finalize(() => this.searchLoading.set(false)))
      .subscribe({
        next: (results) => {
          this.searchResults.set(results);
          this.showToast(
            'info',
            `Found ${results.length} scan(s) for "${domain}".`
          );
        },
        error: (err: HttpErrorResponse) => {
          if (err.status === 400) {
            this.showToast(
              'danger',
              `Invalid orderBy column "${this.searchOrderBy}".`
            );
          } else {
            this.handleError('Search failed', err);
          }
        },
      });
  }

  clearSearch(): void {
    this.searchDomain = '';
    this.searchResults.set(null);
  }

  // ---------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------
  private emptyScan(): Scan {
    return {
      domainName: '',
      numPages: 0,
      numBrokenLinks: 0,
      numMissingImages: 0,
    };
  }

  private handleError(context: string, err: HttpErrorResponse): void {
    const detail =
      err.status === 0
        ? 'backend unreachable (is Spring Boot running on :8080?)'
        : `${err.status} ${err.statusText}`;
    this.showToast('danger', `${context}: ${detail}`);
  }

  private showToast(type: Toast['type'], message: string): void {
    this.toast.set({ type, message });
    setTimeout(() => this.toast.set(null), 4000);
  }
}
