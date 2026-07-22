import { DatePipe } from '@angular/common';
import { Component, input, output } from '@angular/core';

import { Track } from '../../../core/models/track.model';

/**
 * Presentational table of tracks with delete actions.
 */
@Component({
  selector: 'app-track-list',
  imports: [DatePipe],
  templateUrl: './track-list.component.html',
})
export class TrackListComponent {
  readonly tracks = input.required<Track[]>();
  readonly loading = input<boolean>(false);

  readonly deleteTrack = output<Track>();

  trackById(_index: number, track: Track): number {
    return track.id;
  }
}
