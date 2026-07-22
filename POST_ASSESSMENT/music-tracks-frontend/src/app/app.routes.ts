import { Routes } from '@angular/router';

import { TracksPageComponent } from './features/tracks/tracks-page/tracks-page.component';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'tracks' },
  { path: 'tracks', component: TracksPageComponent },
  { path: '**', redirectTo: 'tracks' },
];
