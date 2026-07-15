import { TestBed } from '@angular/core/testing';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { vi } from 'vitest';
import { App } from './app';

describe('App', () => {
  let httpMock: HttpTestingController;
  const base = 'http://localhost:8080';

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  /** Flush the GET /scan call fired from ngOnInit. */
  function flushInit(): void {
    httpMock.expectOne(`${base}/scan`).flush([]);
  }

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    flushInit();
    expect(fixture.componentInstance).toBeTruthy();
    httpMock.verify();
  });

  it('loads all scans from GET /scan', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    fixture.detectChanges();

    httpMock.expectOne(`${base}/scan`).flush([
      { id: 1, domainName: 'a.com', numPages: 5, numBrokenLinks: 1, numMissingImages: 0 },
    ]);

    expect(app.scans().length).toBe(1);
    expect(app.scans()[0].domainName).toBe('a.com');
    httpMock.verify();
  });

  it('POSTs a new scan to /scan and reloads the list', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    fixture.detectChanges();
    flushInit();

    app.newScan = {
      domainName: 'new.com',
      numPages: 10,
      numBrokenLinks: 2,
      numMissingImages: 1,
    };
    app.createScan();

    const post = httpMock.expectOne(`${base}/scan`);
    expect(post.request.method).toBe('POST');
    expect(post.request.body.id).toBeUndefined();
    post.flush({ id: 7, ...app.newScan });

    // createScan triggers loadScans()
    httpMock.expectOne(`${base}/scan`).flush([]);
    httpMock.verify();
  });

  it('does not POST when the domain name is empty', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    fixture.detectChanges();
    flushInit();

    app.newScan = { domainName: '   ', numPages: 0, numBrokenLinks: 0, numMissingImages: 0 };
    app.createScan();

    httpMock.expectNone(`${base}/scan`);
    expect(app.toast()?.type).toBe('info');
    httpMock.verify();
  });

  it('fetches a scan by id via GET /scan/{id}', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    fixture.detectChanges();
    flushInit();

    app.lookupId = 3;
    app.findById();

    const req = httpMock.expectOne(`${base}/scan/3`);
    expect(req.request.method).toBe('GET');
    req.flush({ id: 3, domainName: 'b.com', numPages: 1, numBrokenLinks: 0, numMissingImages: 0 });

    expect(app.lookupResult()?.id).toBe(3);
    httpMock.verify();
  });

  it('shows a not-found toast when GET /scan/{id} returns 404', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    fixture.detectChanges();
    flushInit();

    app.lookupId = 99;
    app.findById();

    httpMock
      .expectOne(`${base}/scan/99`)
      .flush('not found', { status: 404, statusText: 'Not Found' });

    expect(app.toast()?.type).toBe('danger');
    expect(app.lookupResult()).toBeNull();
    httpMock.verify();
  });

  it('DELETEs a scan via /scan/{id}', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    fixture.detectChanges();
    flushInit();

    vi.spyOn(window, 'confirm').mockReturnValue(true);
    app.deleteScan(5);

    const req = httpMock.expectOne(`${base}/scan/5`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);

    // delete triggers loadScans()
    httpMock.expectOne(`${base}/scan`).flush([]);
    httpMock.verify();
  });

  it('searches with GET /scan/search/{domain}?orderBy=', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    fixture.detectChanges();
    flushInit();

    app.searchDomain = 'testdomain.com';
    app.searchOrderBy = 'numPages';
    app.search();

    const req = httpMock.expectOne(
      `${base}/scan/search/testdomain.com?orderBy=numPages`
    );
    expect(req.request.method).toBe('GET');
    req.flush([
      { id: 1, domainName: 'testdomain.com', numPages: 3, numBrokenLinks: 0, numMissingImages: 0 },
    ]);

    expect(app.searchResults()?.length).toBe(1);
    httpMock.verify();
  });

  it('shows an error toast when search returns 400 for a bad orderBy', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    fixture.detectChanges();
    flushInit();

    app.searchDomain = 'testdomain.com';
    app.searchOrderBy = 'numPages';
    app.search();

    httpMock
      .expectOne(`${base}/scan/search/testdomain.com?orderBy=numPages`)
      .flush('bad', { status: 400, statusText: 'Bad Request' });

    expect(app.toast()?.type).toBe('danger');
    expect(app.searchResults()).toBeNull();
    httpMock.verify();
  });
});
