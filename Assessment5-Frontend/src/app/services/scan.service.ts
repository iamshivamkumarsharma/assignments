import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Scan } from '../models/scan.model';

/**
 * Client for the Spring Boot Scan REST API.
 *
 * Maps 1:1 to the backend endpoints exposed by `ScanController` and
 * `HealthController`.
 */
@Injectable({ providedIn: 'root' })
export class ScanService {
  private readonly http = inject(HttpClient);

  /** Base URL of the Spring Boot backend. */
  private readonly baseUrl = 'http://localhost:8080';
  private readonly scanUrl = `${this.baseUrl}/scan`;

  /** GET /scan — list all (non-deleted) scans. */
  getAllScans(): Observable<Scan[]> {
    return this.http.get<Scan[]>(this.scanUrl);
  }

  /** GET /scan/{id} — fetch a single scan (404 if not found). */
  getScanById(id: number): Observable<Scan> {
    return this.http.get<Scan>(`${this.scanUrl}/${id}`);
  }

  /** POST /scan — create a new scan (id must be omitted). */
  createScan(scan: Scan): Observable<Scan> {
    return this.http.post<Scan>(this.scanUrl, scan);
  }

  /** DELETE /scan/{id} — logically delete a scan (404 if not found). */
  deleteScan(id: number): Observable<void> {
    return this.http.delete<void>(`${this.scanUrl}/${id}`);
  }

  /**
   * GET /scan/search/{domainName}?orderBy={column}
   * Returns scans for the domain sorted by the given column (400 if invalid).
   */
  searchScans(domainName: string, orderBy: string): Observable<Scan[]> {
    const params = new HttpParams().set('orderBy', orderBy);
    return this.http.get<Scan[]>(
      `${this.scanUrl}/search/${encodeURIComponent(domainName)}`,
      { params }
    );
  }

  /** GET /health — backend liveness probe. */
  health(): Observable<unknown> {
    return this.http.get(`${this.baseUrl}/health`, { responseType: 'text' });
  }

  /** GET /ready — backend readiness probe. */
  ready(): Observable<unknown> {
    return this.http.get(`${this.baseUrl}/ready`, { responseType: 'text' });
  }
}
