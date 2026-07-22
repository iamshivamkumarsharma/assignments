import { Component, OnInit, ViewChild, ElementRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { finalize } from 'rxjs';

import { Track, TrackRequest } from '../../../core/models/track.model';
import { TrackService } from '../../../core/services/track.service';
import { TrackFormComponent } from '../track-form/track-form.component';
import { TrackListComponent } from '../track-list/track-list.component';

interface Toast {
  type: 'success' | 'danger';
  message: string;
}

/**
 * Container page orchestrating listing, searching, creating and
 * deleting tracks.
 */
@Component({
  selector: 'app-tracks-page',
  imports: [FormsModule, TrackFormComponent, TrackListComponent],
  templateUrl: './tracks-page.component.html',
})
export class TracksPageComponent implements OnInit {
  private readonly trackService = inject(TrackService);

  @ViewChild('modalClose') private modalClose?: ElementRef<HTMLButtonElement>;

  readonly tracks = signal<Track[]>([]);
  readonly loading = signal(false);
  readonly searching = signal(false);
  readonly toast = signal<Toast | null>(null);

  searchTitle = '';
  private searchActive = false;

  ngOnInit(): void {
    this.loadTracks();
  }

  loadTracks(): void {
    this.loading.set(true);
    this.searchActive = false;
    this.searchTitle = '';
    this.trackService
      .getAll()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (data) => this.tracks.set(data),
        error: (err) => this.handleError(err, 'Failed to load tracks.'),
      });
  }

  onCreate(request: TrackRequest): void {
    this.trackService.create(request).subscribe({
      next: (created) => {
        this.modalClose?.nativeElement.click();
        this.showToast('success', `Track "${created.title}" was created.`);
        this.loadTracks();
      },
      error: (err) => this.handleError(err, 'Failed to create track.'),
    });
  }

  onDelete(track: Track): void {
    if (!confirm(`Delete track "${track.title}"?`)) {
      return;
    }
    this.trackService.delete(track.id).subscribe({
      next: () => {
        this.showToast('success', `Track "${track.title}" was deleted.`);
        if (this.searchActive) {
          this.tracks.set([]);
          this.searchActive = false;
        } else {
          this.tracks.update((list) => list.filter((t) => t.id !== track.id));
        }
      },
      error: (err) => this.handleError(err, 'Failed to delete track.'),
    });
  }

  onSearch(): void {
    const title = this.searchTitle.trim();
    if (!title) {
      this.loadTracks();
      return;
    }

    this.searching.set(true);
    this.trackService
      .searchByTitle(title)
      .pipe(finalize(() => this.searching.set(false)))
      .subscribe({
        next: (track) => {
          this.searchActive = true;
          this.tracks.set(track ? [track] : []);
          if (!track) {
            this.showToast('danger', `No track found with title "${title}".`);
          }
        },
        error: (err) => {
          if (err instanceof HttpErrorResponse && err.status === 404) {
            this.searchActive = true;
            this.tracks.set([]);
            this.showToast('danger', `No track found with title "${title}".`);
          } else {
            this.handleError(err, 'Search failed.');
          }
        },
      });
  }

  clearSearch(): void {
    this.loadTracks();
  }

  private handleError(err: unknown, fallback: string): void {
    const message =
      err instanceof HttpErrorResponse && err.error?.message
        ? err.error.message
        : fallback;
    this.showToast('danger', message);
  }

  private showToast(type: Toast['type'], message: string): void {
    this.toast.set({ type, message });
    setTimeout(() => this.toast.set(null), 4000);
  }
}
