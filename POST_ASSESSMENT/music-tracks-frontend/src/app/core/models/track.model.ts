/**
 * Track domain model as returned by the backend API.
 */
export interface Track {
  id: number;
  title: string;
  albumName: string;
  releaseDate: string | null;
  playCount: number | null;
}

/**
 * Payload used when creating a track.
 * Mirrors the backend {@code TrackRequest} record.
 */
export interface TrackRequest {
  title: string;
  albumName: string;
  releaseDate: string | null;
  playCount: number | null;
}
