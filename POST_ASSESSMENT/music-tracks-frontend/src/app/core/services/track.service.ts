import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Track, TrackRequest } from '../models/track.model';

/**
 * Client for the Track REST API exposed at
 * {@code music/platform/v1/tracks}.
 */
@Injectable({ providedIn: 'root' })
export class TrackService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/tracks`;

  /** Retrieve every track. */
  getAll(): Observable<Track[]> {
    return this.http.get<Track[]>(this.baseUrl);
  }

  /** Create a new track. */
  create(track: TrackRequest): Observable<Track> {
    return this.http.post<Track>(this.baseUrl, track);
  }

  /** Delete a track by its identifier. */
  delete(trackId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${trackId}`);
  }

  /** Find a single track by its exact title. */
  searchByTitle(title: string): Observable<Track> {
    const params = { title };
    return this.http.get<Track>(`${this.baseUrl}/search`, { params });
  }
}
