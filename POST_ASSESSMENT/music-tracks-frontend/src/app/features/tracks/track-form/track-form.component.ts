import { Component, output } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';

import { TrackRequest } from '../../../core/models/track.model';

/**
 * Reactive form used to create a new track.
 */
@Component({
  selector: 'app-track-form',
  imports: [ReactiveFormsModule],
  templateUrl: './track-form.component.html',
})
export class TrackFormComponent {
  /** Emits a valid track request when the form is submitted. */
  readonly createTrack = output<TrackRequest>();

  submitted = false;

  readonly form: FormGroup;

  constructor(private readonly fb: FormBuilder) {
    this.form = this.fb.group({
      title: ['', [Validators.required, Validators.maxLength(120)]],
      albumName: ['', [Validators.required, Validators.maxLength(120)]],
      releaseDate: [null as string | null],
      playCount: [0, [Validators.min(0)]],
    });
  }

  hasError(control: string, error: string): boolean {
    const c = this.form.get(control);
    return !!c && c.hasError(error) && (c.touched || this.submitted);
  }

  onSubmit(): void {
    this.submitted = true;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.value;
    const request: TrackRequest = {
      title: value.title!.trim(),
      albumName: value.albumName!.trim(),
      releaseDate: value.releaseDate || null,
      playCount: value.playCount ?? 0,
    };

    this.createTrack.emit(request);
    this.reset();
  }

  reset(): void {
    this.submitted = false;
    this.form.reset({ title: '', albumName: '', releaseDate: null, playCount: 0 });
  }
}
